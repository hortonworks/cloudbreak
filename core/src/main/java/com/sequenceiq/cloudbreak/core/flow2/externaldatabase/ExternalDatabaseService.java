package com.sequenceiq.cloudbreak.core.flow2.externaldatabase;

import java.io.IOException;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

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
import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.auth.crn.Crn;
import com.sequenceiq.cloudbreak.cloud.model.StackTags;
import com.sequenceiq.cloudbreak.cmtemplate.CMRepositoryVersionUtil;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessor;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessorFactory;
import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.conf.ExternalDatabaseConfig;
import com.sequenceiq.cloudbreak.domain.Blueprint;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.repository.cluster.ClusterRepository;
import com.sequenceiq.cloudbreak.rotation.secret.RotationFlowExecutionType;
import com.sequenceiq.cloudbreak.service.externaldatabase.DatabaseOperation;
import com.sequenceiq.cloudbreak.service.externaldatabase.DatabaseServerParameterDecorator;
import com.sequenceiq.cloudbreak.service.externaldatabase.PollingConfig;
import com.sequenceiq.cloudbreak.service.externaldatabase.model.DatabaseServerParameter;
import com.sequenceiq.cloudbreak.service.externaldatabase.model.DatabaseStackConfig;
import com.sequenceiq.cloudbreak.service.rdsconfig.RedbeamsClientService;
import com.sequenceiq.cloudbreak.view.ClusterView;
import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;
import com.sequenceiq.flow.api.model.FlowCheckResponse;
import com.sequenceiq.flow.api.model.FlowIdentifier;
import com.sequenceiq.flow.api.model.FlowType;
import com.sequenceiq.redbeams.api.endpoint.v4.databaseserver.requests.AllocateDatabaseServerV4Request;
import com.sequenceiq.redbeams.api.endpoint.v4.databaseserver.requests.RotateDatabaseServerSecretV4Request;
import com.sequenceiq.redbeams.api.endpoint.v4.databaseserver.requests.SslConfigV4Request;
import com.sequenceiq.redbeams.api.endpoint.v4.databaseserver.requests.SslMode;
import com.sequenceiq.redbeams.api.endpoint.v4.databaseserver.requests.UpgradeDatabaseServerV4Request;
import com.sequenceiq.redbeams.api.endpoint.v4.databaseserver.requests.UpgradeTargetMajorVersion;
import com.sequenceiq.redbeams.api.endpoint.v4.databaseserver.responses.DatabaseServerV4Response;
import com.sequenceiq.redbeams.api.endpoint.v4.databaseserver.responses.UpgradeDatabaseServerV4Response;
import com.sequenceiq.redbeams.api.endpoint.v4.stacks.DatabaseServerV4StackRequest;
import com.sequenceiq.redbeams.rotation.RedbeamsSecretType;

@Service
public class ExternalDatabaseService {

    public static final int SLEEP_TIME_IN_SEC_FOR_DB_POLLING = 10;

    public static final int DURATION_IN_MINUTES_FOR_DB_POLLING = 60;

    private static final Logger LOGGER = LoggerFactory.getLogger(ExternalDatabaseService.class);

    private static final String SSL_ENFORCEMENT_MIN_RUNTIME_VERSION = "7.2.2";

    private static final PollingConfig DB_POLLING_CONFIG = PollingConfig.builder()
            .withSleepTime(SLEEP_TIME_IN_SEC_FOR_DB_POLLING)
            .withSleepTimeUnit(TimeUnit.SECONDS)
            .withTimeout(DURATION_IN_MINUTES_FOR_DB_POLLING)
            .withTimeoutTimeUnit(TimeUnit.MINUTES)
            .withStopPollingIfExceptionOccured(false)
            .build();

    private final RedbeamsClientService redbeamsClient;

    private final ClusterRepository clusterRepository;

    private final Map<CloudPlatform, DatabaseStackConfig> dbConfigs;

    private final Map<CloudPlatform, DatabaseServerParameterDecorator> parameterDecoratorMap;

    private final DatabaseObtainerService databaseObtainerService;

    private final EntitlementService entitlementService;

    private final ExternalDatabaseConfig externalDatabaseConfig;

    private final CmTemplateProcessorFactory cmTemplateProcessorFactory;

    public ExternalDatabaseService(RedbeamsClientService redbeamsClient, ClusterRepository clusterRepository,
            Map<CloudPlatform, DatabaseStackConfig> dbConfigs, Map<CloudPlatform, DatabaseServerParameterDecorator> parameterDecoratorMap,
            DatabaseObtainerService databaseObtainerService, EntitlementService entitlementService, ExternalDatabaseConfig externalDatabaseConfig,
            CmTemplateProcessorFactory cmTemplateProcessorFactory) {
        this.redbeamsClient = redbeamsClient;
        this.clusterRepository = clusterRepository;
        this.dbConfigs = dbConfigs;
        this.parameterDecoratorMap = parameterDecoratorMap;
        this.databaseObtainerService = databaseObtainerService;
        this.entitlementService = entitlementService;
        this.externalDatabaseConfig = externalDatabaseConfig;
        this.cmTemplateProcessorFactory = cmTemplateProcessorFactory;
    }

    public void provisionDatabase(Cluster cluster, DatabaseAvailabilityType externalDatabase, DetailedEnvironmentResponse environment) {
        LOGGER.info("Create external {} database server in environment {} for DataHub {}", externalDatabase.name(), environment.getName(), cluster.getName());
        String databaseCrn;
        try {
            Optional<DatabaseServerV4Response> existingDatabase = findExistingDatabase(cluster, environment.getCrn());
            if (existingDatabase.isPresent()) {
                databaseCrn = existingDatabase.get().getCrn();
                LOGGER.debug("Found existing database with CRN {}", databaseCrn);
            } else {
                LOGGER.debug("Requesting new database server creation");
                AllocateDatabaseServerV4Request request = getDatabaseRequest(environment, externalDatabase, cluster);
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

    public void upgradeDatabase(ClusterView cluster, UpgradeTargetMajorVersion targetMajorVersion) {
        LOGGER.info("Upgrading external database server to version {} for DataHub {}",
                targetMajorVersion.name(), cluster.getName());
        String databaseCrn = cluster.getDatabaseServerCrn();

            if (externalDatabaseReferenceExist(databaseCrn)) {
                try {
                    UpgradeDatabaseServerV4Request request = new UpgradeDatabaseServerV4Request();
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

    public void rotateDatabaseSecret(String databaseServerCrn, RedbeamsSecretType secretType, RotationFlowExecutionType executionType) {
        LOGGER.info("Rotating external database server secret: {} for database server {}", secretType, databaseServerCrn);
        RotateDatabaseServerSecretV4Request request = new RotateDatabaseServerSecretV4Request();
        request.setCrn(databaseServerCrn);
        request.setSecret(secretType.name());
        request.setExecutionType(executionType);
        FlowIdentifier flowIdentifier = redbeamsClient.rotateSecret(request);
        if (flowIdentifier == null) {
            handleUnsuccessfulFlow(databaseServerCrn, flowIdentifier, null);
        } else {
            pollUntilFlowFinished(databaseServerCrn, flowIdentifier);
        }
    }

    private void pollUntilFlowFinished(String databaseCrn, FlowIdentifier flowIdentifier) {
        try {
            Boolean success = Polling.waitPeriodly(DB_POLLING_CONFIG.getSleepTime(), DB_POLLING_CONFIG.getSleepTimeUnit())
                    .stopIfException(DB_POLLING_CONFIG.getStopPollingIfExceptionOccured())
                    .stopAfterDelay(DB_POLLING_CONFIG.getTimeout(), DB_POLLING_CONFIG.getTimeoutTimeUnit())
                    .run(() -> pollFlowState(flowIdentifier));
            if (success == null || !success) {
                handleUnsuccessfulFlow(databaseCrn, flowIdentifier, null);
            }
        } catch (UserBreakException e) {
            handleUnsuccessfulFlow(databaseCrn, flowIdentifier, e);
        }
    }

    private static void handleUnsuccessfulFlow(String databaseCrn, FlowIdentifier flowIdentifier, UserBreakException e) {
        String message = String.format("Database flow failed in Redbeams. Database crn: %s, flow: %s",
                databaseCrn, flowIdentifier);
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

    private Optional<DatabaseServerV4Response> findExistingDatabase(Cluster cluster, String environmentCrn) {
        if (cluster.getStack() != null) {
            String clusterCrn = cluster.getStack().getResourceCrn();
            LOGGER.debug("Trying to find existing database server for environment {} and cluster {}", environmentCrn, clusterCrn);
            try {
                return Optional.ofNullable(redbeamsClient.getByClusterCrn(environmentCrn, clusterCrn));
            } catch (NotFoundException ignore) {
                LOGGER.debug("External database in environment {} for cluster {} does not exist.", environmentCrn, cluster);
                return Optional.empty();
            }
        }
        LOGGER.warn("[INVESTIGATE] Stack is empty for cluster '{}' in environment {}", cluster.getName(), environmentCrn);
        return Optional.empty();
    }

    private boolean externalDatabaseReferenceExist(String databaseCrn) {
        return !Strings.isNullOrEmpty(databaseCrn);
    }

    private void updateClusterWithDatabaseServerCrn(Cluster cluster, String databaseServerCrn) {
        cluster.setDatabaseServerCrn(databaseServerCrn);
        clusterRepository.save(cluster);
    }

    private AllocateDatabaseServerV4Request getDatabaseRequest(DetailedEnvironmentResponse environment, DatabaseAvailabilityType externalDatabase,
            Cluster cluster) {
        AllocateDatabaseServerV4Request req = new AllocateDatabaseServerV4Request();
        req.setEnvironmentCrn(environment.getCrn());
        CloudPlatform cloudPlatform = CloudPlatform.valueOf(environment.getCloudPlatform().toUpperCase(Locale.US));
        Stack stack = cluster.getStack();
        String databaseEngineVersion = Optional.ofNullable(stack).map(Stack::getExternalDatabaseEngineVersion).orElse(null);
        req.setDatabaseServer(getDatabaseServerStackRequest(cloudPlatform, externalDatabase, databaseEngineVersion));
        if (stack != null) {
            req.setClusterCrn(stack.getResourceCrn());
            req.setTags(getUserDefinedTags(stack));
        }
        configureSslEnforcement(req, cloudPlatform, cluster);
        return req;
    }

    private void configureSslEnforcement(AllocateDatabaseServerV4Request req, CloudPlatform cloudPlatform, Cluster cluster) {
        String runtime = getRuntime(cluster);
        if (externalDatabaseConfig.isExternalDatabaseSslEnforcementSupportedFor(cloudPlatform) && isSslEnforcementSupportedForRuntime(runtime)
                && entitlementService.databaseWireEncryptionDatahubEnabled(Crn.safeFromString(cluster.getEnvironmentCrn()).getAccountId())) {
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
            String databaseEngineVersion) {
        DatabaseStackConfig databaseStackConfig = dbConfigs.get(cloudPlatform);
        if (databaseStackConfig == null) {
            throw new BadRequestException("Database config for cloud platform " + cloudPlatform + " not found");
        }
        DatabaseServerV4StackRequest request = new DatabaseServerV4StackRequest();
        request.setInstanceType(databaseStackConfig.getInstanceType());
        request.setDatabaseVendor(databaseStackConfig.getVendor());
        request.setStorageSize(databaseStackConfig.getVolumeSize());
        DatabaseServerParameter serverParameter = DatabaseServerParameter.builder()
                .withHighlyAvailable(DatabaseAvailabilityType.HA == externalDatabase)
                .withEngineVersion(databaseEngineVersion)
                .build();
        parameterDecoratorMap.get(cloudPlatform).setParameters(request, serverParameter);
        return request;
    }

    private void waitAndGetDatabase(ClusterView cluster, String databaseCrn,
            DatabaseOperation databaseOperation, boolean cancellable) {
        waitAndGetDatabase(cluster, databaseCrn, DB_POLLING_CONFIG, databaseOperation, cancellable);
    }

    private void waitAndGetDatabase(ClusterView cluster, String databaseCrn, PollingConfig pollingConfig, DatabaseOperation databaseOperation,
            boolean cancellable) {
        Polling.waitPeriodly(pollingConfig.getSleepTime(), pollingConfig.getSleepTimeUnit())
                .stopIfException(pollingConfig.getStopPollingIfExceptionOccured())
                .stopAfterDelay(pollingConfig.getTimeout(), pollingConfig.getTimeoutTimeUnit())
                .run(() -> databaseObtainerService.obtainAttemptResult(cluster, databaseOperation, databaseCrn, cancellable));
    }

}
