package com.sequenceiq.datalake.service.sdx;

import static com.sequenceiq.cloudbreak.common.mappable.CloudPlatform.AWS;
import static com.sequenceiq.cloudbreak.common.mappable.CloudPlatform.AZURE;
import static com.sequenceiq.cloudbreak.exception.NotFoundException.notFound;
import static com.sequenceiq.cloudbreak.util.Benchmark.measure;
import static com.sequenceiq.sdx.api.model.SdxClusterShape.CUSTOM;
import static org.apache.commons.lang3.StringUtils.isNotEmpty;

import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.Nonnull;
import javax.inject.Inject;

import org.apache.commons.lang3.EnumUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.sequenceiq.authorization.resource.AuthorizationResourceType;
import com.sequenceiq.authorization.service.ResourceBasedCrnProvider;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.StackV4Endpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceTemplateV4Base;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.parameter.template.AwsInstanceTemplateV4Parameters;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.parameter.template.AwsInstanceTemplateV4SpotParameters;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.StackV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.instancegroup.InstanceGroupV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.instancegroup.securitygroup.SecurityGroupV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackViewV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.cluster.ClusterV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.cluster.clouderamanager.ClouderaManagerProductV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.cluster.clouderamanager.ClouderaManagerV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.util.requests.SecurityRuleV4Request;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.altus.Crn;
import com.sequenceiq.cloudbreak.auth.altus.CrnParseException;
import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.auth.altus.GrpcUmsClient;
import com.sequenceiq.cloudbreak.cloud.VersionComparator;
import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.cloudbreak.common.json.JsonUtil;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.common.service.Clock;
import com.sequenceiq.cloudbreak.common.service.TransactionService;
import com.sequenceiq.cloudbreak.common.service.TransactionService.TransactionExecutionException;
import com.sequenceiq.cloudbreak.common.service.TransactionService.TransactionRuntimeExecutionException;
import com.sequenceiq.cloudbreak.common.type.Versioned;
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
import com.sequenceiq.sdx.api.model.SdxAwsBase;
import com.sequenceiq.sdx.api.model.SdxAwsSpotParameters;
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

    @Inject
    private GrpcUmsClient grpcUmsClient;

    @Inject
    private TransactionService transactionService;

    @Inject
    private EntitlementService entitlementService;

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

    public StackV4Response getDetail(String name, Set<String> entries, String accountId) {
        try {
            LOGGER.info("Calling cloudbreak for SDX cluster details by name {}", name);
            return ThreadBasedUserCrnProvider.doAsInternalActor(() -> stackV4Endpoint.get(0L, name, entries, accountId));
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

    public SdxCluster getByNameInAccount(String userCrn, String name) {
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
        DetailedEnvironmentResponse environment = getEnvironment(sdxClusterRequest.getEnvironment());
        validateInternalSdxRequest(internalStackV4Request, sdxClusterRequest.getClusterShape());
        validateEnv(environment);
        validateRazEnablement(sdxClusterRequest, environment);
        validateMediumDutySdxEnablement(sdxClusterRequest, environment);
        SdxCluster sdxCluster = new SdxCluster();
        sdxCluster.setInitiatorUserCrn(userCrn);
        sdxCluster.setCrn(createCrn(getAccountIdFromCrn(userCrn)));
        sdxCluster.setClusterName(name);
        sdxCluster.setAccountId(getAccountIdFromCrn(userCrn));
        sdxCluster.setClusterShape(sdxClusterRequest.getClusterShape());
        sdxCluster.setCreated(clock.getCurrentTimeMillis());
        sdxCluster.setEnvName(environment.getName());
        sdxCluster.setEnvCrn(environment.getCrn());
        sdxCluster.setRangerRazEnabled(sdxClusterRequest.isEnableRangerRaz());
        setTagsSafe(sdxClusterRequest, sdxCluster);

        CloudPlatform cloudPlatform = CloudPlatform.valueOf(environment.getCloudPlatform());

        if (isCloudStorageConfigured(sdxClusterRequest)) {
            validateCloudStorageRequest(sdxClusterRequest.getCloudStorage(), environment);
            String trimmedBaseLocation = StringUtils.stripEnd(sdxClusterRequest.getCloudStorage().getBaseLocation(), "/");
            sdxCluster.setCloudStorageBaseLocation(trimmedBaseLocation);
            sdxCluster.setCloudStorageFileSystemType(sdxClusterRequest.getCloudStorage().getFileSystemType());
            sdxClusterRequest.getCloudStorage().setBaseLocation(trimmedBaseLocation);
        } else if (!CloudPlatform.YARN.equalsIgnoreCase(cloudPlatform.name()) &&
                !CloudPlatform.MOCK.equalsIgnoreCase(cloudPlatform.name()) &&
                internalStackV4Request == null) {
            throw new BadRequestException("Cloud storage parameter is required.");
        }

        String runtimeVersion = getRuntime(sdxClusterRequest, internalStackV4Request);
        sdxCluster.setRuntime(runtimeVersion);
        externalDatabaseConfigurer.configure(cloudPlatform, sdxClusterRequest.getExternalDatabase(), sdxCluster);
        updateStackV4RequestWithEnvironmentCrnIfNotExistsOnIt(internalStackV4Request, environment.getCrn());
        StackV4Request stackRequest = getStackRequest(sdxClusterRequest, internalStackV4Request, cloudPlatform, runtimeVersion);
        prepareCloudStorageForStack(sdxClusterRequest, stackRequest, sdxCluster, environment);
        prepareDefaultSecurityConfigs(internalStackV4Request, stackRequest, cloudPlatform);
        prepareProviderSpecificParameters(stackRequest, sdxClusterRequest, cloudPlatform);
        stackRequest.setResourceCrn(sdxCluster.getCrn());
        try {
            sdxCluster.setStackRequest(JsonUtil.writeValueAsString(stackRequest));
        } catch (JsonProcessingException e) {
            LOGGER.error("Can not parse internal stackrequest", e);
            throw new BadRequestException("Can not parse internal stackrequest", e);
        }

        MDCBuilder.buildMdcContext(sdxCluster);

        SdxCluster savedSdxCluster;
        try {
            savedSdxCluster = transactionService.required(() -> {
                SdxCluster created = sdxClusterRepository.save(sdxCluster);
                grpcUmsClient.assignResourceOwnerRoleIfEntitled(created.getInitiatorUserCrn(), created.getCrn(), created.getAccountId());
                sdxStatusService.setStatusForDatalakeAndNotify(DatalakeStatusEnum.REQUESTED, "Datalake requested", created);
                return created;
            });
        } catch (TransactionExecutionException e) {
            throw new TransactionRuntimeExecutionException(e);
        }

        FlowIdentifier flowIdentifier = sdxReactorFlowManager.triggerSdxCreation(savedSdxCluster);

        return Pair.of(savedSdxCluster, flowIdentifier);
    }

    private void validateEnv(DetailedEnvironmentResponse environment) {
        if (environment.getEnvironmentStatus().isDeleteInProgress()) {
            throw new BadRequestException("The environment is in delete in progress phase. Please create a new environment first!");
        } else if (environment.getEnvironmentStatus().isStopInProgressOrStopped()) {
            throw new BadRequestException("The environment is stopped. Please start the environment first!");
        } else if (environment.getEnvironmentStatus().isStartInProgress()) {
            throw new BadRequestException("The environment is starting. Please wait until finished!");
        }
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
            stackRequest.getCluster().setRangerRazEnabled(sdxClusterRequest.isEnableRangerRaz());
            return stackRequest;
        } else {
            // We have provided a --ranger-raz-enabled flag in the CLI, but it will
            // get overwritten if you use a custom json (using --cli-json). To avoid
            // this, we will set the raz enablement here as well. See CB-7474 for more details
            internalStackV4Request.getCluster().setRangerRazEnabled(sdxClusterRequest.isEnableRangerRaz());
            return internalStackV4Request;
        }
    }

    private String getRuntime(SdxClusterRequest sdxClusterRequest, StackV4Request stackV4Request) {
        if (sdxClusterRequest.getRuntime() != null) {
            return sdxClusterRequest.getRuntime();
        } else if (stackV4Request != null) {
            return null;
        } else {
            return cdpConfigService.getDefaultRuntime();
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

    public FlowIdentifier sync(String name, String accountId) {
        return ThreadBasedUserCrnProvider.doAsInternalActor(() -> stackV4Endpoint.sync(0L, name, accountId));
    }

    public void syncByCrn(String userCrn, String crn) {
        SdxCluster sdxCluster = getByCrn(userCrn, crn);
        ThreadBasedUserCrnProvider.doAsInternalActor(() ->
                stackV4Endpoint.sync(0L, sdxCluster.getClusterName(), Crn.fromString(crn).getAccountId()));
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

    private void prepareProviderSpecificParameters(StackV4Request stackRequest, SdxClusterRequest sdxClusterRequest, CloudPlatform cloudPlatform) {
        switch (cloudPlatform) {
            case AWS:
                useAwsSpotPercentageIfPresent(stackRequest, sdxClusterRequest);
                break;
            case GCP:
            case AZURE:
            case OPENSTACK:
            case YARN:
            case MOCK:
            default:
                break;
        }
    }

    private void useAwsSpotPercentageIfPresent(StackV4Request stackRequest, SdxClusterRequest sdxClusterRequest) {
        Optional.ofNullable(sdxClusterRequest.getAws())
                .map(SdxAwsBase::getSpot)
                .ifPresent(spotParameters -> updateAwsSpotParameters(stackRequest, spotParameters));
    }

    private void updateAwsSpotParameters(StackV4Request stackRequest, SdxAwsSpotParameters sdxSpotParameters) {
        stackRequest.getInstanceGroups().stream()
                .map(InstanceGroupV4Request::getTemplate)
                .peek(template -> {
                    if (template.getAws() == null) {
                        template.setAws(new AwsInstanceTemplateV4Parameters());
                    }
                })
                .map(InstanceTemplateV4Base::getAws)
                .peek(aws -> {
                    if (aws.getSpot() == null) {
                        aws.setSpot(new AwsInstanceTemplateV4SpotParameters());
                    }
                })
                .map(AwsInstanceTemplateV4Parameters::getSpot)
                .forEach(spot -> {
                    spot.setPercentage(sdxSpotParameters.getPercentage());
                    spot.setMaxPrice(sdxSpotParameters.getMaxPrice());
                });
    }

    @Override
    public Long getResourceIdByResourceCrn(String resourceCrn) {
        String userCrn = ThreadBasedUserCrnProvider.getUserCrn();
        return getByCrn(userCrn, resourceCrn).getId();
    }

    @Override
    public Long getResourceIdByResourceName(String resourceName) {
        String userCrn = ThreadBasedUserCrnProvider.getUserCrn();
        return getByNameInAccount(userCrn, resourceName).getId();
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

    private void validateRazEnablement(SdxClusterRequest sdxClusterRequest, DetailedEnvironmentResponse environment) {
        ValidationResultBuilder validationBuilder = new ValidationResultBuilder();
        if (sdxClusterRequest.isEnableRangerRaz()) {
            boolean razEntitlementEnabled = entitlementService.razEnabled(environment.getCreator(), Crn.safeFromString(environment.getCreator()).getAccountId());
            if (!razEntitlementEnabled) {
                validationBuilder.error("Provisioning Ranger Raz is not enabled for this account.");
            }
            CloudPlatform cloudPlatform = EnumUtils.getEnumIgnoreCase(CloudPlatform.class, environment.getCloudPlatform());
            if (!(AWS.equals(cloudPlatform) || AZURE.equals(cloudPlatform))) {
                validationBuilder.error("Provisioning Ranger Raz is only valid for Amazon Web Services and Microsoft Azure.");
            }
            if (!isRazSupported(sdxClusterRequest.getRuntime(), cloudPlatform)) {
                String errorMsg =  AWS.equals(cloudPlatform) ? "Provisioning Ranger Raz on Amazon Web Services is only valid for CM version >= 7.2.2 and not " :
                        "Provisioning Ranger Raz on Microsoft Azure is only valid for CM version >= 7.2.1 and not ";
                validationBuilder.error(errorMsg + sdxClusterRequest.getRuntime());
            }
        }
        ValidationResult validationResult = validationBuilder.build();
        if (validationResult.hasError()) {
            throw new BadRequestException(validationResult.getFormattedErrors());
        }
    }

    private void validateMediumDutySdxEnablement(SdxClusterRequest sdxClusterRequest, DetailedEnvironmentResponse environment) {
        ValidationResultBuilder validationBuilder = new ValidationResultBuilder();
        if (SdxClusterShape.MEDIUM_DUTY_HA.equals(sdxClusterRequest.getClusterShape())) {
            boolean mediumDutySdxEntitlementEnabled = entitlementService.mediumDutySdxEnabled(environment.getCreator(),
                    Crn.safeFromString(environment.getCreator()).getAccountId());
            if (!mediumDutySdxEntitlementEnabled) {
                validationBuilder.error("Provisioning a medium duty data lake cluster is not enabled for this account. " +
                        "Contact Cloudera support to enable CDP_MEDIUM_DUTY_SDX entitlement for the account.");
            }
            if (!isMediumDutySdxSupported(sdxClusterRequest.getRuntime())) {
                validationBuilder.error("Provisioning a Medium Duty SDX shape is only valid for CM version >= 7.2.2 and not " + sdxClusterRequest.getRuntime());
            }
        }
        ValidationResult validationResult = validationBuilder.build();
        if (validationResult.hasError()) {
            throw new BadRequestException(validationResult.getFormattedErrors());
        }
    }

    /**
     * Ranger Raz is only on 7.2.1 and later on Microsoft Azure, and only on 7.2.2 and later on Amazon Web Services.
     * If runtime is empty, then sdx-internal call was used.
     */
    private boolean isRazSupported(String runtime, CloudPlatform cloudPlatform) {
        if (StringUtils.isEmpty(runtime)) {
            return true;
        }
        Comparator<Versioned> versionComparator = new VersionComparator();
        return versionComparator.compare(() -> runtime, () -> AWS.equals(cloudPlatform) ? "7.2.2" : "7.2.1") > -1;
    }

    /**
     * Medium Duty HA is only on 7.2.2 and later.  If runtime is empty, then sdx-internal call was used.
     */
    private boolean isMediumDutySdxSupported(String runtime) {
        if (StringUtils.isEmpty(runtime)) {
            return true;
        }
        Comparator<Versioned> versionComparator = new VersionComparator();
        return versionComparator.compare(() -> runtime, () -> "7.2.2") > -1;
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
        ValidationResultBuilder validationResultBuilder = ValidationResult.builder();
        if (stackv4Request != null) {
            if (!clusterShape.equals(CUSTOM)) {
                validationResultBuilder.error("Cluster shape '" + clusterShape + "' is not accepted on SDX Internal API. Use 'CUSTOM' cluster shape");
            }
            if (stackv4Request.getCluster() == null) {
                validationResultBuilder.error("Cluster cannot be null.");
            }
        }
        ValidationResult validationResult = validationResultBuilder.build();
        if (validationResult.hasError()) {
            LOGGER.error("Cannot create SDX via internal API: {}", validationResult.getFormattedErrors());
            throw new BadRequestException(validationResult.getFormattedErrors());
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

    public List<SdxCluster> listSdxByEnvCrn(String envCrn) {
        LOGGER.debug("Listing SDX clusters by environment crn {}", envCrn);
        String accountIdFromCrn = getAccountIdFromCrn(envCrn);
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

    public void updateRuntimeVersionFromStackResponse(SdxCluster sdxCluster, StackV4Response stackV4Response) {
        String clusterName = sdxCluster.getClusterName();
        Optional<String> cdpVersionOpt = getCdpVersion(stackV4Response);
        LOGGER.info("Update '{}' runtime version from stackV4Response", clusterName);
        if (cdpVersionOpt.isPresent()) {
            String version = cdpVersionOpt.get();
            LOGGER.info("Update Sdx runtime version of {} to {}, previous version: {}", clusterName, version, sdxCluster.getRuntime());
            sdxCluster.setRuntime(version);
            sdxClusterRepository.save(sdxCluster);
        } else {
            LOGGER.warn("Cannot update the Sdx runtime version for cluster: {}", clusterName);
        }
    }

    private Optional<String> getCdpVersion(StackV4Response stack) {
        Optional<String> result = Optional.empty();
        String stackName = stack.getName();
        ClusterV4Response cluster = stack.getCluster();
        if (cluster != null) {
            ClouderaManagerV4Response cm = cluster.getCm();
            if (cm != null) {
                LOGGER.info("Repository details are available for cluster: {}: {}", stackName, cm);
                List<ClouderaManagerProductV4Response> products = cm.getProducts();
                if (products != null && !products.isEmpty()) {
                    Optional<ClouderaManagerProductV4Response> cdpOpt = products.stream().filter(p -> "CDH".equals(p.getName())).findFirst();
                    if (cdpOpt.isPresent()) {
                        result = getRuntimeVersionFromCdpVersion(cdpOpt.get().getVersion());
                    }
                }
            }
        }
        return result;
    }

    private Optional<String> getRuntimeVersionFromCdpVersion(String cdpVersion) {
        Optional<String> result = Optional.empty();
        if (isNotEmpty(cdpVersion)) {
            LOGGER.info("Extract runtime version from CDP version: {}", cdpVersion);
            result = Optional.of(StringUtils.substringBefore(cdpVersion, "-"));
        }
        return result;
    }

    private FlowIdentifier deleteSdxCluster(SdxCluster sdxCluster, boolean forced) {
        checkIfSdxIsDeletable(sdxCluster, forced);
        MDCBuilder.buildMdcContext(sdxCluster);
        sdxClusterRepository.save(sdxCluster);
        sdxStatusService.setStatusForDatalakeAndNotify(DatalakeStatusEnum.DELETE_REQUESTED, "Datalake deletion requested", sdxCluster);
        FlowIdentifier flowIdentifier = sdxReactorFlowManager.triggerSdxDeletion(sdxCluster, forced);
        flowCancelService.cancelRunningFlows(sdxCluster.getId());
        return flowIdentifier;
    }

    private void checkIfSdxIsDeletable(SdxCluster sdxCluster, boolean forced) {
        if (!forced && sdxCluster.hasExternalDatabase() && Strings.isEmpty(sdxCluster.getDatabaseCrn())) {
            throw new BadRequestException(String.format("Can not find external database for Data Lake: %s", sdxCluster.getClusterName()));
        }
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

    public String getAccountIdFromCrn(String crnStr) {
        try {
            Crn crn = Crn.safeFromString(crnStr);
            return crn.getAccountId();
        } catch (NullPointerException | CrnParseException e) {
            throw new BadRequestException("Can not parse CRN to find account ID: " + crnStr);
        }
    }

    private DetailedEnvironmentResponse getEnvironment(String environmentName) {
        return environmentClientService.getByName(environmentName);
    }

    @Override
    public String getResourceCrnByResourceName(String resourceName) {
        return getByNameInAccount(ThreadBasedUserCrnProvider.getUserCrn(), resourceName).getCrn();
    }

    @Override
    public List<String> getResourceCrnListByResourceNameList(List<String> resourceNames) {
        return resourceNames.stream()
                .map(resourceName -> getByNameInAccount(ThreadBasedUserCrnProvider.getUserCrn(), resourceName).getCrn())
                .collect(Collectors.toList());
    }

    @Override
    public List<String> getResourceCrnsInAccount() {
        return sdxClusterRepository.findAllCrnInAccount(ThreadBasedUserCrnProvider.getAccountId());
    }

    @Override
    public AuthorizationResourceType getResourceType() {
        return AuthorizationResourceType.DATALAKE;
    }

    public SdxCluster save(SdxCluster sdxCluster) {
        return sdxClusterRepository.save(sdxCluster);
    }
}
