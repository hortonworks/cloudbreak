package com.sequenceiq.cloudbreak.core.flow2.cluster.rds.cert.migrate;

import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.StackType.DATALAKE;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.UPDATE_IN_PROGRESS;
import static com.sequenceiq.cloudbreak.cluster.model.CMConfigUpdateStrategy.NO_FALLBACK;

import java.util.Set;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.google.common.collect.Table;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.DetailedStackStatus;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.StackType;
import com.sequenceiq.cloudbreak.api.endpoint.v4.database.base.DatabaseType;
import com.sequenceiq.cloudbreak.cmtemplate.CentralCmTemplateUpdater;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateComponentConfigProviderProcessor;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessor;
import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.converter.StackToTemplatePreparationObjectConverter;
import com.sequenceiq.cloudbreak.core.bootstrap.service.ClusterDeletionBasedExitCriteriaModel;
import com.sequenceiq.cloudbreak.core.bootstrap.service.container.postgres.PostgresConfigService;
import com.sequenceiq.cloudbreak.core.flow2.cluster.rds.upgrade.RdsSettingsMigrationService;
import com.sequenceiq.cloudbreak.core.flow2.externaldatabase.ExternalDatabaseService;
import com.sequenceiq.cloudbreak.core.flow2.stack.CloudbreakFlowMessageService;
import com.sequenceiq.cloudbreak.domain.RDSConfig;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.event.ResourceEvent;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorException;
import com.sequenceiq.cloudbreak.orchestrator.host.HostOrchestrator;
import com.sequenceiq.cloudbreak.orchestrator.host.OrchestratorStateParams;
import com.sequenceiq.cloudbreak.service.StackUpdater;
import com.sequenceiq.cloudbreak.service.cluster.ClusterService;
import com.sequenceiq.cloudbreak.service.cluster.DatabaseSslService;
import com.sequenceiq.cloudbreak.service.rdsconfig.RdsConfigService;
import com.sequenceiq.cloudbreak.service.salt.SaltStateParamsService;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;
import com.sequenceiq.cloudbreak.template.TemplatePreparationObject;
import com.sequenceiq.cloudbreak.view.StackView;

@Service
public class MigrateRdsCertificateService {
    private static final Logger LOGGER = LoggerFactory.getLogger(MigrateRdsCertificateService.class);

    private static final int MAX_RETRY_ON_ERROR = 3;

    private static final int MAX_RETRY = 100;

    @Inject
    private StackUpdater stackUpdater;

    @Inject
    private CloudbreakFlowMessageService flowMessageService;

    @Inject
    private StackDtoService stackDtoService;

    @Inject
    private ClusterService clusterService;

    @Inject
    private ExternalDatabaseService externalDatabaseService;

    @Inject
    private DatabaseSslService databaseSslService;

    @Inject
    private CmTemplateComponentConfigProviderProcessor cmTemplateComponentConfigProviderProcessor;

    @Inject
    private CentralCmTemplateUpdater centralCmTemplateUpdater;

    @Inject
    private StackToTemplatePreparationObjectConverter stackToTemplatePreparationObjectConverter;

    @Inject
    private RdsSettingsMigrationService rdsSettingsMigrationService;

    @Inject
    private RdsConfigService rdsConfigService;

    @Inject
    private SaltStateParamsService saltStateParamsService;

    @Inject
    private PostgresConfigService postgresConfigService;

    @Inject
    private HostOrchestrator hostOrchestrator;

    public void setupNonTlsToTlsIfRequired(Long stackId) {
        String statusReason = "Setup TLS on cluster if required";
        LOGGER.debug(statusReason);
        stackUpdater.updateStackStatus(stackId, DetailedStackStatus.ROTATE_RDS_CERTIFICATE_IN_PROGRESS, statusReason);
        flowMessageService.fireEventAndLog(stackId, UPDATE_IN_PROGRESS.name(), ResourceEvent.ROTATE_RDS_CERTIFICATE_TLS_SETUP);
    }

    public void updateNonTlsToTlsIfRequired(Long stackId) {
        StackDto stackDto = stackDtoService.getById(stackId);
        try {
            LOGGER.debug("Triggering reconfigure services for stack: '{}'", stackId);
            TemplatePreparationObject templatePreparationObject = stackToTemplatePreparationObjectConverter.convert(stackDto);
            CmTemplateProcessor cmTemplateProcessor = centralCmTemplateUpdater.getCmTemplateProcessor(templatePreparationObject);
            Table<String, String, String> configuration =
                    cmTemplateComponentConfigProviderProcessor.collectDataConfigurations(cmTemplateProcessor, templatePreparationObject);
            rdsSettingsMigrationService.updateCMServiceConfigs(stackDto, configuration, NO_FALLBACK, false);
        } catch (Exception e) {
            LOGGER.warn("Update non tls to tls: '{}'", stackId, e);
            throw new CloudbreakServiceException(String.format("Failed to migrate non tls to tls %s %s.",
                    stackDto.getName(), getType(stackDto.getStack())));
        }
    }

    public void turnOnSslOnProvider(Long stackId) {
        StackDto stackDto = stackDtoService.getById(stackId);
        if (clusterHasExternalDatabase(stackDto)) {
            Cluster cluster = clusterService.getCluster(stackDto.getCluster().getId());
            externalDatabaseService.turnOnSslOnProvider(cluster);
        }
    }

    public void migrateRdsToTls(Long stackId) {
        StackDto stackDto = stackDtoService.getById(stackId);
        if (clusterHasExternalDatabase(stackDto)) {
            Cluster cluster = clusterService.getCluster(stackDto.getCluster().getId());
            externalDatabaseService.migrateRdsToTls(cluster);
        }
    }

    public void migrateStackToTls(Long stackId) {
        StackDto stackDto = stackDtoService.getById(stackId);
        if (clusterHasExternalDatabase(stackDto)) {
            Cluster cluster = clusterService.getCluster(stackDto.getCluster().getId());
            clusterService.enableSsl(cluster.getId());
            for (RDSConfig rdsConfig : rdsConfigService.findByClusterId(cluster.getId())) {
                rdsConfigService.enableSsl(rdsConfig.getId());
            }
        } else {
            // When only Hive connects to Data Lake Hive Database
            Cluster cluster = clusterService.getCluster(stackDto.getCluster().getId());
            for (RDSConfig rdsConfig : getHiveRdsConfigs(cluster)) {
                rdsConfigService.enableSsl(rdsConfig.getId());
            }
        }
        databaseSslService.getDbSslDetailsForCreationAndUpdateInCluster(stackDto);
    }

    public void enableSslOnClusterSide(Long stackId) {
        StackDto stackDto = stackDtoService.getById(stackId);
        ClusterDeletionBasedExitCriteriaModel clusterDeletionBasedExitCriteriaModel =
                ClusterDeletionBasedExitCriteriaModel.clusterDeletionBasedModel(stackId, stackDto.getCluster().getId());
        try {
            OrchestratorStateParams stateParams = saltStateParamsService.createStateParamsForReachableNodes(stackDto, "postgresql.enable_ssl", MAX_RETRY,
                    MAX_RETRY_ON_ERROR);
            postgresConfigService.uploadServicePillarsForPostgres(stackDto, clusterDeletionBasedExitCriteriaModel, stateParams);
            hostOrchestrator.runOrchestratorState(stateParams);
            LOGGER.info("Database SSL certificates have been distributes on stack with id: '{}'", stackId);
        } catch (CloudbreakOrchestratorException e) {
            String message = String.format("Distribution of database SSL certificates failed on %s with name: '%s'",
                    getType(stackDto.getStack()), stackDto.getName());
            LOGGER.warn(message, e);
            throw new CloudbreakServiceException(message, e);
        }
    }

    private Set<RDSConfig> getHiveRdsConfigs(Cluster cluster) {
        return rdsConfigService.findByClusterId(cluster.getId()).stream().filter(r -> r.getType().equalsIgnoreCase(DatabaseType
                .HIVE.name())).collect(Collectors.toSet());
    }

    private String getType(StackView stackView) {
        return getType(stackView.getType());
    }

    private String getType(StackType type) {
        return DATALAKE.equals(type) ? "Data Lake" : "Data Hub";
    }

    private boolean clusterHasExternalDatabase(StackDto stackDto) {
        return stackDto.getCluster().hasExternalDatabase();
    }
}
