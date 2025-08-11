package com.sequenceiq.cloudbreak.core.flow2.cluster.rds.upgrade;

import static com.sequenceiq.cloudbreak.cluster.model.CMConfigUpdateStrategy.FALLBACK_TO_ROLLCONFIG;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.map.HashedMap;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.google.common.collect.Table;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status;
import com.sequenceiq.cloudbreak.api.endpoint.v4.database.base.DatabaseType;
import com.sequenceiq.cloudbreak.cloud.scheduler.PollGroup;
import com.sequenceiq.cloudbreak.cloud.store.InMemoryStateStore;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessor;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessorFactory;
import com.sequenceiq.cloudbreak.domain.RDSConfig;
import com.sequenceiq.cloudbreak.domain.projection.StackClusterStatusView;
import com.sequenceiq.cloudbreak.domain.projection.StackIdView;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.dto.StackDtoDelegate;
import com.sequenceiq.cloudbreak.event.ResourceEvent;
import com.sequenceiq.cloudbreak.service.rdsconfig.RdsConfigService;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.cloudbreak.structuredevent.event.CloudbreakEventService;
import com.sequenceiq.cloudbreak.validation.AllRoleTypes;

@Service
public class AttachedDatahubsRdsSettingsMigrationService {
    private static final Logger LOGGER = LoggerFactory.getLogger(AttachedDatahubsRdsSettingsMigrationService.class);

    private static final int MAX_EXCEPTIONMSG_LENGTH_LENGTH = 100;

    @Inject
    private StackDtoService stackDtoService;

    @Inject
    private StackService stackService;

    @Inject
    private RdsSettingsMigrationService rdsSettingsMigrationService;

    @Inject
    private RdsConfigService rdsConfigService;

    @Inject
    private CloudbreakEventService cloudbreakEventService;

    @Inject
    private CmTemplateProcessorFactory cmTemplateProcessorFactory;

    public void migrate(Long stackId) throws Exception {
        StackDto stackDto = stackDtoService.getById(stackId);
        if (stackDto.getStack().isDatalake()) {
            Set<StackIdView> datahubIds = stackService.findNotTerminatedByDatalakeCrn(stackDto.getResourceCrn());
            if (CollectionUtils.isNotEmpty(datahubIds)) {
                migrateDatahubs(stackId, datahubIds, stackDto);
            }
        }
    }

    private void migrateDatahubs(Long stackId, Set<StackIdView> datahubIds, StackDto stackDto) {
        List<StackClusterStatusView> datahubStatuses =
                stackService.findStatusesByIds(datahubIds.stream().map(StackIdView::getId).collect(Collectors.toList()));
        LOGGER.debug("Attached datahubs for datalake {}: {}", stackDto.getName(),
                datahubStatuses.stream().map(StackClusterStatusView::getName).collect(Collectors.joining(",")));
        Map<MigrationState, List<StackClusterStatusView>> datahubStatusesMap = datahubStatuses.stream().collect(Collectors.groupingBy(this::getUpdateStatus));
        LOGGER.debug("The following datahubs db params will be updated: {}", datahubStatusesMap.getOrDefault(MigrationState.UPDATABLE, List.of()).stream()
                .map(StackClusterStatusView::getName).collect(Collectors.joining(",")));
        LOGGER.debug("The following datahubs db params update will be skipped as they are not available: {}",
                datahubStatusesMap.getOrDefault(MigrationState.SKIPPED, List.of()).stream()
                        .map(StackClusterStatusView::getName).collect(Collectors.joining(",")));
        Map<StackClusterStatusView, Exception> updateFailedClusters = new HashedMap<>();
        List<StackClusterStatusView> updatedClusters = new ArrayList<>();
        for (StackClusterStatusView datahubStatus : datahubStatusesMap.getOrDefault(MigrationState.UPDATABLE, List.of())) {
            try {
                updateCMServiceConfigs(datahubStatus);
                updatedClusters.add(datahubStatus);
            } catch (Exception ex) {
                LOGGER.error("Error happened during cm services db upgrade param on cluster with crn {}", datahubStatus.getCrn(), ex);
                updateFailedClusters.put(datahubStatus, ex);
            }
        }
        handleErrorsAndWarnings(stackId, datahubStatusesMap, updatedClusters, updateFailedClusters);
    }

    private MigrationState getUpdateStatus(StackClusterStatusView datahubStatus) {
        if (Status.isClusterAvailable(datahubStatus.getStatus(), datahubStatus.getClusterStatus())) {
            return MigrationState.UPDATABLE;
        } else {
            return MigrationState.SKIPPED;
        }
    }

    private void handleErrorsAndWarnings(Long stackId, Map<MigrationState, List<StackClusterStatusView>> datahubStatusesMap,
            List<StackClusterStatusView> updatedClusters, Map<StackClusterStatusView, Exception> updateFailedClusters) {
        String updatedClustersStr = updatedClusters.stream()
                .map(StackClusterStatusView::getName)
                .collect(Collectors.joining(", "));
        String skippedClusters = datahubStatusesMap.getOrDefault(MigrationState.SKIPPED, List.of()).stream()
                .map(statusView -> String.format("%s [stackStatus: %s, clusterStatus: %s]",
                        statusView.getName(), statusView.getStatus(), statusView.getClusterStatus()))
                .collect(Collectors.joining(", "));
        ResourceEvent resourceEvent;
        List<String> eventParameters;
        if (updateFailedClusters.isEmpty()) {
            resourceEvent = ResourceEvent.CLUSTER_RDS_UPGRADE_ATTACHED_DATAHUBS_MIGRATE_DBSETTINGS_FINISHED;
            eventParameters = List.of(updatedClustersStr, skippedClusters);
        } else {
            String failedClusters = updateFailedClusters.entrySet().stream()
                    .map(entry -> entry.getKey().getName() + ": " + abbreviate(entry.getValue().getMessage(), MAX_EXCEPTIONMSG_LENGTH_LENGTH))
                    .collect(Collectors.joining(", "));
            resourceEvent = ResourceEvent.CLUSTER_RDS_UPGRADE_ATTACHED_DATAHUBS_MIGRATE_SETTINGS_FAILED;
            eventParameters = List.of(updatedClustersStr, skippedClusters, failedClusters);
        }
        cloudbreakEventService.fireCloudbreakEvent(stackId, Status.UPDATE_IN_PROGRESS.name(), resourceEvent, eventParameters);
    }

    private void updateCMServiceConfigs(StackClusterStatusView datahubStatusView) throws Exception {
        if (datahubStatusView.getClusterId() != null) {
            Long clusterId = datahubStatusView.getClusterId();
            Predicate<RDSConfig> cmServicePredicate = this::isClusterService;
            Predicate<RDSConfig> sharedDbPredicate = this::isSharedDatabase;
            Set<RDSConfig> rdsConfigs = rdsSettingsMigrationService.collectRdsConfigs(clusterId, cmServicePredicate.and(sharedDbPredicate));
            if (!rdsConfigs.isEmpty()) {
                StackDtoDelegate datahub = stackDtoService.getById(datahubStatusView.getId());
                if (shouldReloadDatabaseConfig(getCmTemplateProcessor(datahub))) {
                    try {
                        cloudbreakEventService.fireCloudbreakEvent(datahub.getStack().getId(), Status.UPDATE_IN_PROGRESS.name(),
                                ResourceEvent.CLUSTER_RDS_UPGRADE_ATTACHED_DATAHUB_MIGRATE_DBSETTINGS_STARTED);
                        InMemoryStateStore.putStack(datahub.getStack().getId(), PollGroup.POLLABLE);
                        InMemoryStateStore.putCluster(datahub.getCluster().getId(), PollGroup.POLLABLE);
                        Table<String, String, String> cmServiceConfigs = rdsSettingsMigrationService.collectCMServiceConfigs(rdsConfigs);
                        LOGGER.debug("The following db params will be updated in {}: {}", datahub.getName(), cmServiceConfigs);
                        rdsSettingsMigrationService.updateCMServiceConfigs(datahub, cmServiceConfigs, FALLBACK_TO_ROLLCONFIG, true);
                        cloudbreakEventService.fireCloudbreakEvent(datahub.getStack().getId(), Status.AVAILABLE.name(),
                                ResourceEvent.CLUSTER_RDS_UPGRADE_ATTACHED_DATAHUB_MIGRATE_DBSETTINGS_FINISHED);
                    } catch (Exception exception) {
                        cloudbreakEventService.fireCloudbreakEvent(datahub.getStack().getId(), Status.UPDATE_FAILED.name(),
                                ResourceEvent.CLUSTER_RDS_UPGRADE_ATTACHED_DATAHUB_MIGRATE_SETTINGS_FAILED, List.of(exception.getMessage()));
                        throw exception;
                    } finally {
                        InMemoryStateStore.deleteCluster(datahub.getCluster().getId());
                        InMemoryStateStore.deleteStack(datahub.getStack().getId());
                    }
                }
            }
        }
    }

    private boolean isClusterService(RDSConfig rdsConfig) {
        return !DatabaseType.CLOUDERA_MANAGER.name().equals(rdsConfig.getType());
    }

    private boolean isSharedDatabase(RDSConfig rdsConfig) {
        return rdsConfigService.countOfClustersUsingResource(rdsConfig) > 1;
    }

    private boolean shouldReloadDatabaseConfig(CmTemplateProcessor blueprintProcessor) {
        return blueprintProcessor.doesCMComponentExistsInBlueprint(AllRoleTypes.HIVEMETASTORE.name());
    }

    private CmTemplateProcessor getCmTemplateProcessor(StackDtoDelegate stackDto) {
        String blueprintText = stackDto.getBlueprintJsonText();
        return cmTemplateProcessorFactory.get(blueprintText);
    }

    private String abbreviate(String str, int max) {
        return StringUtils.abbreviate(str, max);
    }

    private enum MigrationState {
        UPDATABLE,
        SKIPPED,
        UPDATED,
        FAILED
    }
}
