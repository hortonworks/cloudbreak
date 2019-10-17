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
import com.sequenceiq.freeipa.flow.freeipa.cleanup.event.users.RemoveUsersRequest;
import com.sequenceiq.freeipa.flow.freeipa.cleanup.event.users.RemoveUsersResponse;
import com.sequenceiq.freeipa.service.freeipa.cleanup.CleanupService;

import reactor.bus.Event;
import reactor.bus.EventBus;

@Component
public class UserRemoveHandler implements EventHandler<RemoveUsersRequest> {
    private static final Logger LOGGER = LoggerFactory.getLogger(UserRemoveHandler.class);

    @Inject
    private EventBus eventBus;

    @Inject
    private CleanupService cleanupService;

    @Override
    public String selector() {
        return EventSelectorUtil.selector(RemoveUsersRequest.class);
    }

    @Override
    public void accept(Event<RemoveUsersRequest> event) {
        RemoveUsersRequest request = event.getData();
        try {
            Pair<Set<String>, Map<String, String>> removeUsersResult =
                    cleanupService.removeUsers(request.getResourceId(), request.getUsers(), request.getClusterName());
            RemoveUsersResponse response = new RemoveUsersResponse(request, removeUsersResult.getFirst(), removeUsersResult.getSecond());
            eventBus.notify(response.getUserCleanupFailed().isEmpty()
                            ? EventSelectorUtil.selector(RemoveUsersResponse.class) : EventSelectorUtil.failureSelector(RemoveUsersResponse.class),
                    new Event<>(event.getHeaders(), response));
        } catch (FreeIpaClientException e) {
            LOGGER.error("Removing users failed: [{}]", request.getUsers(), e);
            Map<String, String> failureResult = request.getUsers().stream().collect(Collectors.toMap(h -> h, h -> e.getMessage()));
            RemoveUsersResponse response = new RemoveUsersResponse(request, Collections.emptySet(), failureResult);
            eventBus.notify(EventSelectorUtil.failureSelector(RemoveUsersResponse.class),
                    new Event<>(event.getHeaders(), response));
        }
    }
}
