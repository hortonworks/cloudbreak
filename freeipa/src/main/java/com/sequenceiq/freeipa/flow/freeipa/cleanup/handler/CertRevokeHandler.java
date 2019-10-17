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
import com.sequenceiq.freeipa.flow.freeipa.cleanup.event.cert.RevokeCertsRequest;
import com.sequenceiq.freeipa.flow.freeipa.cleanup.event.cert.RevokeCertsResponse;
import com.sequenceiq.freeipa.service.freeipa.cleanup.CleanupService;

import reactor.bus.Event;
import reactor.bus.EventBus;

@Component
public class CertRevokeHandler implements EventHandler<RevokeCertsRequest> {
    private static final Logger LOGGER = LoggerFactory.getLogger(CertRevokeHandler.class);

    @Inject
    private EventBus eventBus;

    @Inject
    private CleanupService cleanupService;

    @Override
    public String selector() {
        return EventSelectorUtil.selector(RevokeCertsRequest.class);
    }

    @Override
    public void accept(Event<RevokeCertsRequest> event) {
        RevokeCertsRequest request = event.getData();
        try {
            Pair<Set<String>, Map<String, String>> revokeCertResult = cleanupService.revokeCerts(request.getResourceId(), request.getHosts());
            RevokeCertsResponse response = new RevokeCertsResponse(request, revokeCertResult.getFirst(), revokeCertResult.getSecond());
            eventBus.notify(response.getCertCleanupFailed().isEmpty()
                            ? EventSelectorUtil.selector(RevokeCertsResponse.class) : EventSelectorUtil.failureSelector(RevokeCertsResponse.class),
                    new Event<>(event.getHeaders(), response));
        } catch (FreeIpaClientException e) {
            LOGGER.error("Revoking of certificates failed for hosts: [{}]", request.getHosts(), e);
            Map<String, String> failureResult = request.getHosts().stream().collect(Collectors.toMap(h -> h, h -> e.getMessage()));
            RevokeCertsResponse response = new RevokeCertsResponse(request, Collections.emptySet(), failureResult);
            eventBus.notify(EventSelectorUtil.failureSelector(RevokeCertsResponse.class),
                    new Event<>(event.getHeaders(), response));
        }
    }
}
