package com.sequenceiq.cloudbreak.reactor.handler.cluster.upgrade.rds;

import static com.sequenceiq.cloudbreak.auth.altus.model.Entitlement.CDP_POSTGRES_UPGRADE_SKIP_SERVICE_STOP;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.DetailedStackStatus;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.core.bootstrap.service.ClusterDeletionBasedExitCriteriaModel;
import com.sequenceiq.cloudbreak.core.flow2.cluster.rds.upgrade.UpgradeRdsService;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.cloudbreak.orchestrator.host.HostOrchestrator;
import com.sequenceiq.cloudbreak.orchestrator.model.GatewayConfig;
import com.sequenceiq.cloudbreak.orchestrator.state.ExitCriteriaModel;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.upgrade.rds.UpgradeRdsFailedEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.upgrade.rds.UpgradeRdsStartCMRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.upgrade.rds.UpgradeRdsStartCMResult;
import com.sequenceiq.cloudbreak.service.GatewayConfigService;
import com.sequenceiq.cloudbreak.service.cluster.ClusterApiConnectors;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;
import com.sequenceiq.cloudbreak.util.StackUtil;
import com.sequenceiq.cloudbreak.view.ClusterView;
import com.sequenceiq.cloudbreak.view.InstanceMetadataView;
import com.sequenceiq.cloudbreak.view.StackView;
import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.flow.reactor.api.handler.ExceptionCatcherEventHandler;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;

@Component
public class StartClusterManagerHandler extends ExceptionCatcherEventHandler<UpgradeRdsStartCMRequest> {
    private static final Logger LOGGER = LoggerFactory.getLogger(StartClusterManagerHandler.class);

    @Inject
    private StackDtoService stackDtoService;

    @Inject
    private GatewayConfigService gatewayConfigService;

    @Inject
    private ClusterApiConnectors clusterApiConnectors;

    @Inject
    private StackUtil stackUtil;

    @Inject
    private HostOrchestrator hostOrchestrator;

    @Inject
    private UpgradeRdsService upgradeRdsService;

    @Override
    public String selector() {
        return EventSelectorUtil.selector(UpgradeRdsStartCMRequest.class);
    }

    @Override
    protected Selectable defaultFailureEvent(Long resourceId, Exception e, Event<UpgradeRdsStartCMRequest> event) {
        LOGGER.error("Starting ClusterManager for RDS upgrade has failed", e);
        return new UpgradeRdsFailedEvent(resourceId, e, DetailedStackStatus.DATABASE_UPGRADE_FAILED);
    }

    @Override
    public Selectable doAccept(HandlerEvent<UpgradeRdsStartCMRequest> event) {
        UpgradeRdsStartCMRequest request = event.getData();
        LOGGER.info("Starting ClusterManager after RDS upgrade...");
        Long stackId = request.getResourceId();
        StackDto stackDto = stackDtoService.getById(stackId);
        ClusterView cluster = stackDto.getCluster();
        StackView stack = stackDto.getStack();
        try {
            InstanceMetadataView gatewayInstance = stackDto.getPrimaryGatewayInstance();
            GatewayConfig gatewayConfig = gatewayConfigService.getGatewayConfig(stack, stackDto.getSecurityConfig(), gatewayInstance, stackDto.hasGateway());
            ExitCriteriaModel exitModel = ClusterDeletionBasedExitCriteriaModel.clusterDeletionBasedModel(stack.getId(), cluster.getId());
            LOGGER.info("Starting cluster manager server and its agents after RDS upgrade...");
            hostOrchestrator.startClusterManagerWithItsAgents(gatewayConfig, stackUtil.collectReachableNodes(stackDto), exitModel);
            if (upgradeRdsService.shouldStopStartServices(stack)) {
                LOGGER.info("Starting services after RDS upgrade...");
                clusterApiConnectors.getConnector(stackDto).startClusterManagerAndAgents();
            } else {
                LOGGER.info("Skip starting services as {} entitlement is enabled.", CDP_POSTGRES_UPGRADE_SKIP_SERVICE_STOP);
            }
        } catch (Exception ex) {
            LOGGER.warn("Start cluster manager has failed", ex);
            return new UpgradeRdsFailedEvent(stackId, ex, DetailedStackStatus.DATABASE_UPGRADE_FAILED);
        }
        return new UpgradeRdsStartCMResult(stackId, request.getVersion());
    }
}
