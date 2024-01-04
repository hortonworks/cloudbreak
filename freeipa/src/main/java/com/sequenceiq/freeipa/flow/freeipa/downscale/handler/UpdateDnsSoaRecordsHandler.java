package com.sequenceiq.freeipa.flow.freeipa.downscale.handler;

import static com.sequenceiq.freeipa.flow.freeipa.downscale.DownscaleFlowEvent.DOWNSCALE_UPDATE_DNS_SOA_RECORDS_FAILED_EVENT;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.flow.reactor.api.handler.ExceptionCatcherEventHandler;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;
import com.sequenceiq.freeipa.flow.freeipa.downscale.event.DownscaleFailureEvent;
import com.sequenceiq.freeipa.flow.freeipa.downscale.event.dnssoarecords.UpdateDnsSoaRecordsRequest;
import com.sequenceiq.freeipa.flow.freeipa.downscale.event.dnssoarecords.UpdateDnsSoaRecordsResponse;
import com.sequenceiq.freeipa.service.freeipa.dns.DnsSoaRecordService;

@Component
public class UpdateDnsSoaRecordsHandler extends ExceptionCatcherEventHandler<UpdateDnsSoaRecordsRequest> {

    private static final Logger LOGGER = LoggerFactory.getLogger(UpdateDnsSoaRecordsHandler.class);

    @Inject
    private DnsSoaRecordService dnsSoaRecordService;

    @Override
    public String selector() {
        return EventSelectorUtil.selector(UpdateDnsSoaRecordsRequest.class);
    }

    @Override
    protected Selectable defaultFailureEvent(Long resourceId, Exception e, Event<UpdateDnsSoaRecordsRequest> event) {
        return new DownscaleFailureEvent(DOWNSCALE_UPDATE_DNS_SOA_RECORDS_FAILED_EVENT.selector(),
                resourceId, "Downscale Update DNS SOA Records", Set.of(), Map.of(), e);
    }

    @Override
    protected Selectable doAccept(HandlerEvent<UpdateDnsSoaRecordsRequest> event) {
        UpdateDnsSoaRecordsRequest request = event.getData();
        try {
            Long stackId = request.getResourceId();
            Set<String> fqdns = request.getHosts().stream()
                    .map(hostname -> StringUtils.appendIfMissing(hostname, "."))
                    .collect(Collectors.toSet());
            dnsSoaRecordService.updateSoaRecords(stackId, fqdns);

            return new UpdateDnsSoaRecordsResponse(request.getResourceId());
        } catch (Exception e) {
            LOGGER.error("Downscale updating DNS SOA records failed", e);
            return new DownscaleFailureEvent(DOWNSCALE_UPDATE_DNS_SOA_RECORDS_FAILED_EVENT.event(),
                    request.getResourceId(), "Downscale Update DNS SOA Records", Set.of(), Map.of(), e);
        }
    }
}
