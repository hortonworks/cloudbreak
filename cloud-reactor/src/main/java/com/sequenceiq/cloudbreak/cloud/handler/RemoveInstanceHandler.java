package com.sequenceiq.cloudbreak.cloud.handler;

import javax.inject.Inject;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.event.resource.DownscaleStackResult;
import com.sequenceiq.cloudbreak.cloud.event.resource.RemoveInstanceRequest;
import com.sequenceiq.cloudbreak.cloud.event.resource.RemoveInstanceResult;

import reactor.bus.Event;
import reactor.bus.EventBus;

@Component
public class RemoveInstanceHandler implements CloudPlatformEventHandler<RemoveInstanceRequest> {

    @Inject
    private EventBus eventBus;
    @Inject
    @Qualifier("DownscaleStackHandler")
    private DownscaleStackExecuter downscaleStackExecuter;

    @Override
    public Class<RemoveInstanceRequest> type() {
        return RemoveInstanceRequest.class;
    }

    @Override
    public void accept(Event<RemoveInstanceRequest> removeInstanceRequestEvent) {
        RemoveInstanceRequest request = removeInstanceRequestEvent.getData();
        RemoveInstanceResult result;
        try {
            DownscaleStackResult downScaleResult = downscaleStackExecuter.execute(request);
            result = new RemoveInstanceResult(downScaleResult, request);
        } catch (Exception e) {
            result = new RemoveInstanceResult(e.getMessage(), e, request);
        }
        eventBus.notify(result.selector(), new Event(removeInstanceRequestEvent.getHeaders(), result));
    }

}
