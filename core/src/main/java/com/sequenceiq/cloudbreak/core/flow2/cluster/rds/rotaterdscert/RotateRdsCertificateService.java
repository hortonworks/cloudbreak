package com.sequenceiq.cloudbreak.core.flow2.cluster.rds.rotaterdscert;

import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.DetailedStackStatus.ROTATE_RDS_CERTIFICATE_FAILED;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.StackType.DATALAKE;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.AVAILABLE;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.UPDATE_FAILED;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.UPDATE_IN_PROGRESS;
import static com.sequenceiq.cloudbreak.common.mappable.CloudPlatform.AWS;
import static com.sequenceiq.cloudbreak.sdx.TargetPlatform.PAAS;

import java.util.Collections;
import java.util.Set;

import jakarta.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.DetailedStackStatus;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.StackType;
import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.core.bootstrap.service.ClusterDeletionBasedExitCriteriaModel;
import com.sequenceiq.cloudbreak.core.bootstrap.service.container.postgres.PostgresConfigService;
import com.sequenceiq.cloudbreak.core.cluster.ClusterManagerDefaultConfigAdjuster;
import com.sequenceiq.cloudbreak.core.flow2.externaldatabase.ExternalDatabaseService;
import com.sequenceiq.cloudbreak.core.flow2.stack.CloudbreakFlowMessageService;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.event.ResourceEvent;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorException;
import com.sequenceiq.cloudbreak.orchestrator.host.HostOrchestrator;
import com.sequenceiq.cloudbreak.orchestrator.host.OrchestratorStateParams;
import com.sequenceiq.cloudbreak.orchestrator.model.GatewayConfig;
import com.sequenceiq.cloudbreak.orchestrator.state.ExitCriteriaModel;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.rotaterdscert.RotateRdsCertificateFailedEvent;
import com.sequenceiq.cloudbreak.sdx.TargetPlatform;
import com.sequenceiq.cloudbreak.service.GatewayConfigService;
import com.sequenceiq.cloudbreak.service.StackUpdater;
import com.sequenceiq.cloudbreak.service.cluster.ClusterApiConnectors;
import com.sequenceiq.cloudbreak.service.cluster.ClusterService;
import com.sequenceiq.cloudbreak.service.cluster.DatabaseSslService;
import com.sequenceiq.cloudbreak.service.salt.SaltStateParamsService;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.cloudbreak.view.ClusterView;
import com.sequenceiq.cloudbreak.view.InstanceMetadataView;
import com.sequenceiq.cloudbreak.view.StackView;

@Service
public class RotateRdsCertificateService {
    private static final Logger LOGGER = LoggerFactory.getLogger(RotateRdsCertificateService.class);

    private static final int MAX_RETRY_ON_ERROR = 3;

    private static final int MAX_RETRY = 100;

    private static final String POSTGRESQL_ROOT_CERTS_STATE = "postgresql/root-certs";

    @Inject
    private StackUpdater stackUpdater;

    @Inject
    private CloudbreakFlowMessageService flowMessageService;

    @Inject
    private StackDtoService stackDtoService;

    @Inject
    private ClusterService clusterService;

    @Inject
    private StackService stackService;

    @Inject
    private ClusterApiConnectors clusterApiConnectors;

    @Inject
    private ExternalDatabaseService externalDatabaseService;

    @Inject
    private GatewayConfigService gatewayConfigService;

    @Inject
    private HostOrchestrator hostOrchestrator;

    @Inject
    private DatabaseSslService databaseSslService;

    @Inject
    private PostgresConfigService postgresConfigService;

    @Inject
    private ClusterManagerDefaultConfigAdjuster clusterManagerDefaultConfigAdjuster;

    @Inject
    private SaltStateParamsService saltStateParamsService;

    void checkPrerequisitesState(Long stackId) {
        String statusReason = "Checking cluster prerequisites for RDS certificate rotation";
        LOGGER.debug(statusReason);
        stackUpdater.updateStackStatus(stackId, DetailedStackStatus.ROTATE_RDS_CERTIFICATE_IN_PROGRESS, statusReason);
        flowMessageService.fireEventAndLog(stackId, UPDATE_IN_PROGRESS.name(), ResourceEvent.ROTATE_RDS_CERTIFICATE_CHECK_PREREQUISITES);
    }

    void getLatestRdsCertificateState(Long stackId) {
        String statusReason = "Obtaining the latest RDS certificate";
        LOGGER.debug(statusReason);
        stackUpdater.updateStackStatus(stackId, DetailedStackStatus.ROTATE_RDS_CERTIFICATE_IN_PROGRESS, statusReason);
        flowMessageService.fireEventAndLog(stackId, UPDATE_IN_PROGRESS.name(), ResourceEvent.ROTATE_RDS_CERTIFICATE_GET_LATEST);
    }

    void updateLatestRdsCertificateState(Long stackId) {
        String statusReason = "Pushing latest RDS certificate to the cluster";
        LOGGER.debug(statusReason);
        stackUpdater.updateStackStatus(stackId, DetailedStackStatus.ROTATE_RDS_CERTIFICATE_IN_PROGRESS, statusReason);
        flowMessageService.fireEventAndLog(stackId, UPDATE_IN_PROGRESS.name(), ResourceEvent.ROTATE_RDS_CERTIFICATE_PUSH_LATEST);
    }

    void restartCmState(Long stackId) {
        String statusReason = "Restarting Cluster Manager service";
        LOGGER.debug(statusReason);
        stackUpdater.updateStackStatus(stackId, DetailedStackStatus.ROTATE_RDS_CERTIFICATE_IN_PROGRESS, statusReason);
        flowMessageService.fireEventAndLog(stackId, UPDATE_IN_PROGRESS.name(), ResourceEvent.ROTATE_RDS_CERTIFICATE_CM_RESTART);
    }

    void rollingRestartRdsCertificateState(Long stackId) {
        String statusReason = "Restarting cluster services";
        LOGGER.debug(statusReason);
        stackUpdater.updateStackStatus(stackId, DetailedStackStatus.ROTATE_RDS_CERTIFICATE_IN_PROGRESS, statusReason);
        flowMessageService.fireEventAndLog(stackId, UPDATE_IN_PROGRESS.name(), ResourceEvent.ROTATE_RDS_CERTIFICATE_ROLLING_SERVICE_RESTART);
    }

    void rotateOnProviderState(Long stackId) {
        String statusReason = "Rotating RDS certificate";
        LOGGER.debug(statusReason);
        stackUpdater.updateStackStatus(stackId, DetailedStackStatus.ROTATE_RDS_CERTIFICATE_IN_PROGRESS, statusReason);
        flowMessageService.fireEventAndLog(stackId, UPDATE_IN_PROGRESS.name(), ResourceEvent.ROTATE_RDS_CERTIFICATE_ON_PROVIDER);
    }

    void rotateRdsCertFinished(Long stackId) {
        String statusReason = "RDS certificate rotation finished";
        LOGGER.debug(statusReason);
        stackUpdater.updateStackStatus(stackId, DetailedStackStatus.AVAILABLE, statusReason);
        flowMessageService.fireEventAndLog(stackId, AVAILABLE.name(), ResourceEvent.ROTATE_RDS_CERTIFICATE_FINISHED);
    }

    void rotateRdsCertFailed(RotateRdsCertificateFailedEvent failedEvent) {
        String statusReason = "RDS certificate rotation failed: " + failedEvent.getException().getMessage();
        LOGGER.debug(statusReason);
        stackUpdater.updateStackStatus(failedEvent.getResourceId(), ROTATE_RDS_CERTIFICATE_FAILED, statusReason);
        flowMessageService.fireEventAndLog(failedEvent.getResourceId(), UPDATE_FAILED.name(), ResourceEvent.ROTATE_RDS_CERTIFICATE_FAILED);
    }

    public void checkPrerequisites(Long stackId) {
        StackView stack = stackDtoService.getStackViewById(stackId);
        if (AWS.name().equalsIgnoreCase(stack.getCloudPlatform())) {
            Cluster cluster = clusterService.getCluster(stack.getClusterId());
            if (cluster != null && cluster.getDbSslRootCertBundle() != null && cluster.getDbSslEnabled()) {
                LOGGER.info("{} with name {} is applicable for rotation", getType(stack), stack.getName());
            } else {
                throw new CloudbreakServiceException(String.format("%s Database not ssl enabled. " +
                        "Rotation of certificate does not supported", getType(stack)));
            }
        } else {
            throw new CloudbreakServiceException(String.format("%s is not deployed on AWS. " +
                    "Rotation of certificate does not supported", getType(stack)));
        }
    }

    public void getLatestRdsCertificate(Long stackId) {
        StackDto stackDto = stackDtoService.getById(stackId);
        String datalakeCrn = stackDto.getDatalakeCrn();
        if (StringUtils.isNoneEmpty(datalakeCrn) && isPaaSDataLake(datalakeCrn)) {
            StackDto datalakeDto = stackDtoService.getByCrn(datalakeCrn);
            if (datalakeDto.getCluster().hasExternalDatabase()) {
                LOGGER.info("Update Datalake's RDS SSL certificate to the latest: '{}/{}'", datalakeDto.getName(), datalakeCrn);
                Cluster cluster = clusterService.getCluster(datalakeDto.getCluster().getId());
                externalDatabaseService.updateToLatestSslCert(cluster);
            }
        }
        if (stackDto.getCluster().hasExternalDatabase()) {
            LOGGER.info("Update ('{}')'s external database SSL certificate to the latest", stackDto.getName());
            Cluster cluster = clusterService.getCluster(stackDto.getCluster().getId());
            externalDatabaseService.updateToLatestSslCert(cluster);
            databaseSslService.getDbSslDetailsForRotationAndUpdateInCluster(stackDto);
        } else {
            LOGGER.info("Update ('{}')'s embedded database SSL certificate to the latest", stackDto.getName());
            databaseSslService.setEmbeddedDbSslDetailsAndUpdateInClusterInternal(stackDto);
        }
    }

    private boolean isPaaSDataLake(String datalakeCrn) {
        return datalakeCrn != null && PAAS.equals(TargetPlatform.getByCrn(datalakeCrn));
    }

    public void updateLatestRdsCertificate(Long stackId) {
        LOGGER.info("Distribute database SSL certificates on stack with id: '{}'", stackId);
        StackDto stackDto = stackDtoService.getById(stackId);
        ClusterDeletionBasedExitCriteriaModel clusterDeletionBasedExitCriteriaModel =
                ClusterDeletionBasedExitCriteriaModel.clusterDeletionBasedModel(stackId, stackDto.getCluster().getId());
        try {
            OrchestratorStateParams stateParams = saltStateParamsService.createStateParamsForReachableNodes(stackDto, POSTGRESQL_ROOT_CERTS_STATE, MAX_RETRY,
                    MAX_RETRY_ON_ERROR);
            postgresConfigService.uploadServicePillarsForPostgres(stackDto, clusterDeletionBasedExitCriteriaModel, stateParams);
            hostOrchestrator.runOrchestratorState(stateParams);
            LOGGER.info("Database SSL certificates have been distributes on stack with id: '{}'", stackId);
        } catch (CloudbreakOrchestratorException e) {
            String message = String.format("Distribution of database SSL certificates failed on %s with name: '%s'", getType(stackDto), stackDto.getName());
            LOGGER.warn(message, e);
            throw new CloudbreakServiceException(message, e);
        }
    }

    public void restartCm(Long stackId) {
        StackDto stackDto = stackDtoService.getById(stackId);
        try {
            LOGGER.debug("Restarting CM server for stack: '{}'", stackId);
            restartCMServer(stackDto);
            LOGGER.info("CM server has been restarted for stack: '{}'", stackId);
        } catch (Exception e) {
            LOGGER.warn("Failed to restart CM server for stack: '{}'", stackId, e);
            throw new CloudbreakServiceException(String.format("Failed to restart Cloudera Manager on host for %s %s.",
                    stackDto.getName(), getType(stackDto.getStack())));
        }
    }

    public void rollingRestartServices(Long stackId) {
        LOGGER.debug("Triggering rolling restart of the services for stack: '{}'", stackId);
        Stack stack = stackService.getByIdWithLists(stackId);
        clusterApiConnectors.getConnector(stack).clusterModificationService().rollingRestartServices();
    }

    public void rotateOnProvider(Long stackId) {
        StackDto stackDto = stackDtoService.getById(stackId);
        if (stackDto.getCluster().hasExternalDatabase()) {
            Cluster cluster = clusterService.getCluster(stackDto.getCluster().getId());
            externalDatabaseService.rotateSSLCertificate(cluster);
        }
    }

    private String getType(StackView stackView) {
        return getType(stackView.getType());
    }

    private String getType(StackDto stackDto) {
        return getType(stackDto.getType());
    }

    private String getType(StackType type) {
        return DATALAKE.equals(type) ? "Data Lake" : "Data Hub";
    }

    private void restartCMServer(StackDto stackDto) throws Exception {
        ClusterView cluster = stackDto.getCluster();
        StackView stack = stackDto.getStack();
        InstanceMetadataView gatewayInstance = stackDto.getPrimaryGatewayInstance();
        GatewayConfig gatewayConfig = gatewayConfigService.getGatewayConfig(stack, stackDto.getSecurityConfig(), gatewayInstance, stackDto.hasGateway());
        Set<String> gatewayFQDN = Collections.singleton(gatewayInstance.getDiscoveryFQDN());
        ExitCriteriaModel exitModel = ClusterDeletionBasedExitCriteriaModel.clusterDeletionBasedModel(stack.getId(), cluster.getId());
        hostOrchestrator.restartClusterManagerOnMaster(gatewayConfig, gatewayFQDN, exitModel);
        clusterManagerDefaultConfigAdjuster.waitForClusterManagerToBecomeAvailable(stackDto, false);
    }
}
