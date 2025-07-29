package com.sequenceiq.freeipa.flow.freeipa.cleanup.handler;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.cloudbreak.eventbus.EventBus;
import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.flow.reactor.api.handler.EventHandler;
import com.sequenceiq.freeipa.entity.FreeIpa;
import com.sequenceiq.freeipa.flow.freeipa.cleanup.event.dns.RemoveDnsRequest;
import com.sequenceiq.freeipa.flow.freeipa.cleanup.event.dns.RemoveDnsResponse;
import com.sequenceiq.freeipa.service.freeipa.FreeIpaService;
import com.sequenceiq.freeipa.service.freeipa.cleanup.CleanupService;
import com.sequenceiq.freeipa.service.stack.StackService;

@Component
public class DnsRemoveHandler implements EventHandler<RemoveDnsRequest> {
    private static final Logger LOGGER = LoggerFactory.getLogger(DnsRemoveHandler.class);

    @Inject
    private EventBus eventBus;

    @Inject
    private CleanupService cleanupService;

    @Inject
    private FreeIpaService freeIpaService;

    @Inject
    private StackService stackService;

    @Override
    public String selector() {
        return EventSelectorUtil.selector(RemoveDnsRequest.class);
    }

    @Override
    public void accept(Event<RemoveDnsRequest> event) {
        RemoveDnsRequest request = event.getData();
        try {
            FreeIpa freeIpa = freeIpaService.findByStackId(request.getResourceId());
            String envCrn = stackService.getEnvironmentCrnByStackId(request.getResourceId());
            Pair<Set<String>, Map<String, String>> removeDnsResult =
                    cleanupService.removeDnsEntries(request.getResourceId(), request.getHosts(), request.getIps(), freeIpa.getDomain(), envCrn);
            RemoveDnsResponse response = new RemoveDnsResponse(request, removeDnsResult.getFirst(), removeDnsResult.getSecond());
            eventBus.notify(response.getDnsCleanupFailed().isEmpty()
                            ? EventSelectorUtil.selector(RemoveDnsResponse.class) : EventSelectorUtil.failureSelector(RemoveDnsResponse.class),
                    new Event<>(event.getHeaders(), response));
        } catch (Exception e) {
            LOGGER.error("Removing DNS entries failed for hosts: [{}]", request.getHosts(), e);
            Map<String, String> failureResult = request.getHosts().stream().collect(Collectors.toMap(h -> h, h -> e.getMessage()));
            RemoveDnsResponse response = new RemoveDnsResponse(request, Collections.emptySet(), failureResult);
            eventBus.notify(EventSelectorUtil.failureSelector(RemoveDnsResponse.class),
                    new Event<>(event.getHeaders(), response));
        }
    }
}
