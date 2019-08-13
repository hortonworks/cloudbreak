package com.sequenceiq.datalake.service.sdx;

import static com.sequenceiq.cloudbreak.exception.NotFoundException.notFound;
import static com.sequenceiq.sdx.api.model.SdxClusterShape.CUSTOM;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.ws.rs.ProcessingException;
import javax.ws.rs.WebApplicationException;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.StackV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackViewV4Response;
import com.sequenceiq.cloudbreak.auth.altus.Crn;
import com.sequenceiq.cloudbreak.auth.altus.CrnParseException;
import com.sequenceiq.cloudbreak.client.CloudbreakServiceUserCrnClient;
import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.cloudbreak.common.json.JsonUtil;
import com.sequenceiq.cloudbreak.common.service.Clock;
import com.sequenceiq.cloudbreak.event.ResourceEvent;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.cloudbreak.util.FileReaderUtils;
import com.sequenceiq.cloudbreak.validation.ValidationResult;
import com.sequenceiq.cloudbreak.validation.ValidationResult.ValidationResultBuilder;
import com.sequenceiq.common.api.cloudstorage.CloudStorageRequest;
import com.sequenceiq.common.api.telemetry.request.LoggingRequest;
import com.sequenceiq.common.api.telemetry.request.TelemetryRequest;
import com.sequenceiq.common.model.FileSystemType;
import com.sequenceiq.datalake.controller.exception.BadRequestException;
import com.sequenceiq.datalake.entity.SdxCluster;
import com.sequenceiq.datalake.entity.DatalakeStatusEnum;
import com.sequenceiq.datalake.entity.SdxStatusEntity;
import com.sequenceiq.datalake.flow.SdxReactorFlowManager;
import com.sequenceiq.datalake.repository.SdxClusterRepository;
import com.sequenceiq.datalake.service.sdx.status.SdxStatusService;
import com.sequenceiq.datalake.service.validation.cloudstorage.CloudStorageLocationValidator;
import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;
import com.sequenceiq.environment.client.EnvironmentServiceCrnClient;
import com.sequenceiq.sdx.api.model.SdxCloudStorageRequest;
import com.sequenceiq.sdx.api.model.SdxClusterRequest;
import com.sequenceiq.sdx.api.model.SdxClusterShape;

@Service
public class SdxService {

    private static final Logger LOGGER = LoggerFactory.getLogger(SdxService.class);

    @Inject
    private SdxClusterRepository sdxClusterRepository;

    @Inject
    private SdxReactorFlowManager sdxReactorFlowManager;

    @Inject
    private EnvironmentServiceCrnClient environmentServiceCrnClient;

    @Inject
    private CloudbreakServiceUserCrnClient cloudbreakClient;

    @Inject
    private CloudStorageManifester cloudStorageManifester;

    @Inject
    private SdxStatusService sdxStatusService;

    @Inject
    private Clock clock;

    @Inject
    private CloudStorageLocationValidator cloudStorageLocationValidator;

    @Inject
    private SdxNotificationService notificationService;

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

    public StackV4Response getDetail(String userCrn, String name, Set<String> entries) {
        try {
            LOGGER.info("Calling cloudbreak for SDX cluster details by name {}", name);
            return cloudbreakClient.withCrn(userCrn).stackV4Endpoint().get(0L, name, entries);
        } catch (javax.ws.rs.NotFoundException e) {
            LOGGER.info("Sdx cluster not found on CB side", e);
            return null;
        }
    }

    private Set<String> getAttachedDistroXClusterNames(String userCrn, String environmentName, String environmentCrn) {
        Set<String> clusterNames = new HashSet<>();
        LOGGER.debug("Get DistroX clusters of the environment: '{}'", environmentName);
        try {
            Set<String> distroXClusterNames = cloudbreakClient
                    .withCrn(userCrn)
                    .distroXV1Endpoint()
                    .list(environmentName, environmentCrn)
                    .getResponses()
                    .stream()
                    .map(StackViewV4Response::getName)
                    .collect(Collectors.toSet());
            clusterNames.addAll(distroXClusterNames);
        } catch (WebApplicationException | ProcessingException e) {
            String message = String.format("Failed to get DistroX clusters from Cloudbreak service due to: '%s' ", e.getMessage());
            LOGGER.error(message, e);
            throw e;
        }
        return clusterNames;
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
        Optional<SdxCluster> sdxCluster = sdxClusterRepository.findByAccountIdAndClusterNameAndDeletedIsNull(accountIdFromCrn, name);
        if (sdxCluster.isPresent()) {
            return sdxCluster.get();
        } else {
            throw notFound("SDX cluster", name).get();
        }
    }

    public SdxCluster createSdx(final String userCrn, final String name, final SdxClusterRequest sdxClusterRequest, StackV4Request stackV4Request) {
        LOGGER.info("Creating SDX cluster with name {}", name);
        validateSdxRequest(name, sdxClusterRequest.getEnvironment(), getAccountIdFromCrn(userCrn));
        validateInternalSdxRequest(stackV4Request, sdxClusterRequest.getClusterShape());
        DetailedEnvironmentResponse environment = getEnvironment(userCrn, sdxClusterRequest);
        validateCloudStorageRequest(userCrn, sdxClusterRequest.getCloudStorage(), environment);
        SdxCluster sdxCluster = new SdxCluster();
        sdxCluster.setInitiatorUserCrn(userCrn);
        sdxCluster.setCrn(createCrn(getAccountIdFromCrn(userCrn)));
        sdxCluster.setClusterName(name);
        sdxCluster.setAccountId(getAccountIdFromCrn(userCrn));
        sdxCluster.setClusterShape(sdxClusterRequest.getClusterShape());
        sdxCluster.setCreated(clock.getCurrentTimeMillis());
        sdxCluster.setCreateDatabase(sdxClusterRequest.getExternalDatabase() != null && sdxClusterRequest.getExternalDatabase().getCreate());

        createDatabaseByDefaultForAWS(sdxClusterRequest, sdxCluster, environment);
        validateDatabaseRequest(sdxCluster, environment);
        sdxCluster.setEnvName(environment.getName());
        sdxCluster.setEnvCrn(environment.getCrn());
        if (isCloudStorageConfigured(sdxClusterRequest)) {
            sdxCluster.setCloudStorageBaseLocation(sdxClusterRequest.getCloudStorage().getBaseLocation());
            sdxCluster.setCloudStorageFileSystemType(sdxClusterRequest.getCloudStorage().getFileSystemType());
        }

        setTagsSafe(sdxClusterRequest, sdxCluster);
        stackV4Request = prepareStackRequest(sdxClusterRequest, stackV4Request, sdxCluster, environment);

        try {
            sdxCluster.setStackRequest(JsonUtil.writeValueAsString(stackV4Request));
        } catch (JsonProcessingException e) {
            LOGGER.error("Can not parse internal stackrequest", e);
            throw new BadRequestException("Can not parse internal stackrequest", e);
        }

        MDCBuilder.buildMdcContext(sdxCluster);

        sdxCluster = sdxClusterRepository.save(sdxCluster);
        sdxStatusService.setStatusForDatalake(DatalakeStatusEnum.REQUESTED, "Datalake requested", sdxCluster);

        notificationService.send(ResourceEvent.SDX_CLUSTER_CREATED, sdxCluster);

        LOGGER.info("trigger SDX creation: {}", sdxCluster);
        sdxReactorFlowManager.triggerSdxCreation(sdxCluster.getId());

        return sdxCluster;
    }

    private void createDatabaseByDefaultForAWS(SdxClusterRequest sdxClusterRequest, SdxCluster sdxCluster, DetailedEnvironmentResponse environment) {
        if ("AWS".equals(environment.getCloudPlatform()) &&
                (sdxClusterRequest.getExternalDatabase() == null ||
                        sdxClusterRequest.getExternalDatabase().getCreate() == null)) {
            sdxCluster.setCreateDatabase(true);
        }
    }

    private void validateCloudStorageRequest(String userCrn, SdxCloudStorageRequest cloudStorage, DetailedEnvironmentResponse environment) {
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
                    cloudStorageLocationValidator.validate(userCrn, cloudStorage.getBaseLocation(), environment, validationBuilder);
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

    private void validateDatabaseRequest(SdxCluster sdxCluster, DetailedEnvironmentResponse environment) {
        if (sdxCluster.isCreateDatabase() && !"AWS".equals(environment.getCloudPlatform())) {
            String message = String.format("Cannot create external database for sdx: %s, for now only AWS is supported", sdxCluster.getClusterName());
            LOGGER.debug(message);
            throw new BadRequestException(message);
        }
    }

    private StackV4Request prepareStackRequest(SdxClusterRequest sdxClusterRequest, StackV4Request stackV4Request,
            SdxCluster sdxCluster, DetailedEnvironmentResponse environment) {
        stackV4Request = getStackRequest(stackV4Request, sdxClusterRequest.getClusterShape(), environment.getCloudPlatform());
        CloudStorageRequest cloudStorageRequest = cloudStorageManifester.initCloudStorageRequest(environment,
                stackV4Request.getCluster(), sdxCluster, sdxClusterRequest);
        stackV4Request.getCluster().setCloudStorage(cloudStorageRequest);
        prepareTelemetryForStack(stackV4Request, environment);
        return stackV4Request;
    }

    private void prepareTelemetryForStack(StackV4Request stackV4Request, DetailedEnvironmentResponse environment) {
        if (environment.getTelemetry() != null && environment.getTelemetry().getLogging() != null) {
            TelemetryRequest telemetryRequest = new TelemetryRequest();
            LoggingRequest loggingRequest = new LoggingRequest();
            loggingRequest.setS3(environment.getTelemetry().getLogging().getS3());
            loggingRequest.setWasb(environment.getTelemetry().getLogging().getWasb());
            loggingRequest.setStorageLocation(environment.getTelemetry().getLogging().getStorageLocation());
            telemetryRequest.setLogging(loggingRequest);
            telemetryRequest.setReportDeploymentLogs(
                    environment.getTelemetry().getReportDeploymentLogs());
            stackV4Request.setTelemetry(telemetryRequest);
        }
    }

    private boolean isCloudStorageConfigured(SdxClusterRequest clusterRequest) {
        return clusterRequest.getCloudStorage() != null
                && StringUtils.isNotEmpty(clusterRequest.getCloudStorage().getBaseLocation());
    }

    private StackV4Request getStackRequest(@Nullable StackV4Request internalStackRequest, SdxClusterShape shape, String cloudPlatform) {
        if (internalStackRequest == null) {
            String clusterTemplatePath = generateClusterTemplatePath(cloudPlatform, shape);
            LOGGER.info("Using path of {} based on Cloudplatform {} and Shape {}", clusterTemplatePath, cloudPlatform, shape);
            return getStackRequestFromFile(clusterTemplatePath);
        } else {
            return internalStackRequest;
        }
    }

    protected StackV4Request getStackRequestFromFile(String templatePath) {
        try {
            String clusterTemplateJson = FileReaderUtils.readFileFromClasspath(templatePath);
            return JsonUtil.readValue(clusterTemplateJson, StackV4Request.class);
        } catch (IOException e) {
            LOGGER.info("Can not read template from path: " + templatePath, e);
            throw new BadRequestException("Can not read template from path: " + templatePath);
        }
    }

    protected String generateClusterTemplatePath(String cloudPlatform, SdxClusterShape shape) {
        String convertedShape = shape.toString().toLowerCase().replaceAll("_", "-");
        return "sdx/" + cloudPlatform.toLowerCase() + "/cluster-" + convertedShape + "-template.json";
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
            sdxCluster.setTags(new Json(sdxClusterRequest.getTags()));
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

    public void deleteSdxByClusterCrn(String userCrn, String clusterCrn) {
        LOGGER.info("Deleting SDX {}", clusterCrn);
        String accountIdFromCrn = getAccountIdFromCrn(userCrn);
        sdxClusterRepository.findByAccountIdAndCrnAndDeletedIsNull(accountIdFromCrn, clusterCrn).ifPresentOrElse(this::deleteSdxCluster, () -> {
            throw notFound("SDX cluster", clusterCrn).get();
        });
    }

    public void deleteSdx(String userCrn, String name) {
        LOGGER.info("Deleting SDX {}", name);
        String accountIdFromCrn = getAccountIdFromCrn(userCrn);
        sdxClusterRepository.findByAccountIdAndClusterNameAndDeletedIsNull(accountIdFromCrn, name).ifPresentOrElse(this::deleteSdxCluster, () -> {
            throw notFound("SDX cluster", name).get();
        });
    }

    private void deleteSdxCluster(SdxCluster sdxCluster) {
        checkIfSdxIsDeletable(sdxCluster);
        MDCBuilder.buildMdcContext(sdxCluster);
        sdxClusterRepository.save(sdxCluster);
        sdxStatusService.setStatusForDatalake(DatalakeStatusEnum.DELETE_REQUESTED, "Datalake deletion requested", sdxCluster);
        sdxReactorFlowManager.triggerSdxDeletion(sdxCluster.getId());
        sdxReactorFlowManager.cancelRunningFlows(sdxCluster.getId());
        LOGGER.info("SDX delete triggered: {}", sdxCluster.getClusterName());
    }

    private void checkIfSdxIsDeletable(SdxCluster sdxCluster) {
        Set<String> attachedDistroXClusterNames =
                getAttachedDistroXClusterNames(sdxCluster.getInitiatorUserCrn(), sdxCluster.getEnvName(), sdxCluster.getEnvCrn());
        if (!attachedDistroXClusterNames.isEmpty()) {
            throw new BadRequestException(String.format("The following Data Hub cluster(s) must be terminated before SDX deletion [%s]",
                    String.join(", ", attachedDistroXClusterNames)));
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

    private DetailedEnvironmentResponse getEnvironment(String userCrn, SdxClusterRequest sdxClusterRequest) {
        return environmentServiceCrnClient
                .withCrn(userCrn)
                .environmentV1Endpoint()
                .getByName(sdxClusterRequest.getEnvironment());
    }
}
