package com.sequenceiq.cloudbreak.core.flow2.externaldatabase;

import static com.sequenceiq.cloudbreak.common.mappable.CloudPlatform.AZURE;
import static com.sequenceiq.cloudbreak.rotation.common.RotationPollingSvcOutageUtils.pollWithSvcOutageErrorHandling;
import static com.sequenceiq.common.model.AzureDatabaseType.FLEXIBLE_SERVER;
import static com.sequenceiq.common.model.DatabaseCapabilityType.DEFAULT;
import static java.lang.String.format;

import java.io.IOException;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import jakarta.inject.Inject;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.NotFoundException;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.dyngr.Polling;
import com.dyngr.core.AttemptResult;
import com.dyngr.core.AttemptResults;
import com.dyngr.exception.PollerStoppedException;
import com.dyngr.exception.UserBreakException;
import com.google.common.base.Strings;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.database.DatabaseAvailabilityType;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.cloud.model.StackTags;
import com.sequenceiq.cloudbreak.cmtemplate.CMRepositoryVersionUtil;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessor;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessorFactory;
import com.sequenceiq.cloudbreak.common.database.TargetMajorVersion;
import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.common.type.Versioned;
import com.sequenceiq.cloudbreak.conf.ExternalDatabaseConfig;
import com.sequenceiq.cloudbreak.core.flow2.cluster.rds.upgrade.UpgradeRdsContext;
import com.sequenceiq.cloudbreak.domain.Blueprint;
import com.sequenceiq.cloudbreak.domain.stack.Database;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.rotation.RotationFlowExecutionType;
import com.sequenceiq.cloudbreak.rotation.SecretType;
import com.sequenceiq.cloudbreak.rotation.common.SecretRotationException;
import com.sequenceiq.cloudbreak.service.cluster.ClusterService;
import com.sequenceiq.cloudbreak.service.datalake.SdxClientService;
import com.sequenceiq.cloudbreak.service.environment.EnvironmentClientService;
import com.sequenceiq.cloudbreak.service.externaldatabase.DatabaseOperation;
import com.sequenceiq.cloudbreak.service.externaldatabase.DatabaseServerParameterDecorator;
import com.sequenceiq.cloudbreak.service.externaldatabase.PollingConfig;
import com.sequenceiq.cloudbreak.service.externaldatabase.model.DatabaseServerParameter;
import com.sequenceiq.cloudbreak.service.externaldatabase.model.DatabaseStackConfig;
import com.sequenceiq.cloudbreak.service.externaldatabase.model.DatabaseStackConfigKey;
import com.sequenceiq.cloudbreak.service.rdsconfig.RedbeamsClientService;
import com.sequenceiq.cloudbreak.view.ClusterView;
import com.sequenceiq.cloudbreak.view.StackView;
import com.sequenceiq.common.model.AzureDatabaseType;
import com.sequenceiq.common.model.DatabaseCapabilityType;
import com.sequenceiq.common.model.DatabaseType;
import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;
import com.sequenceiq.environment.api.v1.platformresource.EnvironmentPlatformResourceEndpoint;
import com.sequenceiq.environment.api.v1.platformresource.model.PlatformDatabaseCapabilitiesResponse;
import com.sequenceiq.flow.api.model.FlowCheckResponse;
import com.sequenceiq.flow.api.model.FlowIdentifier;
import com.sequenceiq.flow.api.model.FlowLogResponse;
import com.sequenceiq.flow.api.model.FlowType;
import com.sequenceiq.flow.api.model.StateStatus;
import com.sequenceiq.redbeams.api.endpoint.v4.databaseserver.requests.AllocateDatabaseServerV4Request;
import com.sequenceiq.redbeams.api.endpoint.v4.databaseserver.requests.RotateDatabaseServerSecretV4Request;
import com.sequenceiq.redbeams.api.endpoint.v4.databaseserver.requests.SslConfigV4Request;
import com.sequenceiq.redbeams.api.endpoint.v4.databaseserver.requests.SslMode;
import com.sequenceiq.redbeams.api.endpoint.v4.databaseserver.requests.UpgradeDatabaseServerV4Request;
import com.sequenceiq.redbeams.api.endpoint.v4.databaseserver.requests.UpgradeTargetMajorVersion;
import com.sequenceiq.redbeams.api.endpoint.v4.databaseserver.responses.DatabaseServerV4Response;
import com.sequenceiq.redbeams.api.endpoint.v4.databaseserver.responses.UpgradeDatabaseServerV4Response;
import com.sequenceiq.redbeams.api.endpoint.v4.stacks.DatabaseServerV4StackRequest;
import com.sequenceiq.sdx.api.model.SdxClusterResponse;
import com.sequenceiq.sdx.api.model.SdxDatabaseAvailabilityType;
import com.sequenceiq.sdx.api.model.SdxDatabaseResponse;

@Service
public class ExternalDatabaseService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ExternalDatabaseService.class);

    private static final String SSL_ENFORCEMENT_MIN_RUNTIME_VERSION = "7.2.2";

    @Inject
    private ExternalDatabasePollingConfig dbPollingConfig;

    @Inject
    private RedbeamsClientService redbeamsClient;

    @Inject
    private ClusterService clusterService;

    @Inject
    private Map<DatabaseStackConfigKey, DatabaseStackConfig> dbConfigs;

    @Inject
    private Map<CloudPlatform, DatabaseServerParameterDecorator> parameterDecoratorMap;

    @Inject
    private DatabaseObtainerService databaseObtainerService;

    @Inject
    private ExternalDatabaseConfig externalDatabaseConfig;

    @Inject
    private CmTemplateProcessorFactory cmTemplateProcessorFactory;

    @Inject
    private EnvironmentPlatformResourceEndpoint environmentPlatformResourceEndpoint;

    @Inject
    private SdxClientService sdxClientService;

    @Inject
    private EnvironmentClientService environmentClientService;

    public void provisionDatabase(Stack stack, DetailedEnvironmentResponse environment) {
        String databaseCrn;
        Cluster cluster = stack.getCluster();
        DatabaseAvailabilityType externalDatabase = stack.getExternalDatabaseCreationType();
        LOGGER.info("Create external {} database server in environment {} for DataHub {}", externalDatabase.name(), environment.getName(), cluster.getName());
        try {
            Optional<DatabaseServerV4Response> existingDatabase = findExistingDatabase(stack, stack.getCluster(), environment.getCrn());
            if (existingDatabase.isPresent()) {
                databaseCrn = existingDatabase.get().getCrn();
                LOGGER.debug("Found existing database with CRN {}", databaseCrn);
            } else {
                LOGGER.debug("Requesting new database server creation");
                AllocateDatabaseServerV4Request request = getDatabaseRequest(stack, environment);
                databaseCrn = redbeamsClient.create(request).getResourceCrn();
            }
            updateClusterWithDatabaseServerCrn(cluster, databaseCrn);
        } catch (BadRequestException badRequestException) {
            LOGGER.error("Redbeams create request failed, bad request", badRequestException);
            throw badRequestException;
        }
        waitAndGetDatabase(cluster, databaseCrn, DatabaseOperation.CREATION, true);
    }

    public void terminateDatabase(Cluster cluster, DatabaseAvailabilityType externalDatabase, DetailedEnvironmentResponse environment, boolean forced) {
        LOGGER.info("Terminate external {} database server in environment {} for DataHub {}",
                externalDatabase.name(), environment.getName(), cluster.getName());
        try {
            if (externalDatabaseReferenceExist(cluster.getDatabaseServerCrn())) {
                DatabaseServerV4Response response = redbeamsClient.deleteByCrn(cluster.getDatabaseServerCrn(), forced);
                waitAndGetDatabase(cluster, response.getCrn(), DatabaseOperation.DELETION, false);
            } else {
                LOGGER.warn("[INVESTIGATE] The external database type was {} but there was no crn", externalDatabase);
            }
        } catch (NotFoundException notFoundException) {
            LOGGER.info("Database server is deleted on redbeams side {}", cluster.getDatabaseServerCrn());
        }
    }

    public void startDatabase(Cluster cluster, DatabaseAvailabilityType externalDatabase, DetailedEnvironmentResponse environment) {
        LOGGER.info("Starting external {} database server in environment {} for DataHub {}",
                externalDatabase.name(), environment.getName(), cluster.getName());
        String databaseCrn = cluster.getDatabaseServerCrn();
        try {
            if (externalDatabaseReferenceExist(databaseCrn)) {
                redbeamsClient.startByCrn(databaseCrn);
                waitAndGetDatabase(cluster, databaseCrn, DatabaseOperation.START, false);
            } else {
                LOGGER.warn("[INVESTIGATE] The external database type was {} but there was no crn", externalDatabase);
            }
        } catch (NotFoundException notFoundException) {
            LOGGER.info("Database server not found on redbeams side {}", databaseCrn);
        }
    }

    public void stopDatabase(Cluster cluster, DatabaseAvailabilityType externalDatabase, DetailedEnvironmentResponse environment) {
        LOGGER.info("Stopping external {} database server in environment {} for DataHub {}",
                externalDatabase.name(), environment.getName(), cluster.getName());
        String databaseCrn = cluster.getDatabaseServerCrn();
        try {
            if (externalDatabaseReferenceExist(databaseCrn)) {
                redbeamsClient.stopByCrn(databaseCrn);
                waitAndGetDatabase(cluster, databaseCrn, DatabaseOperation.STOP, false);
            } else {
                LOGGER.warn("[INVESTIGATE] The external database type was {} but there was no crn", externalDatabase);
            }
        } catch (NotFoundException notFoundException) {
            LOGGER.info("Database server not found on redbeams side {}", databaseCrn);
        }
    }

    public void updateToLatestSslCert(Cluster cluster) {
        String databaseCrn = cluster.getDatabaseServerCrn();
        try {
            if (externalDatabaseReferenceExist(databaseCrn)) {
                FlowIdentifier flowIdentifier = redbeamsClient.updateToLatestSslCert(databaseCrn);
                pollUntilFlowFinished(databaseCrn, flowIdentifier);
            }
        } catch (NotFoundException notFoundException) {
            LOGGER.info("Database server not found on redbeams side {}", databaseCrn);
        }
    }

    public void turnOnSslOnProvider(Cluster cluster) {
        String databaseCrn = cluster.getDatabaseServerCrn();
        try {
            if (externalDatabaseReferenceExist(databaseCrn)) {
                redbeamsClient.turnOnSslOnProvider(databaseCrn);
            }
        } catch (NotFoundException notFoundException) {
            LOGGER.info("Database server not found on redbeams side {}", databaseCrn);
        }
    }

    public void rotateSSLCertificate(Cluster cluster) {
        String databaseCrn = cluster.getDatabaseServerCrn();
        try {
            if (externalDatabaseReferenceExist(databaseCrn)) {
                FlowIdentifier flowIdentifier = redbeamsClient.rotateSslCert(databaseCrn);
                pollUntilFlowFinished(databaseCrn, flowIdentifier);
            }
        } catch (NotFoundException notFoundException) {
            LOGGER.info("Database server not found on redbeams side {}", databaseCrn);
        }
    }

    public void migrateRdsToTls(Cluster cluster) {
        String databaseCrn = cluster.getDatabaseServerCrn();
        try {
            if (externalDatabaseReferenceExist(databaseCrn)) {
                redbeamsClient.migrateRdsToTls(databaseCrn);
            }
        } catch (NotFoundException notFoundException) {
            LOGGER.info("Database server not found on redbeams side {}", databaseCrn);
        }
    }

    public FlowIdentifier upgradeDatabase(ClusterView cluster, UpgradeTargetMajorVersion targetMajorVersion, DatabaseServerV4StackRequest newSettings) {
        LOGGER.info("Upgrading external database server to version {} for DataHub {}",
                targetMajorVersion.name(), cluster.getName());
        String databaseCrn = cluster.getDatabaseServerCrn();

        if (externalDatabaseReferenceExist(databaseCrn)) {
            try {
                UpgradeDatabaseServerV4Request request = new UpgradeDatabaseServerV4Request();
                request.setUpgradedDatabaseSettings(newSettings);
                request.setUpgradeTargetMajorVersion(targetMajorVersion);
                UpgradeDatabaseServerV4Response response = redbeamsClient.upgradeByCrn(databaseCrn, request);
                if (response.getFlowIdentifier() == null) {
                    LOGGER.info(response.getReason());
                }
                return response.getFlowIdentifier();
            } catch (NotFoundException notFoundException) {
                LOGGER.info("Database server not found on redbeams side {}", databaseCrn);
                return null;
            }
        } else {
            LOGGER.warn("[INVESTIGATE] The external database crn reference was not present");
            return null;
        }
    }

    public DatabaseServerV4Response waitForDatabaseFlowToBeFinished(ClusterView cluster, FlowIdentifier flowIdentifier) {
        if (flowIdentifier != null) {
            return pollUntilFlowFinished(cluster.getDatabaseServerCrn(), flowIdentifier);
        }
        return null;
    }

    public void rotateDatabaseSecret(String databaseServerCrn, SecretType secretType, RotationFlowExecutionType executionType,
            Map<String, String> additionalProperties) {
        LOGGER.info("Rotating external database server secret: {} for database server {}", secretType, databaseServerCrn);
        RotateDatabaseServerSecretV4Request request = new RotateDatabaseServerSecretV4Request();
        request.setCrn(databaseServerCrn);
        request.setSecret(secretType.value());
        request.setExecutionType(executionType);
        request.setAdditionalProperties(additionalProperties);
        FlowIdentifier flowIdentifier = redbeamsClient.rotateSecret(request);
        if (flowIdentifier == null) {
            handleUnsuccessfulFlow(databaseServerCrn, flowIdentifier, null);
        } else {
            pollWithSvcOutageErrorHandling(() -> pollUntilFlowFinished(databaseServerCrn, flowIdentifier), PollerStoppedException.class);
        }
    }

    public void preValidateDatabaseSecretRotation(String databaseServerCrn) {
        if (StringUtils.isEmpty(databaseServerCrn)) {
            throw new SecretRotationException("No database server crn found, rotation is not possible.", null);
        }
        FlowLogResponse lastFlow = redbeamsClient.getLastFlowId(databaseServerCrn);
        if (lastFlow != null && lastFlow.getStateStatus() == StateStatus.PENDING) {
            String message = format("Polling in Redbeams is not possible since last known state of flow for the database is %s", lastFlow.getCurrentState());
            throw new SecretRotationException(message, null);
        }
    }

    private DatabaseServerV4Response pollUntilFlowFinished(String databaseCrn, FlowIdentifier flowIdentifier) {
        try {
            PollingConfig pollingConfig = dbPollingConfig.getConfig();
            Boolean success = Polling.waitPeriodly(pollingConfig.getSleepTime(), pollingConfig.getSleepTimeUnit())
                    .stopIfException(pollingConfig.getStopPollingIfExceptionOccured())
                    .stopAfterDelay(pollingConfig.getTimeout(), pollingConfig.getTimeoutTimeUnit())
                    .run(() -> pollFlowState(flowIdentifier));
            if (success == null || !success) {
                String errorDescription;
                try {
                    DatabaseServerV4Response rdsStatus = redbeamsClient.getByCrn(databaseCrn);
                    LOGGER.info("Response from redbeams: {}", rdsStatus);
                    errorDescription = rdsStatus.getStatusReason();
                } catch (CloudbreakServiceException | NotFoundException e) {
                    errorDescription = e.getMessage();
                    LOGGER.info("Error {} returned for database crn: {}", errorDescription, databaseCrn);
                }
                handleUnsuccessfulFlow(databaseCrn, flowIdentifier, new UserBreakException(errorDescription));
            } else {
                try {
                    return redbeamsClient.getByCrn(databaseCrn);
                } catch (CloudbreakServiceException | NotFoundException e) {
                    LOGGER.info("Error {} returned for database crn: {}", e.getMessage(), databaseCrn);
                    return null;
                }
            }
        } catch (UserBreakException e) {
            handleUnsuccessfulFlow(databaseCrn, flowIdentifier, e);
        }
        return null;
    }

    private void handleUnsuccessfulFlow(String databaseCrn, FlowIdentifier flowIdentifier, UserBreakException e) {
        String message = format("Database flow failed in Redbeams with error: '%s'. Database crn: %s, flow: %s",
                e != null ? e.getMessage() : "unknown", databaseCrn, flowIdentifier);
        LOGGER.warn(message);
        throw new CloudbreakServiceException(message, e);
    }

    private AttemptResult<Boolean> pollFlowState(FlowIdentifier flowIdentifier) {
        FlowCheckResponse flowState;
        if (flowIdentifier.getType() == FlowType.NOT_TRIGGERED) {
            return AttemptResults.breakFor(format("Flow %s not triggered", flowIdentifier.getPollableId()));
        } else if (flowIdentifier.getType() == FlowType.FLOW) {
            flowState = redbeamsClient.hasFlowRunningByFlowId(flowIdentifier.getPollableId());
        } else if (flowIdentifier.getType() == FlowType.FLOW_CHAIN) {
            flowState = redbeamsClient.hasFlowChainRunningByFlowChainId(flowIdentifier.getPollableId());
        } else {
            String message = format("Unknown flow identifier type %s for flow: %s", flowIdentifier.getType(), flowIdentifier);
            LOGGER.error(message);
            throw new CloudbreakServiceException(message);
        }

        LOGGER.debug("Database polling has active flow: {}, with latest fail: {}",
                flowState.getHasActiveFlow(), flowState.getLatestFlowFinalizedAndFailed());
        return flowState.getHasActiveFlow()
                ? AttemptResults.justContinue()
                : AttemptResults.finishWith(!flowState.getLatestFlowFinalizedAndFailed());
    }

    public Optional<DatabaseServerV4Response> findExistingDatabase(StackView stack, ClusterView clusterView, String environmentCrn) {
        String stackCrn = stack.getResourceCrn();
        LOGGER.debug("Trying to find existing database server for environment {} and cluster {}", environmentCrn, stackCrn);
        try {
            return Optional.ofNullable(redbeamsClient.getByClusterCrn(environmentCrn, stackCrn));
        } catch (NotFoundException ignore) {
            LOGGER.debug("External database in environment {} for cluster {} does not exist.", environmentCrn, clusterView);
            return Optional.empty();
        }
    }

    public Optional<DatabaseServerV4Response> findExistingDatabase(StackDto stackDto) {
        String stackCrn = stackDto.getResourceCrn();
        String environmentCrn = stackDto.getEnvironmentCrn();
        LOGGER.debug("Trying to find existing database server for environment {} and cluster {}", environmentCrn, stackCrn);
        try {
            return Optional.ofNullable(redbeamsClient.getByClusterCrn(environmentCrn, stackCrn));
        } catch (NotFoundException ignore) {
            LOGGER.debug("External database in environment {} for resource {} does not exist.", environmentCrn, stackCrn);
            return Optional.empty();
        }
    }

    private boolean externalDatabaseReferenceExist(String databaseCrn) {
        return !Strings.isNullOrEmpty(databaseCrn);
    }

    private void updateClusterWithDatabaseServerCrn(Cluster cluster, String databaseServerCrn) {
        cluster.setDatabaseServerCrn(databaseServerCrn);
        clusterService.save(cluster);
    }

    private AllocateDatabaseServerV4Request getDatabaseRequest(Stack stack, DetailedEnvironmentResponse environment) {
        AllocateDatabaseServerV4Request req = new AllocateDatabaseServerV4Request();
        req.setEnvironmentCrn(environment.getCrn());
        CloudPlatform cloudPlatform = CloudPlatform.valueOf(environment.getCloudPlatform().toUpperCase(Locale.ROOT));
        String databaseEngineVersion = stack.getExternalDatabaseEngineVersion();
        req.setDatabaseServer(getDatabaseServerStackRequest(cloudPlatform, stack.getExternalDatabaseCreationType(), databaseEngineVersion,
                getAttributes(stack.getDatabase()), environment, stack.isMultiAz()));
        req.setClusterCrn(stack.getResourceCrn());
        req.setTags(getUserDefinedTags(stack));
        configureSslEnforcement(req, cloudPlatform, stack.getCluster());
        return req;
    }

    private Map<String, Object> getAttributes(Database database) {
        return Optional.ofNullable(database).map(Database::getAttributes).map(Json::getMap).orElse(new HashMap<>());
    }

    private void configureSslEnforcement(AllocateDatabaseServerV4Request req, CloudPlatform cloudPlatform, Cluster cluster) {
        String runtime = getRuntime(cluster);
        boolean dbSslEnabled = cluster.getDbSslEnabled() != null && cluster.getDbSslEnabled();
        if (externalDatabaseConfig.isExternalDatabaseSslEnforcementSupportedFor(cloudPlatform) && isSslEnforcementSupportedForRuntime(runtime) && dbSslEnabled) {
            LOGGER.info("Applying external DB SSL enforcement for cloud platform {} and runtime version {}", cloudPlatform, runtime);
            SslConfigV4Request sslConfigV4Request = new SslConfigV4Request();
            sslConfigV4Request.setSslMode(SslMode.ENABLED);
            req.setSslConfig(sslConfigV4Request);
        } else {
            LOGGER.info("Skipping external DB SSL enforcement for cloud platform {} and runtime version {}", cloudPlatform, runtime);
        }
    }

    private String getRuntime(Cluster cluster) {
        String runtime = null;
        Optional<String> blueprintTextOpt = Optional.ofNullable(cluster.getBlueprint()).map(Blueprint::getBlueprintJsonText);
        if (blueprintTextOpt.isPresent()) {
            CmTemplateProcessor cmTemplateProcessor = cmTemplateProcessorFactory.get(blueprintTextOpt.get());
            runtime = cmTemplateProcessor.getStackVersion();
            LOGGER.info("Blueprint text is available for stack, found runtime version '{}'", runtime);
        } else {
            LOGGER.warn("Blueprint text is unavailable for stack, thus runtime version cannot be determined.");
        }
        return runtime;
    }

    private boolean isSslEnforcementSupportedForRuntime(String runtime) {
        if (StringUtils.isBlank(runtime)) {
            // While this may happen for custom data lakes, it is not possible for DH clusters
            LOGGER.info("Runtime version is NOT specified, external DB SSL enforcement is NOT permitted");
            return false;
        }
        boolean permitted = CMRepositoryVersionUtil.isVersionNewerOrEqualThanLimited(() -> runtime, () -> SSL_ENFORCEMENT_MIN_RUNTIME_VERSION);
        LOGGER.info("External DB SSL enforcement {} permitted for runtime version {}", permitted ? "is" : "is NOT", runtime);
        return permitted;
    }

    private Map<String, String> getUserDefinedTags(Stack stack) {
        Map<String, String> userDefinedTags = new HashMap<>();
        if (stack.getTags() != null) {
            try {
                StackTags stackTag = stack.getTags().get(StackTags.class);
                if (stackTag != null) {
                    userDefinedTags = stackTag.getUserDefinedTags();
                }
            } catch (IOException e) {
                LOGGER.warn("Stack related applications tags cannot be parsed, use default service type for metering.", e);
            }
        }
        return Objects.requireNonNullElse(userDefinedTags, new HashMap<>());
    }

    private DatabaseServerV4StackRequest getDatabaseServerStackRequest(CloudPlatform cloudPlatform, DatabaseAvailabilityType externalDatabase,
            String databaseEngineVersion, Map<String, Object> attributes, DetailedEnvironmentResponse environment, boolean multiAz) {
        DatabaseServerParameterDecorator databaseServerParameterDecorator = parameterDecoratorMap.get(cloudPlatform);
        DatabaseType databaseType = databaseServerParameterDecorator.getDatabaseType(attributes).orElse(null);
        DatabaseStackConfig databaseStackConfig = dbConfigs.get(new DatabaseStackConfigKey(cloudPlatform, databaseType));
        DatabaseCapabilityType databaseCapabilityType = AZURE.equals(cloudPlatform) ? getAzureDatabaseCapability(databaseType) : DEFAULT;
        PlatformDatabaseCapabilitiesResponse databaseCapabilities = getDatabaseCapabilities(environment, databaseCapabilityType);
        if (databaseStackConfig == null) {
            throw new BadRequestException("Database config for cloud platform " + cloudPlatform + " not found");
        } else {
            DatabaseServerV4StackRequest request = new DatabaseServerV4StackRequest();
            request.setInstanceType(databaseCapabilities.getRegionDefaultInstances().get(environment.getLocation().getName()));
            request.setDatabaseVendor(databaseStackConfig.getVendor());
            request.setStorageSize(databaseStackConfig.getVolumeSize());
            DatabaseServerParameter serverParameter = DatabaseServerParameter.builder()
                    .withAvailabilityType(externalDatabase)
                    .withEngineVersion(databaseEngineVersion)
                    .withAttributes(attributes)
                    .build();
            databaseServerParameterDecorator.setParameters(request, serverParameter, environment, multiAz);
            databaseServerParameterDecorator.validate(request, serverParameter, environment, multiAz);
            if (Objects.isNull(request.getCloudPlatform())) {
                request.setCloudPlatform(cloudPlatform);
            }
            return request;
        }
    }

    private DatabaseCapabilityType getAzureDatabaseCapability(DatabaseType databaseType) {
        return FLEXIBLE_SERVER.equals(databaseType) ? DatabaseCapabilityType.AZURE_FLEXIBLE : DatabaseCapabilityType.AZURE_SINGLE_SERVER;
    }

    private PlatformDatabaseCapabilitiesResponse getDatabaseCapabilities(DetailedEnvironmentResponse env, DatabaseCapabilityType databaseType) {
        String initiatorUserCrn = ThreadBasedUserCrnProvider.getUserCrn();
        return ThreadBasedUserCrnProvider.doAs(initiatorUserCrn, () ->
                environmentPlatformResourceEndpoint.getDatabaseCapabilities(
                        env.getCrn(),
                        env.getLocation().getName(),
                        env.getCloudPlatform(),
                        null,
                        databaseType,
                        null));
    }

    private void waitAndGetDatabase(ClusterView cluster, String databaseCrn,
            DatabaseOperation databaseOperation, boolean cancellable) {
        waitAndGetDatabase(cluster, databaseCrn, dbPollingConfig.getConfig(), databaseOperation, cancellable);
    }

    private void waitAndGetDatabase(ClusterView cluster, String databaseCrn, PollingConfig pollingConfig, DatabaseOperation databaseOperation,
            boolean cancellable) {
        Polling.waitPeriodly(pollingConfig.getSleepTime(), pollingConfig.getSleepTimeUnit())
                .stopIfException(pollingConfig.getStopPollingIfExceptionOccured())
                .stopAfterDelay(pollingConfig.getTimeout(), pollingConfig.getTimeoutTimeUnit())
                .run(() -> databaseObtainerService.obtainAttemptResult(cluster, databaseOperation, databaseCrn, cancellable));
    }

    public boolean isMigrationNeededDuringUpgrade(UpgradeRdsContext context) {
        CloudPlatform cloudPlatform = CloudPlatform.valueOf(context.getStack().getCloudPlatform());
        boolean externalDb = context.getCluster().hasExternalDatabase();
        Database database = context.getDatabase();
        Versioned currentVersion = database::getExternalDatabaseEngineVersion;
        Versioned targetVersion = context.getVersion()::getMajorVersion;
        DatabaseType databaseType = parameterDecoratorMap.get(cloudPlatform).getDatabaseType(database.getAttributesMap()).orElse(null);
        return isMigrationNeededDuringUpgrade(currentVersion, targetVersion, cloudPlatform, externalDb, databaseType);
    }

    private boolean isMigrationNeededDuringUpgrade(Versioned currentVersion, Versioned targetVersion, CloudPlatform cloudPlatform, boolean externalDb,
            DatabaseType databaseType) {
        boolean upgradeTargetVersionImpliesFlexibleServerMigration =
                CMRepositoryVersionUtil.isVersionNewerOrEqualThanLimited(targetVersion, TargetMajorVersion.VERSION_14::getMajorVersion);
        boolean currentVersionImpliesFlexibleServerMigration =
                CMRepositoryVersionUtil.isVersionOlderThanLimited(currentVersion, TargetMajorVersion.VERSION_14::getMajorVersion);
        boolean migrationNeeded = externalDb && cloudPlatform == AZURE && upgradeTargetVersionImpliesFlexibleServerMigration &&
                currentVersionImpliesFlexibleServerMigration;
        String migrationNeededMsg = migrationNeeded ? "Database settings migration is needed during upgrade." :
                "Database settings migration is not needed during upgrade.";
        LOGGER.debug("{} Current version: {}, target version: {}, cloudPlatform: {}, externalDb: {}, databaseType: {}",
                migrationNeededMsg, currentVersion.getVersion(), targetVersion.getVersion(), cloudPlatform, externalDb, databaseType);
        return migrationNeeded;
    }

    public DatabaseServerV4StackRequest migrateDatabaseSettingsIfNeeded(StackDto stack, TargetMajorVersion majorVersion) {
        DetailedEnvironmentResponse environment = environmentClientService.getByCrn(stack.getEnvironmentCrn());
        CloudPlatform cloudPlatform = CloudPlatform.valueOf(stack.getCloudPlatform());
        Versioned currentVersion = stack::getExternalDatabaseEngineVersion;
        Versioned targetVersion = majorVersion::getMajorVersion;
        boolean externalDb = stack.getCluster().hasExternalDatabase();
        DatabaseType databaseType = parameterDecoratorMap.get(cloudPlatform).getDatabaseType(stack.getDatabase().getAttributesMap()).orElse(null);

        if (isMigrationNeededDuringUpgrade(currentVersion, targetVersion, cloudPlatform, externalDb, databaseType)) {
            DatabaseAvailabilityType databaseAvailabilityType = fetchDatabaseAvailabilityType(stack);

            Map<String, Object> attributes = getAttributes(stack.getDatabase());
            attributes.put(AzureDatabaseType.AZURE_DATABASE_TYPE_KEY, AzureDatabaseType.FLEXIBLE_SERVER.name());
            DatabaseServerV4StackRequest modifiedRequest = getDatabaseServerStackRequest(
                    cloudPlatform,
                    databaseAvailabilityType,
                    majorVersion.getMajorVersion(),
                    attributes, environment, stack.getStack().isMultiAz());
            LOGGER.debug("Migration resulted in request: {}", modifiedRequest);
            return modifiedRequest;
        }
        LOGGER.debug("Migration was not needed, original version: {}, target version: {}, cloud platform: {}, externalDb: {}",
                currentVersion, targetVersion, cloudPlatform, externalDb);
        return null;
    }

    private DatabaseAvailabilityType fetchDatabaseAvailabilityType(StackDto stack) {
        if (stack.getStack().isDatalake()) {
            SdxDatabaseAvailabilityType sdxDatabaseAvailabilityType = fetchAvailabilityTypeFromDl(stack);
            return DatabaseAvailabilityType.valueOf(sdxDatabaseAvailabilityType.name());
        } else {
            return stack.getExternalDatabaseCreationType();
        }
    }

    private SdxDatabaseAvailabilityType fetchAvailabilityTypeFromDl(StackDto stack) {
        try {
            SdxClusterResponse datalake = sdxClientService.getByCrnInternal(stack.getResourceCrn());
            return Optional.ofNullable(datalake)
                    .map(SdxClusterResponse::getSdxDatabaseResponse)
                    .map(SdxDatabaseResponse::getAvailabilityType)
                    .orElse(SdxDatabaseAvailabilityType.NONE);
        } catch (Exception e) {
            LOGGER.error("Fetching database availability type from DL service failed", e);
            return SdxDatabaseAvailabilityType.NONE;
        }
    }
}