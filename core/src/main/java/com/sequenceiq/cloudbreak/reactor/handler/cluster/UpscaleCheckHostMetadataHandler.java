package com.sequenceiq.cloudbreak.reactor.handler.cluster;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.cloudbreak.eventbus.EventBus;
import com.sequenceiq.cloudbreak.reactor.api.event.resource.UpscaleCheckHostMetadataRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.resource.UpscaleCheckHostMetadataResult;
import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.flow.reactor.api.handler.EventHandler;

@Component
public class UpscaleCheckHostMetadataHandler implements EventHandler<UpscaleCheckHostMetadataRequest> {

    @Inject
    private EventBus eventBus;

    @Override
    public String selector() {
        return EventSelectorUtil.selector(UpscaleCheckHostMetadataRequest.class);
    }

    @Override
    public void accept(Event<UpscaleCheckHostMetadataRequest> event) {
        UpscaleCheckHostMetadataRequest request = event.getData();
        UpscaleCheckHostMetadataResult result;
        try {
            // TODO: we don't need this step anymore
            result = new UpscaleCheckHostMetadataResult(request);
        } catch (Exception e) {
            result = new UpscaleCheckHostMetadataResult(e.getMessage(), e, request);
        }
        eventBus.notify(result.selector(), new Event<>(event.getHeaders(), result));
    }
}
