package com.sequenceiq.cloudbreak.reactor.handler.orchestration;

import jakarta.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.core.bootstrap.service.ClusterServiceRunner;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.cloudbreak.eventbus.EventBus;
import com.sequenceiq.cloudbreak.reactor.api.event.orchestration.ChangePrimaryGatewayFailed;
import com.sequenceiq.cloudbreak.reactor.api.event.orchestration.ChangePrimaryGatewayRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.orchestration.ChangePrimaryGatewaySuccess;
import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.flow.reactor.api.handler.EventHandler;

@Component
public class ChangePrimaryGatewayHandler implements EventHandler<ChangePrimaryGatewayRequest> {
    @Inject
    private EventBus eventBus;

    @Inject
    private ClusterServiceRunner clusterServiceRunner;

    @Override
    public String selector() {
        return EventSelectorUtil.selector(ChangePrimaryGatewayRequest.class);
    }

    @Override
    public void accept(Event<ChangePrimaryGatewayRequest> event) {
        ChangePrimaryGatewayRequest request = event.getData();
        Selectable response;
        try {
            response = new ChangePrimaryGatewaySuccess(request.getResourceId(), clusterServiceRunner.changePrimaryGateway(request.getResourceId()));
        } catch (Exception e) {
            response = new ChangePrimaryGatewayFailed(request.getResourceId(), e);
        }
        eventBus.notify(response.selector(), new Event<>(event.getHeaders(), response));
    }
}
