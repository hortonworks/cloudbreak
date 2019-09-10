package com.sequenceiq.cloudbreak.reactor.handler.orchestration;

import static com.sequenceiq.cloudbreak.polling.PollingResult.SUCCESS;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.core.bootstrap.service.host.HostOrchestratorResolver;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorException;
import com.sequenceiq.cloudbreak.orchestrator.host.HostOrchestrator;
import com.sequenceiq.cloudbreak.orchestrator.model.GatewayConfig;
import com.sequenceiq.cloudbreak.polling.PollingResult;
import com.sequenceiq.cloudbreak.reactor.api.event.orchestration.RemoveHostsFailed;
import com.sequenceiq.cloudbreak.reactor.api.event.orchestration.RemoveHostsRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.orchestration.RemoveHostsSuccess;
import com.sequenceiq.cloudbreak.service.CloudbreakException;
import com.sequenceiq.cloudbreak.service.GatewayConfigService;
import com.sequenceiq.cloudbreak.service.stack.StackService;
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
    private HostOrchestratorResolver hostOrchestratorResolver;

    @Inject
    private GatewayConfigService gatewayConfigService;

    @Inject
    private EventBus eventBus;

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
            HostOrchestrator hostOrchestrator = hostOrchestratorResolver.get(stack.getOrchestrator().getType());
            List<GatewayConfig> allGatewayConfigs = gatewayConfigService.getAllGatewayConfigs(stack);
            PollingResult orchestratorRemovalPollingResult =
                    removeHostsFromOrchestrator(stack, new ArrayList<>(hostNames), hostOrchestrator, allGatewayConfigs);
            if (!PollingResult.isSuccess(orchestratorRemovalPollingResult)) {
                LOGGER.warn("Can not remove hosts from orchestrator: {}", hostNames);
            }
            result = new RemoveHostsSuccess(request.getResourceId(), request.getHostGroupName(), hostNames);
        } catch (Exception e) {
            result = new RemoveHostsFailed(removeHostsRequestEvent.getData().getResourceId(), e, request.getHostGroupName(), hostNames);
        }
        eventBus.notify(result.selector(), new Event<>(removeHostsRequestEvent.getHeaders(), result));
    }

    private PollingResult removeHostsFromOrchestrator(Stack stack, List<String> hostNames, HostOrchestrator hostOrchestrator,
            List<GatewayConfig> allGatewayConfigs) throws CloudbreakException {
        LOGGER.debug("Remove hosts from orchestrator: {}", hostNames);
        try {
            Map<String, String> privateIpsByFQDN = new HashMap<>();
            stack.getInstanceMetaDataAsList().stream()
                    .filter(instanceMetaData -> instanceMetaData.getDiscoveryFQDN() != null)
                    .filter(instanceMetaData ->
                            hostNames.stream()
                                    .anyMatch(hn -> hn.equals(instanceMetaData.getDiscoveryFQDN())))
                    .forEach(instanceMetaData -> privateIpsByFQDN.put(instanceMetaData.getDiscoveryFQDN(), instanceMetaData.getPrivateIp()));
            hostOrchestrator.tearDown(allGatewayConfigs, privateIpsByFQDN);
        } catch (CloudbreakOrchestratorException e) {
            LOGGER.info("Failed to delete orchestrator components while decommissioning: ", e);
            throw new CloudbreakException("Failed to delete orchestrator components while decommissioning: ", e);
        }
        return SUCCESS;
    }
}
