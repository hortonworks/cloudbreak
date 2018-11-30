package com.sequenceiq.cloudbreak.reactor.handler.cluster;

import static com.sequenceiq.cloudbreak.core.bootstrap.service.ClusterDeletionBasedExitCriteriaModel.clusterDeletionBasedModel;

import java.util.Set;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.orchestrator.host.HostOrchestrator;
import com.sequenceiq.cloudbreak.orchestrator.model.GatewayConfig;
import com.sequenceiq.cloudbreak.orchestrator.model.Node;
import com.sequenceiq.cloudbreak.reactor.api.event.EventSelectorUtil;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.AmbariStopServerAndAgentRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.AmbariStopServerAndAgentResult;
import com.sequenceiq.cloudbreak.reactor.handler.ReactorEventHandler;
import com.sequenceiq.cloudbreak.service.GatewayConfigService;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.cloudbreak.util.StackUtil;

import reactor.bus.Event;
import reactor.bus.EventBus;

@Component
public class AmbariStopServerAndAgentHandler implements ReactorEventHandler<AmbariStopServerAndAgentRequest> {
    private static final Logger LOGGER = LoggerFactory.getLogger(AmbariStopServerAndAgentHandler.class);

    @Inject
    private StackService stackService;

    @Inject
    private StackUtil stackUtil;

    @Inject
    private HostOrchestrator hostOrchestrator;

    @Inject
    private GatewayConfigService gatewayConfigService;

    @Inject
    private EventBus eventBus;

    @Override
    public String selector() {
        return EventSelectorUtil.selector(AmbariStopServerAndAgentRequest.class);
    }

    @Override
    public void accept(Event<AmbariStopServerAndAgentRequest> event) {
        AmbariStopServerAndAgentRequest request = event.getData();
        Long stackId = request.getStackId();
        AmbariStopServerAndAgentResult result;
        try {
            Stack stack = stackService.getByIdWithListsInTransaction(stackId);
            GatewayConfig primaryGatewayConfig = gatewayConfigService.getPrimaryGatewayConfig(stack);
            Set<Node> allNodes = stackUtil.collectNodes(stack);

            hostOrchestrator.stopAmbariOnMaster(primaryGatewayConfig, allNodes, clusterDeletionBasedModel(stack.getId(), stack.getCluster().getId()));
            result = new AmbariStopServerAndAgentResult(request);
        } catch (Exception e) {
            String message = "Failed to start ambari agent and/or server on new host.";
            LOGGER.error(message, e);
            result = new AmbariStopServerAndAgentResult(message, e, request);
        }
        eventBus.notify(result.selector(), new Event<>(event.getHeaders(), result));
    }
}
