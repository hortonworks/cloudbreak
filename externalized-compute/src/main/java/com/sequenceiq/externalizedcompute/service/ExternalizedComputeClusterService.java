package com.sequenceiq.externalizedcompute.service;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.cloudera.api.DefaultApi;
import com.cloudera.model.CommonDeleteClusterResponse;
import com.sequenceiq.cloudbreak.auth.CrnUser;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.auth.crn.Crn;
import com.sequenceiq.cloudbreak.auth.crn.CrnResourceDescriptor;
import com.sequenceiq.cloudbreak.auth.crn.RegionAwareCrnGenerator;
import com.sequenceiq.cloudbreak.auth.crn.RegionAwareInternalCrnGeneratorFactory;
import com.sequenceiq.cloudbreak.auth.security.CrnUserDetailsService;
import com.sequenceiq.cloudbreak.common.event.PayloadContext;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.cloudbreak.common.exception.NotFoundException;
import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.cloudbreak.tag.CostTagging;
import com.sequenceiq.cloudbreak.tag.request.CDPTagGenerationRequest;
import com.sequenceiq.environment.api.v1.environment.endpoint.EnvironmentEndpoint;
import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;
import com.sequenceiq.externalizedcompute.ApiException;
import com.sequenceiq.externalizedcompute.api.model.ExternalizedComputeClusterRequest;
import com.sequenceiq.externalizedcompute.entity.ExternalizedComputeCluster;
import com.sequenceiq.externalizedcompute.flow.ExternalizedComputeClusterFlowManager;
import com.sequenceiq.externalizedcompute.repository.ExternalizedComputeClusterRepository;
import com.sequenceiq.flow.api.model.FlowIdentifier;
import com.sequenceiq.flow.core.PayloadContextProvider;
import com.sequenceiq.flow.core.ResourceIdProvider;

@Service
public class ExternalizedComputeClusterService implements ResourceIdProvider, PayloadContextProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(ExternalizedComputeClusterService.class);

    @Inject
    private EnvironmentEndpoint environmentEndpoint;

    @Inject
    private RegionAwareCrnGenerator regionAwareCrnGenerator;

    @Inject
    private ExternalizedComputeClusterRepository externalizedComputeClusterRepository;

    @Inject
    private ExternalizedComputeClusterFlowManager externalizedComputeClusterFlowManager;

    @Inject
    private RegionAwareInternalCrnGeneratorFactory regionAwareInternalCrnGeneratorFactory;

    @Inject
    private LiftieService liftieService;

    @Inject
    private EntitlementService entitlementService;

    @Inject
    private CrnUserDetailsService crnUserDetailsService;

    @Inject
    private AccountTagService accountTagService;

    @Inject
    private CostTagging costTagging;

    @Override
    public Long getResourceIdByResourceCrn(String resourceCrn) {
        return externalizedComputeClusterRepository.findByResourceCrnAndAccountId(resourceCrn, ThreadBasedUserCrnProvider.getAccountId())
                .map(ExternalizedComputeCluster::getId)
                .orElseThrow(() -> new NotFoundException("Resource id not found for resource crn"));
    }

    @Override
    public Long getResourceIdByResourceName(String resourceName) {
        return externalizedComputeClusterRepository.findByNameAndAccountId(resourceName, ThreadBasedUserCrnProvider.getAccountId())
                .map(ExternalizedComputeCluster::getId)
                .orElseThrow(() -> new NotFoundException("Resource id not found for resource name"));
    }

    @Override
    public List<Long> getResourceIdsByResourceCrn(String resourceName) {
        return externalizedComputeClusterRepository.findByResourceCrnAndAccountId(resourceName, ThreadBasedUserCrnProvider.getAccountId())
                .stream()
                .map(ExternalizedComputeCluster::getId)
                .collect(Collectors.toList());
    }

    public FlowIdentifier prepareComputeClusterCreation(ExternalizedComputeClusterRequest externalizedComputeClusterRequest) {
        Crn userCrn = Crn.ofUser(ThreadBasedUserCrnProvider.getUserCrn());
        ExternalizedComputeCluster externalizedComputeCluster = new ExternalizedComputeCluster();
        externalizedComputeCluster.setName(externalizedComputeClusterRequest.getName());
        DetailedEnvironmentResponse environment = environmentEndpoint.getByCrn(externalizedComputeClusterRequest.getEnvironmentCrn());
        externalizedComputeCluster.setEnvironmentCrn(environment.getCrn());
        externalizedComputeCluster.setCreated(new Date().getTime());
        externalizedComputeCluster.setAccountId(userCrn.getAccountId());
        String crn = regionAwareCrnGenerator.generateCrnStringWithUuid(CrnResourceDescriptor.EXTERNALIZED_COMPUTE, userCrn.getAccountId());
        externalizedComputeCluster.setResourceCrn(crn);
        setTagsSafe(environment, externalizedComputeClusterRequest.getTags(), externalizedComputeCluster);
        // TODO: add check if exists
        ExternalizedComputeCluster savedExternalizedComputeCluster = externalizedComputeClusterRepository.save(externalizedComputeCluster);
        LOGGER.info("Saved ExternalizedComputeCluster entity: {}", savedExternalizedComputeCluster);
        return externalizedComputeClusterFlowManager.triggerExternalizedComputeClusterCreation(savedExternalizedComputeCluster);
    }

    public void initiateDelete(Long id) {
        ExternalizedComputeCluster externalizedComputeCluster = getExternalizedComputeCluster(id);
        DefaultApi defaultApi = liftieService.getDefaultApi();
        try {
            if (externalizedComputeCluster.getLiftieName() != null) {
                CommonDeleteClusterResponse commonDeleteClusterResponse = defaultApi.deleteCluster(externalizedComputeCluster.getLiftieName());
                LOGGER.info("Liftie delete response: {}", commonDeleteClusterResponse);
            }
        } catch (ApiException e) {
            if (!e.getMessage().contains("already deleted")) {
                LOGGER.error("Externalized compute cluster deletion failed", e);
                throw new RuntimeException(e);
            }
        }
    }

    public ExternalizedComputeCluster getExternalizedComputeCluster(String name, String accountId) {
        return externalizedComputeClusterRepository.findByNameAndAccountId(name, accountId)
                .orElseThrow(() -> new NotFoundException("Can't find externalized compute cluster by name: " + name));
    }

    public ExternalizedComputeCluster getExternalizedComputeCluster(Long id) {
        return externalizedComputeClusterRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Can't find externalized compute cluster by id: " + id));
    }

    public void deleteExternalizedComputeCluster(Long id) {
        externalizedComputeClusterRepository.deleteById(id);
    }

    @Override
    public PayloadContext getPayloadContext(Long resourceId) {
        try {
            ExternalizedComputeCluster externalizedComputeCluster = getExternalizedComputeCluster(resourceId);
            DetailedEnvironmentResponse envResp = ThreadBasedUserCrnProvider.doAsInternalActor(
                    regionAwareInternalCrnGeneratorFactory.iam().getInternalCrnForServiceAsString(),
                    () -> environmentEndpoint.getByCrn(externalizedComputeCluster.getEnvironmentCrn()));
            return PayloadContext.create(externalizedComputeCluster.getResourceCrn(), envResp.getCrn(), envResp.getCloudPlatform());
        } catch (NotFoundException exception) {
            LOGGER.info("Exception happened when trying to get payload context {}", exception.getMessage(), exception);
        } catch (Exception e) {
            LOGGER.warn("Error happened during fetching payload context for externalized compute", e);
        }
        return null;
    }

    public List<ExternalizedComputeCluster> getAllByEnvironmentCrn(String environmentCrn, String accountId) {
        return externalizedComputeClusterRepository.findAllByEnvironmentCrnAndAccountId(environmentCrn, accountId);
    }

    private void setTagsSafe(DetailedEnvironmentResponse environment, Map<String, String> userDefinedTags,
            ExternalizedComputeCluster externalizedComputeCluster) {
        try {
            CrnUser crnUser = crnUserDetailsService.loadUserByUsername(ThreadBasedUserCrnProvider.getUserCrn());
            boolean internalTenant = entitlementService.internalTenant(ThreadBasedUserCrnProvider.getAccountId());
            Map<String, String> tags = new HashMap<>();
            if (environment.getTags().getUserDefined() != null) {
                tags.putAll(environment.getTags().getUserDefined());
            }
            if (environment.getTags().getDefaults() != null) {
                tags.putAll(environment.getTags().getDefaults());
            }
            CDPTagGenerationRequest request = CDPTagGenerationRequest.Builder
                .builder()
                .withCreatorCrn(ThreadBasedUserCrnProvider.getUserCrn())
                .withEnvironmentCrn(environment.getCrn())
                .withPlatform(environment.getCloudPlatform())
                .withAccountId(ThreadBasedUserCrnProvider.getAccountId())
                .withResourceCrn(externalizedComputeCluster.getResourceCrn())
                .withIsInternalTenant(internalTenant)
                .withUserName(crnUser.getUsername())
                .withAccountTags(accountTagService.list())
                .withUserDefinedTags(userDefinedTags)
                .build();

            Map<String, String> prepareDefaultTags = costTagging.prepareDefaultTags(request);
            tags.putAll(prepareDefaultTags);

            externalizedComputeCluster.setTags(new Json(Objects.requireNonNullElseGet(tags, HashMap::new)));
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Can not convert tags", e);
        }
    }

}
