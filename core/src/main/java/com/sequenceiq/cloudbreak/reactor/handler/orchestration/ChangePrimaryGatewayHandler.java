package com.sequenceiq.cloudbreak.reactor.handler.orchestration;

import com.sequenceiq.cloudbreak.cloud.event.Selectable;
import com.sequenceiq.cloudbreak.core.bootstrap.service.ClusterServiceRunner;
import com.sequenceiq.cloudbreak.reactor.api.event.EventSelectorUtil;
import com.sequenceiq.cloudbreak.reactor.api.event.orchestration.ChangePrimaryGatewayFailed;
import com.sequenceiq.cloudbreak.reactor.api.event.orchestration.ChangePrimaryGatewayRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.orchestration.ChangePrimaryGatewaySuccess;
import com.sequenceiq.cloudbreak.reactor.handler.ReactorEventHandler;
import org.springframework.stereotype.Component;
import reactor.bus.Event;
import reactor.bus.EventBus;

import javax.inject.Inject;

@Component
public class ChangePrimaryGatewayHandler implements ReactorEventHandler<ChangePrimaryGatewayRequest> {
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
            response = new ChangePrimaryGatewaySuccess(request.getStackId(), clusterServiceRunner.changePrimaryGateway(request.getStackId()));
        } catch (Exception e) {
            response = new ChangePrimaryGatewayFailed(request.getStackId(), e);
        }
        eventBus.notify(response.selector(), new Event<>(event.getHeaders(), response));
    }
}
