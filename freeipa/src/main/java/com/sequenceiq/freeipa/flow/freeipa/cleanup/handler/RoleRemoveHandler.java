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
import com.sequenceiq.freeipa.flow.freeipa.cleanup.event.roles.RemoveRolesRequest;
import com.sequenceiq.freeipa.flow.freeipa.cleanup.event.roles.RemoveRolesResponse;
import com.sequenceiq.freeipa.service.freeipa.cleanup.CleanupService;

import reactor.bus.Event;
import reactor.bus.EventBus;

@Component
public class RoleRemoveHandler implements EventHandler<RemoveRolesRequest> {
    private static final Logger LOGGER = LoggerFactory.getLogger(RoleRemoveHandler.class);

    @Inject
    private EventBus eventBus;

    @Inject
    private CleanupService cleanupService;

    @Override
    public String selector() {
        return EventSelectorUtil.selector(RemoveRolesRequest.class);
    }

    @Override
    public void accept(Event<RemoveRolesRequest> event) {
        RemoveRolesRequest request = event.getData();
        try {
            Pair<Set<String>, Map<String, String>> removeRolesResult =
                    cleanupService.removeRoles(request.getResourceId(), request.getRoles());
            RemoveRolesResponse response = new RemoveRolesResponse(request, removeRolesResult.getFirst(), removeRolesResult.getSecond());
            eventBus.notify(response.getRoleCleanupFailed().isEmpty()
                            ? EventSelectorUtil.selector(RemoveRolesResponse.class) : EventSelectorUtil.failureSelector(RemoveRolesResponse.class),
                    new Event<>(event.getHeaders(), response));
        } catch (FreeIpaClientException e) {
            LOGGER.error("Removing roles failed: [{}]", request.getRoles(), e);
            Map<String, String> failureResult = request.getRoles().stream().collect(Collectors.toMap(h -> h, h -> e.getMessage()));
            RemoveRolesResponse response = new RemoveRolesResponse(request, Collections.emptySet(), failureResult);
            eventBus.notify(EventSelectorUtil.failureSelector(RemoveRolesResponse.class),
                    new Event<>(event.getHeaders(), response));
        }
    }
}
