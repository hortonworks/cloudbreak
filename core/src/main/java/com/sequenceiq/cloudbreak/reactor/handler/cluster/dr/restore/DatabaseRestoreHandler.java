package com.sequenceiq.cloudbreak.reactor.handler.cluster.dr.restore;

import java.util.Collections;
import java.util.Objects;
import java.util.Set;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.DetailedStackStatus;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.core.bootstrap.service.ClusterDeletionBasedExitCriteriaModel;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.cloudbreak.orchestrator.host.HostOrchestrator;
import com.sequenceiq.cloudbreak.orchestrator.model.GatewayConfig;
import com.sequenceiq.cloudbreak.orchestrator.model.SaltConfig;
import com.sequenceiq.cloudbreak.orchestrator.state.ExitCriteriaModel;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.dr.restore.DatabaseRestoreFailedEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.dr.restore.DatabaseRestoreRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.dr.restore.DatabaseRestoreSuccess;
import com.sequenceiq.cloudbreak.reactor.handler.cluster.dr.BackupRestoreSaltConfigGenerator;
import com.sequenceiq.cloudbreak.reactor.handler.cluster.dr.RangerVirtualGroupService;
import com.sequenceiq.cloudbreak.service.GatewayConfigService;
import com.sequenceiq.cloudbreak.service.datalake.SdxClientService;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;
import com.sequenceiq.cloudbreak.view.ClusterView;
import com.sequenceiq.cloudbreak.view.InstanceMetadataView;
import com.sequenceiq.cloudbreak.view.StackView;
import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.flow.reactor.api.handler.ExceptionCatcherEventHandler;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;
import com.sequenceiq.sdx.api.model.SdxBackupRestoreSettingsResponse;

@Component
public class DatabaseRestoreHandler extends ExceptionCatcherEventHandler<DatabaseRestoreRequest> {
    private static final Logger LOGGER = LoggerFactory.getLogger(DatabaseRestoreHandler.class);

    @Inject
    private BackupRestoreSaltConfigGenerator saltConfigGenerator;

    @Inject
    private GatewayConfigService gatewayConfigService;

    @Inject
    private HostOrchestrator hostOrchestrator;

    @Inject
    private StackDtoService stackDtoService;

    @Inject
    private RangerVirtualGroupService rangerVirtualGroupService;

    @Inject
    private SdxClientService sdxClientService;

    @Override
    public String selector() {
        return EventSelectorUtil.selector(DatabaseRestoreRequest.class);
    }

    @Override
    protected Selectable defaultFailureEvent(Long resourceId, Exception e, Event<DatabaseRestoreRequest> event) {
        return new DatabaseRestoreFailedEvent(resourceId, e, DetailedStackStatus.DATABASE_RESTORE_FAILED);
    }

    @Override
    protected Selectable doAccept(HandlerEvent<DatabaseRestoreRequest> event) {
        DatabaseRestoreRequest request = event.getData();
        Selectable result;
        Long stackId = request.getResourceId();
        LOGGER.debug("Restoring database on stack {}, backup id {}", stackId, request.getBackupId());
        try {
            StackDto stackDto = stackDtoService.getById(stackId);
            String tempBackupDir = BackupRestoreSaltConfigGenerator.DEFAULT_LOCAL_BACKUP_DIR;
            String tempRestoreDir = BackupRestoreSaltConfigGenerator.DEFAULT_LOCAL_BACKUP_DIR;
            ClusterView cluster = stackDto.getCluster();
            StackView stack = stackDto.getStack();
            SdxBackupRestoreSettingsResponse sdxBackupRestoreSettingsResponse = sdxClientService.getBackupRestoreSettings(stackDto.getResourceCrn());
            if (Objects.nonNull(sdxBackupRestoreSettingsResponse)) {
                LOGGER.info("Custom configuration exist {}", sdxBackupRestoreSettingsResponse);
                if (Objects.nonNull(sdxBackupRestoreSettingsResponse.getBackupTempLocation())) {
                    tempBackupDir = sdxBackupRestoreSettingsResponse.getBackupTempLocation();
                }
                if (Objects.nonNull(sdxBackupRestoreSettingsResponse.getRestoreTempLocation())) {
                    tempRestoreDir = sdxBackupRestoreSettingsResponse.getRestoreTempLocation();
                }
            }
            InstanceMetadataView gatewayInstance = stackDto.getPrimaryGatewayInstance();
            GatewayConfig gatewayConfig = gatewayConfigService.getGatewayConfig(stack, stackDto.getSecurityConfig(), gatewayInstance, stackDto.hasGateway());
            Set<String> gatewayFQDN = Collections.singleton(gatewayInstance.getDiscoveryFQDN());
            ExitCriteriaModel exitModel = ClusterDeletionBasedExitCriteriaModel.clusterDeletionBasedModel(stackId, cluster.getId());
            String rangerAdminGroup = rangerVirtualGroupService.getRangerVirtualGroup(stack);
            SaltConfig saltConfig = saltConfigGenerator.createSaltConfig(request.getBackupLocation(), request.getBackupId(), rangerAdminGroup,
                    true, Collections.emptyList(), false, stack, cluster.isRangerRazEnabled());
            saltConfig = saltConfigGenerator.createSaltConfig(saltConfig, tempBackupDir, tempRestoreDir);
            if (event.getData().isDryRun()) {
                hostOrchestrator.restoreDryRunValidation(gatewayConfig, gatewayFQDN, saltConfig, exitModel, request.getDatabaseMaxDurationInMin());
            } else {
                hostOrchestrator.restoreDatabase(gatewayConfig, gatewayFQDN, saltConfig, exitModel, request.getDatabaseMaxDurationInMin());
            }

            result = new DatabaseRestoreSuccess(stackId);
        } catch (Exception e) {
            LOGGER.error("Database restore event failed", e);
            result = new DatabaseRestoreFailedEvent(stackId, e, DetailedStackStatus.DATABASE_RESTORE_FAILED);
        }
        return result;
    }
}
