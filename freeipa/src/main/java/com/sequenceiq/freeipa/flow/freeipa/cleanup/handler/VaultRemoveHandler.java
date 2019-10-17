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
import com.sequenceiq.freeipa.flow.freeipa.cleanup.event.vault.RemoveVaultEntriesRequest;
import com.sequenceiq.freeipa.flow.freeipa.cleanup.event.vault.RemoveVaultEntriesResponse;
import com.sequenceiq.freeipa.service.freeipa.cleanup.CleanupService;

import reactor.bus.Event;
import reactor.bus.EventBus;

@Component
public class VaultRemoveHandler implements EventHandler<RemoveVaultEntriesRequest> {
    private static final Logger LOGGER = LoggerFactory.getLogger(VaultRemoveHandler.class);

    @Inject
    private EventBus eventBus;

    @Inject
    private CleanupService cleanupService;

    @Override
    public String selector() {
        return EventSelectorUtil.selector(RemoveVaultEntriesRequest.class);
    }

    @Override
    public void accept(Event<RemoveVaultEntriesRequest> event) {
        RemoveVaultEntriesRequest request = event.getData();
        try {
            Pair<Set<String>, Map<String, String>> removeVaultResult =
                    cleanupService.removeVaultEntries(request.getResourceId(), request.getHosts());
            RemoveVaultEntriesResponse response = new RemoveVaultEntriesResponse(request, removeVaultResult.getFirst(), removeVaultResult.getSecond());
            eventBus.notify(response.getVaultCleanupFailed().isEmpty()
                            ? EventSelectorUtil.selector(RemoveVaultEntriesResponse.class) : EventSelectorUtil.failureSelector(RemoveVaultEntriesResponse.class),
                    new Event<>(event.getHeaders(), response));
        } catch (FreeIpaClientException e) {
            LOGGER.error("Removing vault entries for hosts failed: [{}]", request.getHosts(), e);
            Map<String, String> failureResult = request.getHosts().stream().collect(Collectors.toMap(h -> h, h -> e.getMessage()));
            RemoveVaultEntriesResponse response = new RemoveVaultEntriesResponse(request, Collections.emptySet(), failureResult);
            eventBus.notify(EventSelectorUtil.failureSelector(RemoveVaultEntriesResponse.class),
                    new Event<>(event.getHeaders(), response));
        }
    }
}
