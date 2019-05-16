package com.sequenceiq.cloudbreak.reactor.handler.orchestration;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.orchestration.MountDisksFailed;
import com.sequenceiq.cloudbreak.reactor.api.event.orchestration.MountDisksRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.orchestration.MountDisksSuccess;
import com.sequenceiq.flow.reactor.api.handler.EventHandler;
import com.sequenceiq.cloudbreak.service.stack.flow.MountDisks;

import reactor.bus.Event;
import reactor.bus.EventBus;

@Component
public class MountDisksHandler implements EventHandler<MountDisksRequest> {

    private static final Logger LOGGER = LoggerFactory.getLogger(MountDisksHandler.class);

    @Inject
    private EventBus eventBus;

    @Inject
    private MountDisks mountDisks;

    @Override
    public String selector() {
        return EventSelectorUtil.selector(MountDisksRequest.class);
    }

    @Override
    public void accept(Event<MountDisksRequest> event) {
        StackEvent request = event.getData();
        Selectable response;
        try {
            mountDisks.mountAllDisks(request.getResourceId());
            response = new MountDisksSuccess(request.getResourceId());
        } catch (Exception e) {
            LOGGER.info("Mount disks failed. ", e);
            response = new MountDisksFailed(request.getResourceId(), e);
        }
        eventBus.notify(response.selector(), new Event<>(event.getHeaders(), response));
    }
}
