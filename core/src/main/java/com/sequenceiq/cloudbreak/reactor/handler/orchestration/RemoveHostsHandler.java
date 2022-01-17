package com.sequenceiq.cloudbreak.reactor.handler.orchestration;

import static com.sequenceiq.cloudbreak.core.bootstrap.service.ClusterDeletionBasedExitCriteriaModel.clusterDeletionBasedModel;
import static com.sequenceiq.cloudbreak.polling.PollingResult.SUCCESS;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.common.orchestration.Node;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorException;
import com.sequenceiq.cloudbreak.orchestrator.host.HostOrchestrator;
import com.sequenceiq.cloudbreak.orchestrator.model.GatewayConfig;
import com.sequenceiq.cloudbreak.orchestrator.state.ExitCriteriaModel;
import com.sequenceiq.cloudbreak.polling.PollingResult;
import com.sequenceiq.cloudbreak.reactor.api.event.orchestration.RemoveHostsFailed;
import com.sequenceiq.cloudbreak.reactor.api.event.orchestration.RemoveHostsRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.orchestration.RemoveHostsSuccess;
import com.sequenceiq.cloudbreak.service.CloudbreakException;
import com.sequenceiq.cloudbreak.service.GatewayConfigService;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.cloudbreak.util.StackUtil;
import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.flow.reactor.api.handler.EventHandler;

import reactor.bus.Event;
import reactor.bus.EventBus;

@Component
public class RemoveHostsHandler implements EventHandler<RemoveHostsRequest> {
    private static final Logger LOGGER = LoggerFactory.getLogger(RemoveHostsHandler.class);

    @Inject
    private StackService stackService;

    @Inject
    private HostOrchestrator hostOrchestrator;

    @Inject
    private GatewayConfigService gatewayConfigService;

    @Inject
    private EventBus eventBus;

    @Inject
    private StackUtil stackUtil;

    @Override
    public String selector() {
        return EventSelectorUtil.selector(RemoveHostsRequest.class);
    }

    @Override
    public void accept(Event<RemoveHostsRequest> removeHostsRequestEvent) {
        RemoveHostsRequest request = removeHostsRequestEvent.getData();
        Set<String> hostNames = request.getHostNames();
        Selectable result;
        try {
            Stack stack = stackService.getByIdWithListsInTransaction(request.getResourceId());
            if (stack.getPrimaryGatewayInstance() != null && stack.getPrimaryGatewayInstance().isReachable()) {
                List<GatewayConfig> allGatewayConfigs = gatewayConfigService.getAllGatewayConfigs(stack);
                PollingResult orchestratorRemovalPollingResult =
                        removeHostsFromOrchestrator(stack, new ArrayList<>(hostNames), hostOrchestrator, allGatewayConfigs);
                if (!orchestratorRemovalPollingResult.isSuccess()) {
                    LOGGER.warn("Can not remove hosts from orchestrator: {}", hostNames);
                }
            } else {
                LOGGER.warn("Primary gateway is not reachable, can't remove hosts from orchestrator");
            }
            result = new RemoveHostsSuccess(request.getResourceId(), request.getHostGroupNames(), hostNames);
        } catch (Exception e) {
            result = new RemoveHostsFailed(removeHostsRequestEvent.getData().getResourceId(), e, request.getHostGroupNames(), hostNames);
        }
        eventBus.notify(result.selector(), new Event<>(removeHostsRequestEvent.getHeaders(), result));
    }

    private PollingResult removeHostsFromOrchestrator(Stack stack, List<String> hostNames, HostOrchestrator hostOrchestrator,
            List<GatewayConfig> allGatewayConfigs) throws CloudbreakException {
        LOGGER.debug("Remove hosts from orchestrator: {}", hostNames);
        try {
            Map<String, String> removeNodePrivateIPsByFQDN = new HashMap<>();
            stack.getNotTerminatedInstanceMetaDataSet().stream()
                    .filter(instanceMetaData -> instanceMetaData.getDiscoveryFQDN() != null)
                    .filter(instanceMetaData ->
                            hostNames.stream()
                                    .anyMatch(hn -> hn.equals(instanceMetaData.getDiscoveryFQDN())))
                    .forEach(instanceMetaData -> removeNodePrivateIPsByFQDN.put(instanceMetaData.getDiscoveryFQDN(), instanceMetaData.getPrivateIp()));
            Set<Node> remainingNodes = stackUtil.collectNodes(stack)
                    .stream().filter(node -> !removeNodePrivateIPsByFQDN.containsValue(node.getPrivateIp())).collect(Collectors.toSet());
            ExitCriteriaModel exitCriteriaModel = clusterDeletionBasedModel(stack.getId(), stack.getCluster().getId());
            hostOrchestrator.tearDown(stack, allGatewayConfigs, removeNodePrivateIPsByFQDN, remainingNodes, exitCriteriaModel);
        } catch (CloudbreakOrchestratorException e) {
            LOGGER.error("Failed to delete orchestrator components while decommissioning: ", e);
            throw new CloudbreakException("Removing selected nodes from master node failed, " +
                    "please check if selected nodes are still reachable from master node using terminal and " +
                    "nodes are in sync between CM, CDP and provider side or you can remove selected nodes forcefully.");
        }
        return SUCCESS;
    }
}
