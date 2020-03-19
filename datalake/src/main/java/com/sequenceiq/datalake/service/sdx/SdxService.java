package com.sequenceiq.datalake.service.sdx;

import static com.sequenceiq.cloudbreak.exception.NotFoundException.notFound;
import static com.sequenceiq.cloudbreak.util.Benchmark.measure;
import static com.sequenceiq.sdx.api.model.SdxClusterShape.CUSTOM;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.Nonnull;
import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.sequenceiq.authorization.resource.AuthorizationResourceType;
import com.sequenceiq.authorization.service.ResourceBasedCrnProvider;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.StackV4Endpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.StackV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.instancegroup.securitygroup.SecurityGroupV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackViewV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.util.requests.SecurityRuleV4Request;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.altus.Crn;
import com.sequenceiq.cloudbreak.auth.altus.CrnParseException;
import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.cloudbreak.common.json.JsonUtil;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.common.service.Clock;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.cloudbreak.validation.ValidationResult;
import com.sequenceiq.cloudbreak.validation.ValidationResult.ValidationResultBuilder;
import com.sequenceiq.common.api.cloudstorage.CloudStorageRequest;
import com.sequenceiq.common.api.type.InstanceGroupType;
import com.sequenceiq.common.model.FileSystemType;
import com.sequenceiq.datalake.configuration.CDPConfigService;
import com.sequenceiq.datalake.controller.exception.BadRequestException;
import com.sequenceiq.datalake.entity.DatalakeStatusEnum;
import com.sequenceiq.datalake.entity.SdxCluster;
import com.sequenceiq.datalake.entity.SdxStatusEntity;
import com.sequenceiq.datalake.flow.SdxReactorFlowManager;
import com.sequenceiq.datalake.repository.SdxClusterRepository;
import com.sequenceiq.datalake.service.EnvironmentClientService;
import com.sequenceiq.datalake.service.sdx.status.SdxStatusService;
import com.sequenceiq.datalake.service.validation.cloudstorage.CloudStorageLocationValidator;
import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;
import com.sequenceiq.flow.api.model.FlowIdentifier;
import com.sequenceiq.flow.core.ResourceIdProvider;
import com.sequenceiq.flow.service.FlowCancelService;
import com.sequenceiq.sdx.api.model.SdxCloudStorageRequest;
import com.sequenceiq.sdx.api.model.SdxClusterRequest;
import com.sequenceiq.sdx.api.model.SdxClusterShape;

@Service
public class SdxService implements ResourceIdProvider, ResourceBasedCrnProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(SdxService.class);

    @Inject
    private SdxExternalDatabaseConfigurer externalDatabaseConfigurer;

    @Inject
    private SdxClusterRepository sdxClusterRepository;

    @Inject
    private SdxReactorFlowManager sdxReactorFlowManager;

    @Inject
    private StackV4Endpoint stackV4Endpoint;

    @Inject
    private DistroxService distroxService;

    @Inject
    private EnvironmentClientService environmentClientService;

    @Inject
    private SdxStatusService sdxStatusService;

    @Inject
    private Clock clock;

    @Inject
    private CloudStorageLocationValidator cloudStorageLocationValidator;

    @Inject
    private CloudStorageManifester cloudStorageManifester;

    @Inject
    private CDPConfigService cdpConfigService;

    @Inject
    private FlowCancelService flowCancelService;

    @Value("${sdx.default.runtime:7.1.0}")
    private String defaultRuntime;

    public Set<Long> findByResourceIdsAndStatuses(Set<Long> resourceIds, Set<DatalakeStatusEnum> statuses) {
        LOGGER.info("Searching for SDX cluster by ids and statuses.");
        List<SdxStatusEntity> sdxStatusEntities = sdxStatusService.findDistinctFirstByStatusInAndDatalakeIdOrderByIdDesc(statuses, resourceIds);
        return sdxStatusEntities.stream().map(sdxStatusEntity -> sdxStatusEntity.getDatalake().getId()).collect(Collectors.toSet());
    }

    public SdxCluster getById(Long id) {
        LOGGER.info("Searching for SDX cluster by id {}", id);
        Optional<SdxCluster> sdxClusters = sdxClusterRepository.findById(id);
        if (sdxClusters.isPresent()) {
            return sdxClusters.get();
        } else {
            throw notFound("SDX cluster", id).get();
        }
    }

    public StackV4Response getDetail(String name, Set<String> entries) {
        try {
            LOGGER.info("Calling cloudbreak for SDX cluster details by name {}", name);
            return stackV4Endpoint.get(0L, name, entries);
        } catch (javax.ws.rs.NotFoundException e) {
            LOGGER.info("Sdx cluster not found on CB side", e);
            return null;
        }
    }

    public SdxCluster getByCrn(String userCrn, String clusterCrn) {
        LOGGER.info("Searching for SDX cluster by crn {}", clusterCrn);
        String accountIdFromCrn = getAccountIdFromCrn(userCrn);
        Optional<SdxCluster> sdxCluster = sdxClusterRepository.findByAccountIdAndCrnAndDeletedIsNull(accountIdFromCrn, clusterCrn);
        if (sdxCluster.isPresent()) {
            return sdxCluster.get();
        } else {
            throw notFound("SDX cluster", clusterCrn).get();
        }
    }

    public SdxCluster getSdxByNameInAccount(String userCrn, String name) {
        LOGGER.info("Searching for SDX cluster by name {}", name);
        String accountIdFromCrn = getAccountIdFromCrn(userCrn);
        Optional<SdxCluster> sdxCluster = measure(() -> sdxClusterRepository.findByAccountIdAndClusterNameAndDeletedIsNull(accountIdFromCrn, name), LOGGER,
                "Fetching SDX cluster took {}ms from DB. Name: [{}]", name);
        if (sdxCluster.isPresent()) {
            return sdxCluster.get();
        } else {
            throw notFound("SDX cluster", name).get();
        }
    }

    public Pair<SdxCluster, FlowIdentifier> createSdx(final String userCrn, final String name, final SdxClusterRequest sdxClusterRequest,
            final StackV4Request internalStackV4Request) {
        LOGGER.info("Creating SDX cluster with name {}", name);
        validateSdxRequest(name, sdxClusterRequest.getEnvironment(), getAccountIdFromCrn(userCrn));
        validateInternalSdxRequest(internalStackV4Request, sdxClusterRequest.getClusterShape());
        DetailedEnvironmentResponse environment = getEnvironment(sdxClusterRequest.getEnvironment());
        SdxCluster sdxCluster = new SdxCluster();
        sdxCluster.setInitiatorUserCrn(userCrn);
        sdxCluster.setCrn(createCrn(getAccountIdFromCrn(userCrn)));
        sdxCluster.setClusterName(name);
        sdxCluster.setAccountId(getAccountIdFromCrn(userCrn));
        sdxCluster.setClusterShape(sdxClusterRequest.getClusterShape());
        sdxCluster.setCreated(clock.getCurrentTimeMillis());
        sdxCluster.setEnvName(environment.getName());
        sdxCluster.setEnvCrn(environment.getCrn());
        setTagsSafe(sdxClusterRequest, sdxCluster);

        if (isCloudStorageConfigured(sdxClusterRequest)) {
            validateCloudStorageRequest(sdxClusterRequest.getCloudStorage(), environment);
            String trimmedBaseLocation = StringUtils.stripEnd(sdxClusterRequest.getCloudStorage().getBaseLocation(), "/");
            sdxCluster.setCloudStorageBaseLocation(trimmedBaseLocation);
            sdxCluster.setCloudStorageFileSystemType(sdxClusterRequest.getCloudStorage().getFileSystemType());
            sdxClusterRequest.getCloudStorage().setBaseLocation(trimmedBaseLocation);
        }
        CloudPlatform cloudPlatform = CloudPlatform.valueOf(environment.getCloudPlatform());
        String runtimeVersion = getRuntime(sdxClusterRequest, internalStackV4Request);
        sdxCluster.setRuntime(runtimeVersion);
        externalDatabaseConfigurer.configure(cloudPlatform, sdxClusterRequest.getExternalDatabase(), sdxCluster);
        updateStackV4RequestWithEnvironmentCrnIfNotExistsOnIt(internalStackV4Request, environment.getCrn());
        StackV4Request stackRequest = getStackRequest(sdxClusterRequest, internalStackV4Request, cloudPlatform, runtimeVersion);
        prepareCloudStorageForStack(sdxClusterRequest, stackRequest, sdxCluster, environment);
        prepareDefaultSecurityConfigs(internalStackV4Request, stackRequest, cloudPlatform);
        try {
            sdxCluster.setStackRequest(JsonUtil.writeValueAsString(stackRequest));
        } catch (JsonProcessingException e) {
            LOGGER.error("Can not parse internal stackrequest", e);
            throw new BadRequestException("Can not parse internal stackrequest", e);
        }

        MDCBuilder.buildMdcContext(sdxCluster);

        sdxCluster = sdxClusterRepository.save(sdxCluster);
        sdxStatusService.setStatusForDatalakeAndNotify(DatalakeStatusEnum.REQUESTED, "Datalake requested", sdxCluster);

        LOGGER.info("trigger SDX creation: {}", sdxCluster);
        FlowIdentifier flowIdentifier = sdxReactorFlowManager.triggerSdxCreation(sdxCluster.getId());

        return Pair.of(sdxCluster, flowIdentifier);
    }

    private StackV4Request getStackRequest(SdxClusterRequest sdxClusterRequest, StackV4Request internalStackV4Request, CloudPlatform cloudPlatform,
            String runtimeVersion) {
        if (internalStackV4Request == null) {
            StackV4Request stackRequest = cdpConfigService.getConfigForKey(
                    new CDPConfigKey(cloudPlatform, sdxClusterRequest.getClusterShape(), runtimeVersion));
            if (stackRequest == null) {
                LOGGER.error("Can't find template for cloudplatform: {}, shape {}, cdp version: {}", cloudPlatform, sdxClusterRequest.getClusterShape(),
                        runtimeVersion);
                throw new BadRequestException("Can't find template for cloudplatform: " + cloudPlatform + ", shape: " + sdxClusterRequest.getClusterShape() +
                        ", runtime version: " + runtimeVersion);
            }
            return stackRequest;
        } else {
            return internalStackV4Request;
        }
    }

    private String getRuntime(SdxClusterRequest sdxClusterRequest, StackV4Request stackV4Request) {
        if (sdxClusterRequest.getRuntime() != null) {
            return sdxClusterRequest.getRuntime();
        } else if (stackV4Request != null) {
            return null;
        } else {
            return defaultRuntime;
        }
    }

    private void updateStackV4RequestWithEnvironmentCrnIfNotExistsOnIt(StackV4Request request, String environmentCrn) {
        if (request != null && StringUtils.isEmpty(request.getEnvironmentCrn())) {
            request.setEnvironmentCrn(environmentCrn);
            LOGGER.debug("Environment crn for internal sdx stack request set to: {}", environmentCrn);
        }
    }

    private void prepareCloudStorageForStack(SdxClusterRequest sdxClusterRequest, StackV4Request stackV4Request,
            SdxCluster sdxCluster, DetailedEnvironmentResponse environment) {
        CloudStorageRequest cloudStorageRequest = cloudStorageManifester.initCloudStorageRequest(environment,
                stackV4Request.getCluster(), sdxCluster, sdxClusterRequest);
        stackV4Request.getCluster().setCloudStorage(cloudStorageRequest);
    }

    public FlowIdentifier sync(String name) {
        return stackV4Endpoint.sync(0L, name);
    }

    public void syncByCrn(String userCrn, String crn) {
        SdxCluster sdxCluster = getByCrn(userCrn, crn);
        stackV4Endpoint.sync(0L, sdxCluster.getClusterName());
    }

    protected StackV4Request prepareDefaultSecurityConfigs(StackV4Request internalRequest, StackV4Request stackV4Request, CloudPlatform cloudPlatform) {
        if (internalRequest == null && !List.of("MOCK", "YARN").contains(cloudPlatform)) {
            stackV4Request.getInstanceGroups().forEach(instance -> {
                SecurityGroupV4Request groupRequest = new SecurityGroupV4Request();
                if (InstanceGroupType.CORE.equals(instance.getType())) {
                    groupRequest.setSecurityRules(rulesWithPorts("22"));
                } else if (InstanceGroupType.GATEWAY.equals(instance.getType())) {
                    groupRequest.setSecurityRules(rulesWithPorts("443", "22"));
                } else {
                    throw new IllegalStateException("Unknown instance group type " + instance.getType());
                }
                instance.setSecurityGroup(groupRequest);
            });
        }
        return stackV4Request;
    }

    private List<SecurityRuleV4Request> rulesWithPorts(String... ports) {
        return Stream.of(ports)
                .map(port -> {
                    SecurityRuleV4Request ruleRequest = new SecurityRuleV4Request();
                    ruleRequest.setSubnet("0.0.0.0/0");
                    ruleRequest.setPorts(List.of(port));
                    ruleRequest.setProtocol("tcp");
                    return ruleRequest;
                })
                .collect(Collectors.toList());
    }

    @Override
    public Long getResourceIdByResourceCrn(String resourceCrn) {
        String userCrn = ThreadBasedUserCrnProvider.getUserCrn();
        return getByCrn(userCrn, resourceCrn).getId();
    }

    @Override
    public Long getResourceIdByResourceName(String resourceName) {
        String userCrn = ThreadBasedUserCrnProvider.getUserCrn();
        return getSdxByNameInAccount(userCrn, resourceName).getId();
    }

    private void validateCloudStorageRequest(SdxCloudStorageRequest cloudStorage, DetailedEnvironmentResponse environment) {
        if (cloudStorage != null) {
            ValidationResultBuilder validationBuilder = new ValidationResultBuilder();
            validationBuilder.ifError(() -> cloudStorage.getFileSystemType() == null, "'fileSystemType' must be set in 'cloudStorage'!");

            if (StringUtils.isEmpty(cloudStorage.getBaseLocation())) {
                validationBuilder.error("'baseLocation' must be set in 'cloudStorage'!");
            } else {
                if (FileSystemType.S3.equals(cloudStorage.getFileSystemType())) {
                    validationBuilder.ifError(() -> !cloudStorage.getBaseLocation().startsWith(FileSystemType.S3.getProtocol()),
                            String.format("'baseLocation' must start with '%s' if 'fileSystemType' is 'S3'!", FileSystemType.S3.getProtocol()));
                    validationBuilder.ifError(() -> cloudStorage.getS3() == null, "'s3' must be set if 'fileSystemType' is 'S3'!");
                    cloudStorageLocationValidator.validate(cloudStorage.getBaseLocation(), FileSystemType.S3, environment, validationBuilder);
                }
                if (FileSystemType.ADLS.equals(cloudStorage.getFileSystemType())) {
                    validationBuilder.ifError(() -> !cloudStorage.getBaseLocation().startsWith(FileSystemType.ADLS.getProtocol()),
                            String.format("'baseLocation' must start with '%s' if 'fileSystemType' is 'ADLS'!", FileSystemType.ADLS.getProtocol()));
                    validationBuilder.ifError(() -> cloudStorage.getAdls() == null, "'adls' must be set if 'fileSystemType' is 'ADLS'!");
                }
                if (FileSystemType.ADLS_GEN_2.equals(cloudStorage.getFileSystemType())) {
                    validationBuilder.ifError(() -> !cloudStorage.getBaseLocation().startsWith(FileSystemType.ADLS_GEN_2.getProtocol()),
                            String.format("'baseLocation' must start with '%s' if 'fileSystemType' is 'ADLS_GEN_2'!",
                                    FileSystemType.ADLS_GEN_2.getProtocol()));
                    validationBuilder.ifError(() -> cloudStorage.getAdlsGen2() == null, "'adlsGen2' must be set if 'fileSystemType' is 'ADLS_GEN_2'!");
                }
                if (FileSystemType.WASB.equals(cloudStorage.getFileSystemType())) {
                    validationBuilder.ifError(() -> !cloudStorage.getBaseLocation().startsWith(FileSystemType.WASB.getProtocol()),
                            String.format("'baseLocation' must start with '%s' if 'fileSystemType' is 'WASB'", FileSystemType.WASB.getProtocol()));
                    validationBuilder.ifError(() -> cloudStorage.getWasb() == null, "'wasb' must be set if 'fileSystemType' is 'WASB'!");
                }
                if (FileSystemType.GCS.equals(cloudStorage.getFileSystemType())) {
                    validationBuilder.ifError(() -> !cloudStorage.getBaseLocation().startsWith(FileSystemType.GCS.getProtocol()),
                            String.format("'baseLocation' must start with '%s' if 'fileSystemType' is 'GCS'!", FileSystemType.GCS.getProtocol()));
                    validationBuilder.ifError(() -> cloudStorage.getGcs() == null, "'gcs' must be set if 'fileSystemType' is 'GCS'!");
                }
            }

            ValidationResult validationResult = validationBuilder.build();
            if (validationResult.hasError()) {
                throw new BadRequestException(validationResult.getFormattedErrors());
            }
        }
    }

    private boolean isCloudStorageConfigured(SdxClusterRequest clusterRequest) {
        return clusterRequest.getCloudStorage() != null
                && StringUtils.isNotEmpty(clusterRequest.getCloudStorage().getBaseLocation());
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

    private void validateInternalSdxRequest(StackV4Request stackv4Request, SdxClusterShape clusterShape) {
        if (stackv4Request != null) {
            if (!clusterShape.equals(CUSTOM)) {
                throw new BadRequestException("Cluster shape '" + clusterShape + "' is not accepted on SDX Internal API. Use 'CUSTOM' cluster shape");
            }
        }
    }

    private void setTagsSafe(SdxClusterRequest sdxClusterRequest, SdxCluster sdxCluster) {
        try {
            if (sdxClusterRequest.getTags() == null) {
                sdxCluster.setTags(new Json(new HashMap<>()));
            } else {
                sdxCluster.setTags(new Json(sdxClusterRequest.getTags()));
            }
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Can not convert tags", e);
        }
    }

    public List<SdxCluster> listSdxByEnvCrn(String userCrn, String envCrn) {
        LOGGER.info("Listing SDX clusters by environment crn {}", envCrn);
        String accountIdFromCrn = getAccountIdFromCrn(userCrn);
        return sdxClusterRepository.findByAccountIdAndEnvCrnAndDeletedIsNull(accountIdFromCrn, envCrn);
    }

    public List<SdxCluster> listSdx(String userCrn, String envName) {
        String accountIdFromCrn = getAccountIdFromCrn(userCrn);
        if (envName != null) {
            LOGGER.info("Listing SDX clusters by environment name {}", envName);
            return sdxClusterRepository.findByAccountIdAndEnvNameAndDeletedIsNull(accountIdFromCrn, envName);
        } else {
            return sdxClusterRepository.findByAccountIdAndDeletedIsNull(accountIdFromCrn);
        }
    }

    public FlowIdentifier deleteSdxByClusterCrn(String userCrn, String clusterCrn, boolean forced) {
        LOGGER.info("Deleting SDX {}", clusterCrn);
        String accountIdFromCrn = getAccountIdFromCrn(userCrn);
        return sdxClusterRepository.findByAccountIdAndCrnAndDeletedIsNull(accountIdFromCrn, clusterCrn)
                .map(sdxCluster -> deleteSdxCluster(sdxCluster, forced))
                .orElseThrow(() -> notFound("SDX cluster", clusterCrn).get());
    }

    public FlowIdentifier deleteSdx(String userCrn, String name, boolean forced) {
        LOGGER.info("Deleting SDX {}", name);
        String accountIdFromCrn = getAccountIdFromCrn(userCrn);
        return sdxClusterRepository.findByAccountIdAndClusterNameAndDeletedIsNull(accountIdFromCrn, name)
                .map(sdxCluster -> deleteSdxCluster(sdxCluster, forced))
                .orElseThrow(() -> notFound("SDX cluster", name).get());
    }

    private FlowIdentifier deleteSdxCluster(SdxCluster sdxCluster, boolean forced) {
        checkIfSdxIsDeletable(sdxCluster);
        MDCBuilder.buildMdcContext(sdxCluster);
        sdxClusterRepository.save(sdxCluster);
        sdxStatusService.setStatusForDatalakeAndNotify(DatalakeStatusEnum.DELETE_REQUESTED, "Datalake deletion requested", sdxCluster);
        FlowIdentifier flowIdentifier = sdxReactorFlowManager.triggerSdxDeletion(sdxCluster.getId(), forced);
        flowCancelService.cancelRunningFlows(sdxCluster.getId());
        LOGGER.info("SDX delete triggered: {}", sdxCluster.getClusterName());
        return flowIdentifier;
    }

    private void checkIfSdxIsDeletable(SdxCluster sdxCluster) {
        Collection<StackViewV4Response> attachedDistroXClusters = distroxService.getAttachedDistroXClusters(sdxCluster.getEnvCrn());
        if (!attachedDistroXClusters.isEmpty()) {
            throw new BadRequestException(String.format("The following Data Hub cluster(s) must be terminated before SDX deletion [%s]",
                    String.join(", ", attachedDistroXClusters.stream().map(StackViewV4Response::getName).collect(Collectors.toList()))));
        }
    }

    private String createCrn(@Nonnull String accountId) {
        return Crn.builder()
                .setService(Crn.Service.DATALAKE)
                .setAccountId(accountId)
                .setResourceType(Crn.ResourceType.DATALAKE)
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

    private DetailedEnvironmentResponse getEnvironment(String environmentName) {
        return environmentClientService.getByName(environmentName);
    }

    @Override
    public String getResourceCrnByResourceName(String resourceName) {
        return getSdxByNameInAccount(ThreadBasedUserCrnProvider.getUserCrn(), resourceName).getCrn();
    }

    @Override
    public List<String> getResourceCrnListByResourceNameList(List<String> resourceNames) {
        return resourceNames.stream()
                .map(resourceName -> getSdxByNameInAccount(ThreadBasedUserCrnProvider.getUserCrn(), resourceName).getCrn())
                .collect(Collectors.toList());
    }

    @Override
    public AuthorizationResourceType getResourceType() {
        return AuthorizationResourceType.DATALAKE;
    }
}
