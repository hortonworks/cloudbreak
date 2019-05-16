package com.sequenceiq.cloudbreak.reactor;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.cloudbreak.reactor.api.event.resource.MountDisksOnNewHostsRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.resource.MountDisksOnNewHostsResult;
import com.sequenceiq.flow.handler.EventHandler;
import com.sequenceiq.cloudbreak.service.stack.flow.MountDisks;

import reactor.bus.Event;
import reactor.bus.EventBus;

@Component
public class MountDisksOnNewNodesHandler implements EventHandler<MountDisksOnNewHostsRequest> {

    private static final Logger LOGGER = LoggerFactory.getLogger(MountDisksOnNewNodesHandler.class);

    @Inject
    private EventBus eventBus;

    @Inject
    private MountDisks mountDisks;

    @Override
    public String selector() {
        return EventSelectorUtil.selector(MountDisksOnNewHostsRequest.class);
    }

    @Override
    public void accept(Event<MountDisksOnNewHostsRequest> event) {
        MountDisksOnNewHostsRequest request = event.getData();
        MountDisksOnNewHostsResult result;
        try {
            mountDisks.mountDisksOnNewNodes(request.getResourceId(), request.getUpscaleCandidateAddresses());
            result = new MountDisksOnNewHostsResult(request);
        } catch (Exception e) {
            LOGGER.info("Failed to mount disks on new nodes. ", e);
            result = new MountDisksOnNewHostsResult(e.getMessage(), e, request);
        }
        eventBus.notify(result.selector(), new Event<>(event.getHeaders(), result));
    }
}
