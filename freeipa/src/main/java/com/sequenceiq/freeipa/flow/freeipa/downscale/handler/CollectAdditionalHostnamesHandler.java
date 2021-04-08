package com.sequenceiq.freeipa.flow.freeipa.downscale.handler;

import static com.sequenceiq.freeipa.flow.freeipa.downscale.DownscaleFlowEvent.DOWNSCALE_COLLECT_ADDITIONAL_HOSTNAMES_FAILED_EVENT;

import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.flow.reactor.api.handler.ExceptionCatcherEventHandler;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;
import com.sequenceiq.freeipa.client.FreeIpaClient;
import com.sequenceiq.freeipa.client.FreeIpaClientException;
import com.sequenceiq.freeipa.client.model.IpaServer;
import com.sequenceiq.freeipa.flow.freeipa.downscale.event.DownscaleFailureEvent;
import com.sequenceiq.freeipa.flow.freeipa.downscale.event.collecthostnames.CollectAdditionalHostnamesRequest;
import com.sequenceiq.freeipa.flow.freeipa.downscale.event.collecthostnames.CollectAdditionalHostnamesResponse;
import com.sequenceiq.freeipa.service.freeipa.FreeIpaClientFactory;

import reactor.bus.Event;

@Component
public class CollectAdditionalHostnamesHandler extends ExceptionCatcherEventHandler<CollectAdditionalHostnamesRequest> {
    private static final Logger LOGGER = LoggerFactory.getLogger(CollectAdditionalHostnamesHandler.class);

    @Inject
    private FreeIpaClientFactory freeIpaClientFactory;

    @Override
    public String selector() {
        return EventSelectorUtil.selector(CollectAdditionalHostnamesRequest.class);
    }

    @Override
    protected Selectable defaultFailureEvent(Long resourceId, Exception e, Event<CollectAdditionalHostnamesRequest> event) {
        return new DownscaleFailureEvent(DOWNSCALE_COLLECT_ADDITIONAL_HOSTNAMES_FAILED_EVENT.event(),
                resourceId, "Downscale Collect Additional Hostnames", Set.of(), Map.of(), e);
    }

    @Override
    protected Selectable doAccept(HandlerEvent<CollectAdditionalHostnamesRequest> event) {
        CollectAdditionalHostnamesRequest request = event.getData();
        Selectable result;
        try {
            Long stackId = request.getResourceId();
            Set<String> fqdns = getHostnamesFromFreeIpaServers(stackId);

            result = new CollectAdditionalHostnamesResponse(request.getResourceId(), fqdns);
        } catch (Exception e) {
            LOGGER.error("Collecting additional hostnames failed", e);
            result = new DownscaleFailureEvent(DOWNSCALE_COLLECT_ADDITIONAL_HOSTNAMES_FAILED_EVENT.event(),
                    request.getResourceId(), "Downscale Collect Additional Hostnames", Set.of(), Map.of(), e);
        }
        return result;
    }

    private Set<String> getHostnamesFromFreeIpaServers(Long stackId) throws FreeIpaClientException {
        FreeIpaClient freeIpaClient = freeIpaClientFactory.getFreeIpaClientForStackId(stackId);
        Set<String> fqdns = freeIpaClient.findAllServers().stream()
                .map(IpaServer::getFqdn)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        LOGGER.debug("Found [{}] hostnames from registered FreeIPA servers", fqdns);
        return fqdns;
    }

}
