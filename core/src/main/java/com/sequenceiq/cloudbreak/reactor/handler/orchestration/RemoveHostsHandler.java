package com.sequenceiq.cloudbreak.reactor.handler.orchestration;

import java.util.ArrayList;
import java.util.Set;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.reactor.api.event.orchestration.RemoveHostsFailed;
import com.sequenceiq.cloudbreak.reactor.api.event.orchestration.RemoveHostsRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.orchestration.RemoveHostsSuccess;
import com.sequenceiq.cloudbreak.service.stack.StackService;

import reactor.bus.Event;
import reactor.bus.EventBus;

@Component
public class RemoveHostsHandler implements ReactorEventHandler<RemoveHostsRequest> {
    private static final Logger LOGGER = LoggerFactory.getLogger(RemoveHostsHandler.class);

    @Inject
    private AmbariDecommissioner ambariDecommissioner;

    @Inject
    private StackService stackService;

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
            Stack stack = stackService.getByIdWithListsInTransaction(request.getStackId());
            PollingResult orchestratorRemovalPollingResult = ambariDecommissioner.removeHostsFromOrchestrator(stack, new ArrayList<>(hostNames));
            if (!isSuccess(orchestratorRemovalPollingResult)) {
                LOGGER.warn("Can not remove hosts from orchestrator: {}", hostNames);
            }
            result = new RemoveHostsSuccess(request.getStackId(), request.getHostGroupName(), hostNames);
        } catch (Exception e) {
            result = new RemoveHostsFailed(removeHostsRequestEvent.getData().getStackId(), e, request.getHostGroupName(), hostNames);
        }
        eventBus.notify(result.selector(), new Event<>(removeHostsRequestEvent.getHeaders(), result));
    }
}
