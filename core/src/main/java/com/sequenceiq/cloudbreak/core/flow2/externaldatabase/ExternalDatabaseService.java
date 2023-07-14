package com.sequenceiq.cloudbreak.core.flow2.externaldatabase;

import java.io.IOException;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import javax.ws.rs.BadRequestException;
import javax.ws.rs.NotFoundException;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.dyngr.Polling;
import com.dyngr.core.AttemptResult;
import com.dyngr.core.AttemptResults;
import com.dyngr.exception.UserBreakException;
import com.google.common.base.Strings;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.database.DatabaseAvailabilityType;
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
import com.sequenceiq.cloudbreak.domain.Blueprint;
import com.sequenceiq.cloudbreak.domain.stack.Database;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.repository.cluster.ClusterRepository;
import com.sequenceiq.cloudbreak.rotation.RotationFlowExecutionType;
import com.sequenceiq.cloudbreak.rotation.SecretType;
import com.sequenceiq.cloudbreak.rotation.common.SecretRotationException;
import com.sequenceiq.cloudbreak.service.externaldatabase.DatabaseOperation;
import com.sequenceiq.cloudbreak.service.externaldatabase.DatabaseServerParameterDecorator;
import com.sequenceiq.cloudbreak.service.externaldatabase.PollingConfig;
import com.sequenceiq.cloudbreak.service.externaldatabase.model.DatabaseServerParameter;
import com.sequenceiq.cloudbreak.service.externaldatabase.model.DatabaseStackConfig;
import com.sequenceiq.cloudbreak.service.externaldatabase.model.DatabaseStackConfigKey;
import com.sequenceiq.cloudbreak.service.rdsconfig.RedbeamsClientService;
import com.sequenceiq.cloudbreak.view.ClusterView;
import com.sequenceiq.common.model.AzureDatabaseType;
import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;
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

@Service
public class ExternalDatabaseService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ExternalDatabaseService.class);

    private static final String SSL_ENFORCEMENT_MIN_RUNTIME_VERSION = "7.2.2";

    private final PollingConfig dbPollingConfig;

    private final RedbeamsClientService redbeamsClient;

    private final ClusterRepository clusterRepository;

    private final Map<DatabaseStackConfigKey, DatabaseStackConfig> dbConfigs;

    private final Map<CloudPlatform, DatabaseServerParameterDecorator> parameterDecoratorMap;

    private final DatabaseObtainerService databaseObtainerService;

    private final ExternalDatabaseConfig externalDatabaseConfig;

    private final CmTemplateProcessorFactory cmTemplateProcessorFactory;

    public ExternalDatabaseService(RedbeamsClientService redbeamsClient, ClusterRepository clusterRepository,
            Map<DatabaseStackConfigKey, DatabaseStackConfig> dbConfigs, Map<CloudPlatform, DatabaseServerParameterDecorator> parameterDecoratorMap,
            DatabaseObtainerService databaseObtainerService, ExternalDatabaseConfig externalDatabaseConfig,
            CmTemplateProcessorFactory cmTemplateProcessorFactory, ExternalDatabasePollingConfig config) {
        this.redbeamsClient = redbeamsClient;
        this.clusterRepository = clusterRepository;
        this.dbConfigs = dbConfigs;
        this.parameterDecoratorMap = parameterDecoratorMap;
        this.databaseObtainerService = databaseObtainerService;
        this.externalDatabaseConfig = externalDatabaseConfig;
        this.cmTemplateProcessorFactory = cmTemplateProcessorFactory;
        this.dbPollingConfig = config.getConfig();
    }

    public void provisionDatabase(Stack stack, DetailedEnvironmentResponse environment) {
        String databaseCrn;
        Cluster cluster = stack.getCluster();
        DatabaseAvailabilityType externalDatabase = stack.getExternalDatabaseCreationType();
        LOGGER.info("Create external {} database server in environment {} for DataHub {}", externalDatabase.name(), environment.getName(), cluster.getName());
        try {
            Optional<DatabaseServerV4Response> existingDatabase = findExistingDatabase(stack, environment.getCrn());
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

    public void upgradeDatabase(ClusterView cluster, UpgradeTargetMajorVersion targetMajorVersion, DatabaseServerV4StackRequest newSettings) {
        LOGGER.info("Upgrading external database server to version {} for DataHub {}",
                targetMajorVersion.name(), cluster.getName());
        String databaseCrn = cluster.getDatabaseServerCrn();

        if (externalDatabaseReferenceExist(databaseCrn)) {
            try {
                UpgradeDatabaseServerV4Request request = new UpgradeDatabaseServerV4Request();
                request.setUpgradedDatabaseSettings(newSettings);
                request.setUpgradeTargetMajorVersion(targetMajorVersion);
                UpgradeDatabaseServerV4Response response = redbeamsClient.upgradeByCrn(databaseCrn, request);
                if (null == response.getFlowIdentifier()) {
                    LOGGER.info(response.getReason());
                } else {
                    pollUntilFlowFinished(databaseCrn, response.getFlowIdentifier());
                }
            } catch (NotFoundException notFoundException) {
                LOGGER.info("Database server not found on redbeams side {}", databaseCrn);
            }
        } else {
            LOGGER.warn("[INVESTIGATE] The external database crn reference was not present");
        }
    }

    public void rotateDatabaseSecret(String databaseServerCrn, SecretType secretType, RotationFlowExecutionType executionType) {
        LOGGER.info("Rotating external database server secret: {} for database server {}", secretType, databaseServerCrn);
        RotateDatabaseServerSecretV4Request request = new RotateDatabaseServerSecretV4Request();
        request.setCrn(databaseServerCrn);
        request.setSecret(secretType.value());
        request.setExecutionType(executionType);
        FlowIdentifier flowIdentifier = redbeamsClient.rotateSecret(request);
        if (flowIdentifier == null) {
            handleUnsuccessfulFlow(databaseServerCrn, flowIdentifier, null);
        } else {
            pollUntilFlowFinished(databaseServerCrn, flowIdentifier);
        }
    }

    public void preValidateDatabaseSecretRotation(String databaseServerCrn) {
        if (StringUtils.isEmpty(databaseServerCrn)) {
            throw new SecretRotationException("No database server crn found, rotation is not possible.", null);
        }
        FlowLogResponse lastFlow = redbeamsClient.getLastFlowId(databaseServerCrn);
        if (lastFlow != null && lastFlow.getStateStatus() == StateStatus.PENDING) {
            String message = String.format("Polling in Redbeams is not possible since last known state of flow for the database is %s",
                    lastFlow.getCurrentState());
            throw new SecretRotationException(message, null);
        }
    }

    private void pollUntilFlowFinished(String databaseCrn, FlowIdentifier flowIdentifier) {
        try {
            Boolean success = Polling.waitPeriodly(dbPollingConfig.getSleepTime(), dbPollingConfig.getSleepTimeUnit())
                    .stopIfException(dbPollingConfig.getStopPollingIfExceptionOccured())
                    .stopAfterDelay(dbPollingConfig.getTimeout(), dbPollingConfig.getTimeoutTimeUnit())
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
            }
        } catch (UserBreakException e) {
            handleUnsuccessfulFlow(databaseCrn, flowIdentifier, e);
        }
    }

    private static void handleUnsuccessfulFlow(String databaseCrn, FlowIdentifier flowIdentifier, UserBreakException e) {
        String message = String.format("Database flow failed in Redbeams with error: '%s'. Database crn: %s, flow: %s",
                e != null ? e.getMessage() : "unknown", databaseCrn, flowIdentifier);
        LOGGER.warn(message);
        throw new CloudbreakServiceException(message, e);
    }

    private AttemptResult<Boolean> pollFlowState(FlowIdentifier flowIdentifier) {
        FlowCheckResponse flowState;
        if (flowIdentifier.getType() == FlowType.NOT_TRIGGERED) {
            return AttemptResults.breakFor(String.format("Flow %s not triggered", flowIdentifier.getPollableId()));
        } else if (flowIdentifier.getType() == FlowType.FLOW) {
            flowState = redbeamsClient.hasFlowRunningByFlowId(flowIdentifier.getPollableId());
        } else if (flowIdentifier.getType() == FlowType.FLOW_CHAIN) {
            flowState = redbeamsClient.hasFlowChainRunningByFlowChainId(flowIdentifier.getPollableId());
        } else {
            String message = String.format("Unknown flow identifier type %s for flow: %s", flowIdentifier.getType(), flowIdentifier);
            LOGGER.error(message);
            throw new CloudbreakServiceException(message);
        }

        LOGGER.debug("Database polling has active flow: {}, with latest fail: {}",
                flowState.getHasActiveFlow(), flowState.getLatestFlowFinalizedAndFailed());
        return flowState.getHasActiveFlow()
                ? AttemptResults.justContinue()
                : AttemptResults.finishWith(!flowState.getLatestFlowFinalizedAndFailed());
    }

    private Optional<DatabaseServerV4Response> findExistingDatabase(Stack stack, String environmentCrn) {
        String stackCrn = stack.getResourceCrn();
        LOGGER.debug("Trying to find existing database server for environment {} and cluster {}", environmentCrn, stackCrn);
        try {
            return Optional.ofNullable(redbeamsClient.getByClusterCrn(environmentCrn, stackCrn));
        } catch (NotFoundException ignore) {
            LOGGER.debug("External database in environment {} for cluster {} does not exist.", environmentCrn, stack.getCluster());
            return Optional.empty();
        }
    }

    private boolean externalDatabaseReferenceExist(String databaseCrn) {
        return !Strings.isNullOrEmpty(databaseCrn);
    }

    private void updateClusterWithDatabaseServerCrn(Cluster cluster, String databaseServerCrn) {
        cluster.setDatabaseServerCrn(databaseServerCrn);
        clusterRepository.save(cluster);
    }

    private AllocateDatabaseServerV4Request getDatabaseRequest(Stack stack, DetailedEnvironmentResponse environment) {
        AllocateDatabaseServerV4Request req = new AllocateDatabaseServerV4Request();
        req.setEnvironmentCrn(environment.getCrn());
        CloudPlatform cloudPlatform = CloudPlatform.valueOf(environment.getCloudPlatform().toUpperCase(Locale.US));
        String databaseEngineVersion = stack.getExternalDatabaseEngineVersion();
        req.setDatabaseServer(getDatabaseServerStackRequest(cloudPlatform, stack.getExternalDatabaseCreationType(), databaseEngineVersion,
                getAttributes(stack.getDatabase())));
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
        if (externalDatabaseConfig.isExternalDatabaseSslEnforcementSupportedFor(cloudPlatform) && isSslEnforcementSupportedForRuntime(runtime)) {
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
        Optional<String> blueprintTextOpt = Optional.ofNullable(cluster.getBlueprint()).map(Blueprint::getBlueprintText);
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
            String databaseEngineVersion, Map<String, Object> attributes) {
        DatabaseStackConfig databaseStackConfig = dbConfigs.get(new DatabaseStackConfigKey(cloudPlatform,
                parameterDecoratorMap.get(cloudPlatform).getDatabaseType(attributes).orElse(null)));
        if (databaseStackConfig == null) {
            throw new BadRequestException("Database config for cloud platform " + cloudPlatform + " not found");
        }
        DatabaseServerV4StackRequest request = new DatabaseServerV4StackRequest();
        request.setInstanceType(databaseStackConfig.getInstanceType());
        request.setDatabaseVendor(databaseStackConfig.getVendor());
        request.setStorageSize(databaseStackConfig.getVolumeSize());
        DatabaseServerParameter serverParameter = DatabaseServerParameter.builder()
                .withAvailabilityType(externalDatabase)
                .withEngineVersion(databaseEngineVersion)
                .withAttributes(attributes)
                .build();
        parameterDecoratorMap.get(cloudPlatform).setParameters(request, serverParameter);
        if (Objects.isNull(request.getCloudPlatform())) {
            request.setCloudPlatform(cloudPlatform);
        }
        return request;
    }

    private void waitAndGetDatabase(ClusterView cluster, String databaseCrn,
            DatabaseOperation databaseOperation, boolean cancellable) {
        waitAndGetDatabase(cluster, databaseCrn, dbPollingConfig, databaseOperation, cancellable);
    }

    private void waitAndGetDatabase(ClusterView cluster, String databaseCrn, PollingConfig pollingConfig, DatabaseOperation databaseOperation,
            boolean cancellable) {
        Polling.waitPeriodly(pollingConfig.getSleepTime(), pollingConfig.getSleepTimeUnit())
                .stopIfException(pollingConfig.getStopPollingIfExceptionOccured())
                .stopAfterDelay(pollingConfig.getTimeout(), pollingConfig.getTimeoutTimeUnit())
                .run(() -> databaseObtainerService.obtainAttemptResult(cluster, databaseOperation, databaseCrn, cancellable));
    }

    public DatabaseServerV4StackRequest migrateDatabaseSettingsIfNeeded(StackDto stack, TargetMajorVersion majorVersion) {

        CloudPlatform cloudPlatform = CloudPlatform.valueOf(stack.getCloudPlatform());
        Versioned currentVersion = stack::getExternalDatabaseEngineVersion;
        Versioned targetVersion = majorVersion::getMajorVersion;
        boolean upgradeTargetVersionImpliesFlexibleServerMigration = CMRepositoryVersionUtil.isVersionNewerOrEqualThanLimited(targetVersion,
                TargetMajorVersion.VERSION_14::getMajorVersion);
        boolean currentVersionImpliesFlexibleServerMigration = CMRepositoryVersionUtil.isVersionOlderThanLimited(currentVersion,
                TargetMajorVersion.VERSION_14::getMajorVersion);

        if (cloudPlatform == CloudPlatform.AZURE
                && upgradeTargetVersionImpliesFlexibleServerMigration
                && currentVersionImpliesFlexibleServerMigration) {

            Map<String, Object> attributes = getAttributes(stack.getDatabase());
            attributes.put(AzureDatabaseType.AZURE_DATABASE_TYPE_KEY, AzureDatabaseType.FLEXIBLE_SERVER.name());
            DatabaseServerV4StackRequest modifiedRequest = getDatabaseServerStackRequest(
                    cloudPlatform,
                    stack.getExternalDatabaseCreationType(),
                    majorVersion.getMajorVersion(),
                    attributes);
            LOGGER.debug("Migration resulted in request: {}", modifiedRequest);
            return modifiedRequest;
        }
        LOGGER.debug("Migration was not needed, original version: {}, target version: {}, cloud platform: {}",
                currentVersion,
                targetVersion,
                cloudPlatform);
        return null;
    }
}
