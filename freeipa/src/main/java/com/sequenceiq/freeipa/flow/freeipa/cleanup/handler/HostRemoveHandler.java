package com.sequenceiq.freeipa.flow.freeipa.cleanup.handler;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Component;

import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.flow.reactor.api.handler.EventHandler;
import com.sequenceiq.freeipa.client.FreeIpaClientException;
import com.sequenceiq.freeipa.flow.freeipa.cleanup.event.host.RemoveHostsRequest;
import com.sequenceiq.freeipa.flow.freeipa.cleanup.event.host.RemoveHostsResponse;
import com.sequenceiq.freeipa.service.freeipa.cleanup.CleanupService;

import reactor.bus.Event;
import reactor.bus.EventBus;

@Component
public class HostRemoveHandler implements EventHandler<RemoveHostsRequest> {
    private static final Logger LOGGER = LoggerFactory.getLogger(HostRemoveHandler.class);

    @Inject
    private EventBus eventBus;

    @Inject
    private CleanupService cleanupService;

    @Override
    public String selector() {
        return EventSelectorUtil.selector(RemoveHostsRequest.class);
    }

    @Override
    public void accept(Event<RemoveHostsRequest> event) {
        RemoveHostsRequest request = event.getData();
        try {
            Pair<Set<String>, Map<String, String>> removeHostsResult =
                    cleanupService.removeHosts(request.getResourceId(), request.getHosts());
            RemoveHostsResponse response = new RemoveHostsResponse(request, removeHostsResult.getFirst(), removeHostsResult.getSecond());
            eventBus.notify(response.getHostCleanupFailed().isEmpty()
                            ? EventSelectorUtil.selector(RemoveHostsResponse.class) : EventSelectorUtil.failureSelector(RemoveHostsResponse.class),
                    new Event<>(event.getHeaders(), response));
        } catch (FreeIpaClientException e) {
            LOGGER.error("Removing failed for hosts: [{}]", request.getHosts(), e);
            Map<String, String> failureResult = request.getHosts().stream().collect(Collectors.toMap(h -> h, h -> e.getMessage()));
            RemoveHostsResponse response = new RemoveHostsResponse(request, Collections.emptySet(), failureResult);
            eventBus.notify(EventSelectorUtil.failureSelector(RemoveHostsResponse.class),
                    new Event<>(event.getHeaders(), response));
        }
    }
}
