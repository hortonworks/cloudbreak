package com.sequenceiq.freeipa.flow.freeipa.downscale.handler;

import static com.sequenceiq.freeipa.flow.freeipa.downscale.DownscaleFlowEvent.DOWNSCALE_UPDATE_DNS_SOA_RECORDS_FAILED_EVENT;

import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.flow.reactor.api.handler.ExceptionCatcherEventHandler;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;
import com.sequenceiq.freeipa.client.FreeIpaClient;
import com.sequenceiq.freeipa.client.FreeIpaClientException;
import com.sequenceiq.freeipa.client.model.DnsZone;
import com.sequenceiq.freeipa.client.model.IpaServer;
import com.sequenceiq.freeipa.flow.freeipa.downscale.event.DownscaleFailureEvent;
import com.sequenceiq.freeipa.flow.freeipa.downscale.event.dnssoarecords.UpdateDnsSoaRecordsRequest;
import com.sequenceiq.freeipa.flow.freeipa.downscale.event.dnssoarecords.UpdateDnsSoaRecordsResponse;
import com.sequenceiq.freeipa.service.freeipa.FreeIpaClientFactory;

import reactor.bus.Event;

@Component
public class UpdateDnsSoaRecordsHandler extends ExceptionCatcherEventHandler<UpdateDnsSoaRecordsRequest> {

    private static final Logger LOGGER = LoggerFactory.getLogger(UpdateDnsSoaRecordsHandler.class);

    @Inject
    private FreeIpaClientFactory freeIpaClientFactory;

    @Override
    public String selector() {
        return EventSelectorUtil.selector(UpdateDnsSoaRecordsRequest.class);
    }

    @Override
    protected Selectable defaultFailureEvent(Long resourceId, Exception e, Event<UpdateDnsSoaRecordsRequest> event) {
        return new DownscaleFailureEvent(DOWNSCALE_UPDATE_DNS_SOA_RECORDS_FAILED_EVENT.selector(),
                resourceId, "Downscale Update DNS SOA Records", Set.of(), Map.of(), e);    }

    @Override
    protected Selectable doAccept(HandlerEvent<UpdateDnsSoaRecordsRequest> event) {
        UpdateDnsSoaRecordsRequest request = event.getData();
        Selectable result;
        try {
            Long stackId = request.getResourceId();
            Set<String> fqdns = request.getHosts().stream()
                    .map(hostname -> StringUtils.appendIfMissing(hostname, "."))
                    .collect(Collectors.toSet());
            updateSoaRecords(stackId, fqdns);

            result = new UpdateDnsSoaRecordsResponse(request.getResourceId());
        } catch (Exception e) {
            LOGGER.error("Downscale updating DNS SOA records failed", e);
            result = new DownscaleFailureEvent(DOWNSCALE_UPDATE_DNS_SOA_RECORDS_FAILED_EVENT.event(),
                    request.getResourceId(), "Downscale Update DNS SOA Records", Set.of(), Map.of(), e);
        }
        return result;
    }

    private void updateSoaRecords(Long stackId, Set<String> fqdnsToUpdate) throws FreeIpaClientException {
        FreeIpaClient freeIpaClient = freeIpaClientFactory.getFreeIpaClientForStackId(stackId);
        String newFqdn = freeIpaClient.findAllServers().stream()
                .map(IpaServer::getFqdn)
                .filter(Objects::nonNull)
                .map(fqdn -> StringUtils.appendIfMissing(fqdn, "."))
                .filter(fqdn -> !fqdnsToUpdate.contains(fqdn))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("A alternate FreeIPA server was not found for the SOA records"));
        Set<DnsZone> dnsZonesToUpdate = freeIpaClient.findAllDnsZone().stream()
                .filter(dnsZone -> Objects.nonNull(dnsZone.getIdnssoamname()))
                .filter(dnsZone -> fqdnsToUpdate.contains(dnsZone.getIdnssoamname()))
                .collect(Collectors.toSet());
        for (DnsZone dnsZone : dnsZonesToUpdate) {
            freeIpaClient.setDnsZoneAuthoritativeNameserver(dnsZone.getIdnsname(), newFqdn);
        }
    }
}
