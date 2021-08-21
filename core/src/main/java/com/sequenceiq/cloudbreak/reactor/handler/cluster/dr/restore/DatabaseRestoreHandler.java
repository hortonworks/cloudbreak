package com.sequenceiq.cloudbreak.reactor.handler.cluster.dr.restore;

import java.util.Collections;
import java.util.Set;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.DetailedStackStatus;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.core.bootstrap.service.ClusterDeletionBasedExitCriteriaModel;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;
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
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.cloudbreak.util.StackUtil;
import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.flow.reactor.api.handler.ExceptionCatcherEventHandler;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;

import reactor.bus.Event;

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
    private StackService stackService;

    @Inject
    private StackUtil stackUtil;

    @Inject
    private RangerVirtualGroupService rangerVirtualGroupService;

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
            Stack stack = stackService.getByIdWithListsInTransaction(stackId);
            Cluster cluster = stack.getCluster();
            InstanceMetaData gatewayInstance = stack.getPrimaryGatewayInstance();
            GatewayConfig gatewayConfig = gatewayConfigService.getGatewayConfig(stack, gatewayInstance, cluster.hasGateway());
            Set<String> gatewayFQDN = Collections.singleton(gatewayInstance.getDiscoveryFQDN());
            ExitCriteriaModel exitModel = ClusterDeletionBasedExitCriteriaModel.clusterDeletionBasedModel(stackId, cluster.getId());
            String rangerAdminGroup = rangerVirtualGroupService.getRangerVirtualGroup(stack);
            SaltConfig saltConfig = saltConfigGenerator.createSaltConfig(request.getBackupLocation(), request.getBackupId(), rangerAdminGroup, true, stack);
            hostOrchestrator.restoreDatabase(gatewayConfig, gatewayFQDN, stackUtil.collectReachableNodes(stack), saltConfig, exitModel);

            result = new DatabaseRestoreSuccess(stackId);
        } catch (Exception e) {
            LOGGER.error("Database restore event failed", e);
            result = new DatabaseRestoreFailedEvent(stackId, e, DetailedStackStatus.DATABASE_RESTORE_FAILED);
        }
        return result;
    }
}
