package com.sequenceiq.cloudbreak.reactor;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cluster.api.ClusterSecurityService;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.reactor.api.event.EventSelectorUtil;
import com.sequenceiq.cloudbreak.reactor.api.event.resource.ClusterCredentialChangeRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.resource.ClusterCredentialChangeResult;
import com.sequenceiq.cloudbreak.reactor.handler.ReactorEventHandler;
import com.sequenceiq.cloudbreak.service.cluster.ClusterApiConnectors;
import com.sequenceiq.cloudbreak.service.stack.StackService;

import reactor.bus.Event;
import reactor.bus.EventBus;

@Component
public class ClusterCredentialChangeHandler implements ReactorEventHandler<ClusterCredentialChangeRequest> {

    @Inject
    private ClusterApiConnectors clusterApiConnectors;

    @Inject
    private StackService stackService;

    @Inject
    private EventBus eventBus;

    @Override
    public String selector() {
        return EventSelectorUtil.selector(ClusterCredentialChangeRequest.class);
    }

    @Override
    public void accept(Event<ClusterCredentialChangeRequest> event) {
        ClusterCredentialChangeRequest request = event.getData();
        ClusterCredentialChangeResult result;
        try {
            Stack stack = stackService.getByIdWithListsInTransaction(request.getStackId());
            ClusterSecurityService clusterSecurityService = clusterApiConnectors.getConnector(stack).clusterSecurityService();
            switch (request.getType()) {
                case REPLACE:
                    clusterSecurityService.replaceUserNamePassword(request.getUser(), request.getPassword());
                    break;
                case UPDATE:
                    clusterSecurityService.updateUserNamePassword(request.getPassword());
                    break;
                default:
                    throw new UnsupportedOperationException("Ambari credential update request not supported: " + request.getType());
            }
            result = new ClusterCredentialChangeResult(request);
        } catch (Exception e) {
            result = new ClusterCredentialChangeResult(e.getMessage(), e, request);
        }
        eventBus.notify(result.selector(), new Event<>(event.getHeaders(), result));
    }
}
