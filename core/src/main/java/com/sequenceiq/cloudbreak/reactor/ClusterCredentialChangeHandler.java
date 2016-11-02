package com.sequenceiq.cloudbreak.reactor;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.reactor.api.event.resource.ClusterCredentialChangeRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.resource.ClusterCredentialChangeResult;
import com.sequenceiq.cloudbreak.service.cluster.flow.AmbariClusterConnector;
import com.sequenceiq.cloudbreak.service.stack.StackService;

import reactor.bus.Event;
import reactor.bus.EventBus;

@Component
public class ClusterCredentialChangeHandler implements ClusterEventHandler<ClusterCredentialChangeRequest> {
    @Inject
    private AmbariClusterConnector ambariClusterConnector;

    @Inject
    private StackService stackService;

    @Inject
    private EventBus eventBus;

    @Override
    public Class<ClusterCredentialChangeRequest> type() {
        return ClusterCredentialChangeRequest.class;
    }

    @Override
    public void accept(Event<ClusterCredentialChangeRequest> event) {
        ClusterCredentialChangeRequest request = event.getData();
        ClusterCredentialChangeResult result;
        try {
            Stack stack = stackService.getById(request.getStackId());
            ambariClusterConnector.credentialChangeAmbariCluster(stack.getId(), request.getUser(), request.getPassword());
            result = new ClusterCredentialChangeResult(request);
        } catch (Exception e) {
            result = new ClusterCredentialChangeResult(e.getMessage(), e, request);
        }
        eventBus.notify(result.selector(), new Event(event.getHeaders(), result));
    }
}
