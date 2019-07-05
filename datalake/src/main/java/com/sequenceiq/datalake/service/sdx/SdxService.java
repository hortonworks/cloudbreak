package com.sequenceiq.datalake.service.sdx;

import static com.sequenceiq.cloudbreak.exception.NotFoundException.notFound;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.StackV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.storage.CloudStorageV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackV4Response;
import com.sequenceiq.cloudbreak.auth.altus.Crn;
import com.sequenceiq.cloudbreak.auth.altus.CrnParseException;
import com.sequenceiq.cloudbreak.client.CloudbreakUserCrnClient;
import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.cloudbreak.common.json.JsonUtil;
import com.sequenceiq.cloudbreak.util.FileReaderUtils;
import com.sequenceiq.datalake.controller.exception.BadRequestException;
import com.sequenceiq.datalake.entity.SdxCluster;
import com.sequenceiq.datalake.entity.SdxClusterStatus;
import com.sequenceiq.datalake.flow.SdxReactorFlowManager;
import com.sequenceiq.datalake.repository.SdxClusterRepository;
import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;
import com.sequenceiq.environment.client.EnvironmentServiceClient;
import com.sequenceiq.sdx.api.model.SdxClusterRequest;

@Service
public class SdxService {

    private static final Logger LOGGER = LoggerFactory.getLogger(SdxService.class);

    @Inject
    private SdxClusterRepository sdxClusterRepository;

    @Inject
    private SdxReactorFlowManager sdxReactorFlowManager;

    @Inject
    private EnvironmentServiceClient environmentServiceClient;

    @Inject
    private CloudbreakUserCrnClient cloudbreakClient;

    @Inject
    private CloudStorageManifester cloudStorageManifester;

    public Set<Long> findByResourceIdsAndStatuses(Set<Long> resourceIds, Set<SdxClusterStatus> statuses) {
        List<SdxCluster> sdxClusters = sdxClusterRepository.findByIdInAndStatusIn(resourceIds, statuses);
        return sdxClusters.stream().map(SdxCluster::getId).collect(Collectors.toSet());
    }

    public SdxCluster getById(Long id) {
        Optional<SdxCluster> sdxClusters = sdxClusterRepository.findById(id);
        if (sdxClusters.isPresent()) {
            return sdxClusters.get();
        } else {
            throw notFound("SDX cluster", id).get();
        }
    }

    public StackV4Response getDetail(String userCrn, String name, Set<String> entries) {
        try {
            return cloudbreakClient.withCrn(userCrn).stackV4Endpoint().get(0L, name, entries);
        } catch (javax.ws.rs.NotFoundException e) {
            LOGGER.info("Sdx cluster not found on CB side", e);
            return null;
        }
    }

    public SdxCluster getByAccountIdAndSdxName(String userCrn, String name) {
        String accountIdFromCrn = getAccountIdFromCrn(userCrn);
        Optional<SdxCluster> sdxCluster = sdxClusterRepository.findByAccountIdAndClusterNameAndDeletedIsNull(accountIdFromCrn, name);
        if (sdxCluster.isPresent()) {
            return sdxCluster.get();
        } else {
            throw notFound("SDX cluster", name).get();
        }
    }

    public void updateSdxStatus(Long id, SdxClusterStatus sdxClusterStatus) {
        updateSdxStatus(id, sdxClusterStatus, null);
    }

    public void updateSdxStatus(Long id, SdxClusterStatus sdxClusterStatus, String statusReason) {
        Optional<SdxCluster> sdxCluster = sdxClusterRepository.findById(id);
        sdxCluster.ifPresentOrElse(sdx -> {
            sdx.setStatus(sdxClusterStatus);
            sdx.setStatusReason(statusReason);
            sdxClusterRepository.save(sdx);
        }, () -> LOGGER.info("Can not update sdx {} to {} status", id, sdxClusterStatus));
    }

    public SdxCluster createSdx(final String userCrn, final String name, final SdxClusterRequest sdxClusterRequest, StackV4Request stackV4Request) {
        validateSdxRequest(name, sdxClusterRequest.getEnvironment(), getAccountIdFromCrn(userCrn));
        SdxCluster sdxCluster = new SdxCluster();
        sdxCluster.setInitiatorUserCrn(userCrn);
        sdxCluster.setCrn(createCrn(getAccountIdFromCrn(userCrn)));
        sdxCluster.setClusterName(name);
        sdxCluster.setAccountId(getAccountIdFromCrn(userCrn));
        sdxCluster.setStatus(SdxClusterStatus.REQUESTED);
        sdxCluster.setClusterShape(sdxClusterRequest.getClusterShape());

        DetailedEnvironmentResponse environment = getEnvironment(userCrn, sdxClusterRequest);
        sdxCluster.setEnvName(environment.getName());
        sdxCluster.setEnvCrn(environment.getCrn());

        setTagsSafe(sdxClusterRequest, sdxCluster);
        stackV4Request = prepareStackRequest(sdxClusterRequest, stackV4Request, sdxCluster, environment);

        try {
            sdxCluster.setStackRequest(JsonUtil.writeValueAsString(stackV4Request));
        } catch (JsonProcessingException e) {
            LOGGER.error("Can not parse internal stackrequest", e);
            throw new BadRequestException("Can not parse internal stackrequest", e);
        }

        sdxCluster = sdxClusterRepository.save(sdxCluster);

        LOGGER.info("trigger SDX creation: {}", sdxCluster);
        sdxReactorFlowManager.triggerSdxCreation(sdxCluster.getId());

        return sdxCluster;
    }

    private StackV4Request prepareStackRequest(SdxClusterRequest sdxClusterRequest, StackV4Request stackV4Request,
            SdxCluster sdxCluster, DetailedEnvironmentResponse environment) {
        stackV4Request = getStackRequest(stackV4Request, environment.getCloudPlatform());
        if (isCloudStorageConfigured(sdxClusterRequest)) {
            CloudStorageV4Request cloudStorageConfig =
                    cloudStorageManifester.getCloudStorageConfig(environment.getCloudPlatform(), sdxCluster, sdxClusterRequest);
            stackV4Request.getCluster().setCloudStorage(cloudStorageConfig);
        }
        return stackV4Request;
    }

    private boolean isCloudStorageConfigured(SdxClusterRequest clusterRequest) {
        return clusterRequest.getCloudStorage() != null
                && StringUtils.isNotEmpty(clusterRequest.getCloudStorage().getBaseLocation());
    }

    private StackV4Request getStackRequest(@Nullable StackV4Request internalStackRequest, String cloudPlatform) {
        if (internalStackRequest == null) {
            return getStackRequestFromFile(cloudPlatform);
        } else {
            return internalStackRequest;
        }
    }

    private StackV4Request getStackRequestFromFile(String cloudPlatform) {
        try {
            String clusterTemplateJson = FileReaderUtils.readFileFromClasspath("sdx/cluster-template.json");
            return JsonUtil.readValue(clusterTemplateJson, StackV4Request.class);
        } catch (IOException e) {
            LOGGER.info("Can not read SDX template for platform {}", cloudPlatform, e);
            throw new BadRequestException("Can not read template for platform " + cloudPlatform);
        }
    }

    private void validateSdxRequest(String name, String envName, String accountId) {
        sdxClusterRepository.findByAccountIdAndClusterNameAndDeletedIsNull(accountId, name)
                .ifPresent(foundSdx -> {
                    throw new BadRequestException("SDX cluster exists with this name: " + name);
                });

        sdxClusterRepository.findByAccountIdAndEnvNameAndDeletedIsNull(accountId, envName).stream().findFirst()
                .ifPresent(existedSdx -> {
                    throw new BadRequestException("SDX cluster exists for environment name: " + existedSdx.getEnvName());
                });
    }

    private void setTagsSafe(SdxClusterRequest sdxClusterRequest, SdxCluster sdxCluster) {
        try {
            sdxCluster.setTags(new Json(sdxClusterRequest.getTags()));
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Can not convert tags", e);
        }
    }

    public List<SdxCluster> listSdx(String userCrn, String envName) {
        String accountIdFromCrn = getAccountIdFromCrn(userCrn);
        if (envName != null) {
            return sdxClusterRepository.findByAccountIdAndEnvNameAndDeletedIsNull(accountIdFromCrn, envName);
        } else {
            return sdxClusterRepository.findByAccountIdAndDeletedIsNull(accountIdFromCrn);
        }
    }

    public void deleteSdx(String userCrn, String name) {
        LOGGER.info("Delete sdx");
        String accountIdFromCrn = getAccountIdFromCrn(userCrn);
        sdxClusterRepository.findByAccountIdAndClusterNameAndDeletedIsNull(accountIdFromCrn, name).ifPresentOrElse(sdxCluster -> {
            sdxCluster.setStatus(SdxClusterStatus.DELETE_REQUESTED);
            sdxClusterRepository.save(sdxCluster);
            sdxReactorFlowManager.triggerSdxDeletion(sdxCluster.getId());
            LOGGER.info("sdx delete triggered: {}", sdxCluster.getClusterName());
        }, () -> {
            throw notFound("SDX cluster", name).get();
        });
    }

    public String createCrn(@Nonnull String accountId) {
        return Crn.builder()
                .setService(Crn.Service.SDX)
                .setAccountId(accountId)
                .setResourceType(Crn.ResourceType.SDX_CLUSTER)
                .setResource(UUID.randomUUID().toString())
                .build()
                .toString();
    }

    private String getAccountIdFromCrn(String userCrn) {
        try {
            Crn crn = Crn.safeFromString(userCrn);
            return crn.getAccountId();
        } catch (NullPointerException | CrnParseException e) {
            throw new BadRequestException("Can not parse CRN to find account ID: " + userCrn);
        }
    }

    private DetailedEnvironmentResponse getEnvironment(String userCrn, SdxClusterRequest sdxClusterRequest) {
        return environmentServiceClient
                .withCrn(userCrn)
                .environmentV1Endpoint()
                .getByName(sdxClusterRequest.getEnvironment());
    }
}
