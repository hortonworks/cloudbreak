package com.sequenceiq.cloudbreak.core.flow2.cluster.rds.cert.rotate;

import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.DetailedStackStatus.ROTATE_RDS_CERTIFICATE_FAILED;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.StackType.DATALAKE;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.AVAILABLE;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.UPDATE_FAILED;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.UPDATE_IN_PROGRESS;
import static com.sequenceiq.cloudbreak.sdx.TargetPlatform.PAAS;

import java.util.List;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.DetailedStackStatus;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.StackType;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.RotateRdsCertificateType;
import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.core.bootstrap.service.ClusterDeletionBasedExitCriteriaModel;
import com.sequenceiq.cloudbreak.core.bootstrap.service.container.postgres.PostgresConfigService;
import com.sequenceiq.cloudbreak.core.flow2.cluster.rds.cert.check.DatahubCertificateChecker;
import com.sequenceiq.cloudbreak.core.flow2.externaldatabase.ExternalDatabaseService;
import com.sequenceiq.cloudbreak.core.flow2.stack.CloudbreakFlowMessageService;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.event.ResourceEvent;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorException;
import com.sequenceiq.cloudbreak.orchestrator.host.HostOrchestrator;
import com.sequenceiq.cloudbreak.orchestrator.host.OrchestratorStateParams;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.rotaterdscert.RotateRdsCertificateFailedEvent;
import com.sequenceiq.cloudbreak.sdx.TargetPlatform;
import com.sequenceiq.cloudbreak.service.StackUpdater;
import com.sequenceiq.cloudbreak.service.cluster.ClusterApiConnectors;
import com.sequenceiq.cloudbreak.service.cluster.ClusterService;
import com.sequenceiq.cloudbreak.service.cluster.DatabaseSslService;
import com.sequenceiq.cloudbreak.service.salt.SaltStateParamsService;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;
import com.sequenceiq.cloudbreak.service.stack.StackService;
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
    private HostOrchestrator hostOrchestrator;

    @Inject
    private DatabaseSslService databaseSslService;

    @Inject
    private PostgresConfigService postgresConfigService;

    @Inject
    private SaltStateParamsService saltStateParamsService;

    @Inject
    private DatahubCertificateChecker datahubCertificateChecker;

    public void checkPrerequisitesState(Long stackId) {
        String statusReason = "Checking cluster prerequisites for RDS certificate rotation";
        LOGGER.debug(statusReason);
        stackUpdater.updateStackStatus(stackId, DetailedStackStatus.ROTATE_RDS_CERTIFICATE_IN_PROGRESS, statusReason);
        flowMessageService.fireEventAndLog(stackId, UPDATE_IN_PROGRESS.name(), ResourceEvent.ROTATE_RDS_CERTIFICATE_CHECK_PREREQUISITES);
    }

    public void getLatestRdsCertificateState(Long stackId) {
        String statusReason = "Obtaining the latest RDS certificate";
        LOGGER.debug(statusReason);
        stackUpdater.updateStackStatus(stackId, DetailedStackStatus.ROTATE_RDS_CERTIFICATE_IN_PROGRESS, statusReason);
        flowMessageService.fireEventAndLog(stackId, UPDATE_IN_PROGRESS.name(), ResourceEvent.ROTATE_RDS_CERTIFICATE_GET_LATEST);
    }

    public void updateLatestRdsCertificateState(Long stackId) {
        String statusReason = "Pushing latest RDS certificate to the cluster";
        LOGGER.debug(statusReason);
        stackUpdater.updateStackStatus(stackId, DetailedStackStatus.ROTATE_RDS_CERTIFICATE_IN_PROGRESS, statusReason);
        flowMessageService.fireEventAndLog(stackId, UPDATE_IN_PROGRESS.name(), ResourceEvent.ROTATE_RDS_CERTIFICATE_PUSH_LATEST);
    }

    public void restartCmState(Long stackId) {
        String statusReason = "Restarting Cluster Manager service";
        LOGGER.debug(statusReason);
        stackUpdater.updateStackStatus(stackId, DetailedStackStatus.ROTATE_RDS_CERTIFICATE_IN_PROGRESS, statusReason);
        flowMessageService.fireEventAndLog(stackId, UPDATE_IN_PROGRESS.name(), ResourceEvent.ROTATE_RDS_CERTIFICATE_CM_RESTART);
    }

    public void rollingRestartRdsCertificateState(Long stackId) {
        String statusReason = "Restarting cluster services";
        LOGGER.debug(statusReason);
        stackUpdater.updateStackStatus(stackId, DetailedStackStatus.ROTATE_RDS_CERTIFICATE_IN_PROGRESS, statusReason);
        flowMessageService.fireEventAndLog(stackId, UPDATE_IN_PROGRESS.name(), ResourceEvent.ROTATE_RDS_CERTIFICATE_ROLLING_SERVICE_RESTART);
    }

    public void rotateOnProviderState(Long stackId) {
        String statusReason = "Rotating RDS certificate";
        LOGGER.debug(statusReason);
        stackUpdater.updateStackStatus(stackId, DetailedStackStatus.ROTATE_RDS_CERTIFICATE_IN_PROGRESS, statusReason);
        flowMessageService.fireEventAndLog(stackId, UPDATE_IN_PROGRESS.name(), ResourceEvent.ROTATE_RDS_CERTIFICATE_ON_PROVIDER);
    }

    public void rotateRdsCertFinished(Long stackId) {
        String statusReason = "RDS certificate rotation finished";
        LOGGER.debug(statusReason);
        stackUpdater.updateStackStatus(stackId, DetailedStackStatus.AVAILABLE, statusReason);
        flowMessageService.fireEventAndLog(stackId, AVAILABLE.name(), ResourceEvent.ROTATE_RDS_CERTIFICATE_FINISHED);
    }

    public void rotateRdsCertFailed(RotateRdsCertificateFailedEvent failedEvent) {
        String statusReason = "RDS certificate rotation failed: " + failedEvent.getException().getMessage();
        LOGGER.debug(statusReason);
        stackUpdater.updateStackStatus(failedEvent.getResourceId(), ROTATE_RDS_CERTIFICATE_FAILED, statusReason);
        flowMessageService.fireEventAndLog(failedEvent.getResourceId(), UPDATE_FAILED.name(), ResourceEvent.ROTATE_RDS_CERTIFICATE_FAILED);
    }

    public void checkPrerequisites(Long stackId, RotateRdsCertificateType rotateRdsCertificateType) {
        StackView stack = stackDtoService.getStackViewById(stackId);
        List<String> datahubsWhichMustBeUpdated = datahubCertificateChecker.collectDatahubsWhichMustBeUpdated(stack);
        if (!datahubsWhichMustBeUpdated.isEmpty()) {
            String errorMessage = String.format("Data Hub with name: '%s' is not on the latest certificate version. " +
                            "Please update certificate on the Data Hub side before update the Data Lake",
                    String.join(", ", datahubsWhichMustBeUpdated.stream().sorted().collect(Collectors.toList())));
            LOGGER.info(errorMessage);
            throw new CloudbreakServiceException(errorMessage);
        }
        Cluster cluster = clusterService.getCluster(stack.getClusterId());
        if (rotateRdsCertificateType.equals(RotateRdsCertificateType.ROTATE)) {
            if (cluster != null && cluster.getDbSslRootCertBundle() != null && cluster.getDbSslEnabled()) {
                LOGGER.info("{} with name {} is applicable for rotation", getType(stack), stack.getName());
            } else {
                throw new CloudbreakServiceException(String.format("%s Database not ssl enabled. " +
                        "Rotation of certificate does not supported", getType(stack)));
            }
        }
    }

    public void getLatestRdsCertificate(Long stackId) {
        StackDto stackDto = stackDtoService.getById(stackId);
        String datalakeCrn = stackDto.getDatalakeCrn();

        if (datahubHasAttachedPaasDatalake(datalakeCrn)) {
            StackDto datalakeDto = stackDtoService.getByCrn(datalakeCrn);
            if (clusterHasExternalDatabase(datalakeDto)) {
                LOGGER.info("Update Datalake's RDS SSL certificate to the latest: '{}/{}'", datalakeDto.getName(), datalakeCrn);
                Cluster cluster = clusterService.getCluster(datalakeDto.getCluster().getId());
                externalDatabaseService.updateToLatestSslCert(cluster);
            }
        }
        if (clusterHasExternalDatabase(stackDto)) {
            LOGGER.info("Update ('{}')'s external database SSL certificate to the latest", stackDto.getName());
            Cluster cluster = clusterService.getCluster(stackDto.getCluster().getId());
            externalDatabaseService.updateToLatestSslCert(cluster);
            databaseSslService.getDbSslDetailsForRotationAndUpdateInCluster(stackDto);
        } else {
            LOGGER.info("Update ('{}')'s embedded database SSL certificate to the latest", stackDto.getName());
            databaseSslService.setEmbeddedDbSslDetailsAndUpdateInClusterInternal(stackDto);
        }
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

    public void rollingRestartServices(Long stackId) {
        LOGGER.debug("Triggering rolling restart of the services for stack: '{}'", stackId);
        Stack stack = stackService.getByIdWithLists(stackId);
        clusterApiConnectors.getConnector(stack).clusterModificationService().rollingRestartServices(false);
    }

    public void rotateOnProvider(Long stackId) {
        StackDto stackDto = stackDtoService.getById(stackId);
        if (clusterHasExternalDatabase(stackDto)) {
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

    private boolean clusterHasExternalDatabase(StackDto stackDto) {
        return stackDto.getCluster().hasExternalDatabase();
    }

    private boolean datahubHasAttachedPaasDatalake(String datalakeCrn) {
        return StringUtils.isNoneEmpty(datalakeCrn) && isPaaSDataLake(datalakeCrn);
    }

    private boolean isPaaSDataLake(String datalakeCrn) {
        return datalakeCrn != null && PAAS.equals(TargetPlatform.getByCrn(datalakeCrn));
    }
}
