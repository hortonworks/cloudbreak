package com.sequenceiq.datalake.flow.imdupdate.handler;

import static java.util.concurrent.TimeUnit.MINUTES;
import static java.util.concurrent.TimeUnit.SECONDS;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.datalake.flow.imdupdate.event.InstanceMetadataUpdateWaitRequest;
import com.sequenceiq.datalake.flow.imdupdate.event.SdxInstanceMetadataUpdateFailedEvent;
import com.sequenceiq.datalake.flow.imdupdate.event.SdxInstanceMetadataUpdateWaitSuccessEvent;
import com.sequenceiq.datalake.service.sdx.CloudbreakPoller;
import com.sequenceiq.datalake.service.sdx.PollingConfig;
import com.sequenceiq.datalake.service.sdx.SdxService;
import com.sequenceiq.flow.api.model.FlowIdentifier;
import com.sequenceiq.flow.api.model.FlowType;
import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.flow.reactor.api.handler.ExceptionCatcherEventHandler;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;

@Component
public class SdxInstanceMetadataUpdateWaitHandler extends ExceptionCatcherEventHandler<InstanceMetadataUpdateWaitRequest> {

    private static final Logger LOGGER = LoggerFactory.getLogger(SdxInstanceMetadataUpdateWaitHandler.class);

    @Inject
    private SdxInstanceMetadataUpdateWaitConfiguration waitConfiguration;

    @Inject
    private CloudbreakPoller cloudbreakPoller;

    @Inject
    private SdxService sdxService;

    @Override
    public String selector() {
        return EventSelectorUtil.selector(InstanceMetadataUpdateWaitRequest.class);
    }

    @Override
    protected Selectable defaultFailureEvent(Long resourceId, Exception e, Event<InstanceMetadataUpdateWaitRequest> event) {
        return new SdxInstanceMetadataUpdateFailedEvent(resourceId, event.getData().getUserId(), e, "");
    }

    @Override
    protected Selectable doAccept(HandlerEvent<InstanceMetadataUpdateWaitRequest> event) {
        InstanceMetadataUpdateWaitRequest request = event.getData();
        Long sdxId = request.getResourceId();
        String userId = request.getUserId();
        try {
            FlowIdentifier flowIdentifier = new FlowIdentifier(FlowType.FLOW, sdxService.getById(sdxId).getLastCbFlowId());
            PollingConfig pollingConfig = new PollingConfig(waitConfiguration.getSleepTimeSec(), SECONDS, waitConfiguration.getDurationMin(), MINUTES);
            cloudbreakPoller.pollFlowStateByFlowIdentifierUntilComplete("Instance metadata update", flowIdentifier, sdxId, pollingConfig);
            return new SdxInstanceMetadataUpdateWaitSuccessEvent(sdxId, userId);
        } catch (Exception e) {
            return new SdxInstanceMetadataUpdateFailedEvent(sdxId, userId, e, e.getMessage());
        }
    }

}
