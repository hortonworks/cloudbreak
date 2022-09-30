package com.sequenceiq.datalake.flow.dr.datalakeinfo.handler;

import static com.sequenceiq.datalake.flow.dr.datalakeinfo.SubmitDatalakeDataInfoEvent.SUBMIT_DATALAKE_DATA_INFO_SUCCESS_EVENT;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.datalake.flow.SdxEvent;
import com.sequenceiq.datalake.flow.dr.datalakeinfo.event.SubmitDatalakeDataInfoFailedEvent;
import com.sequenceiq.datalake.flow.dr.datalakeinfo.event.SubmitDatalakeDataInfoRequest;
import com.sequenceiq.datalake.service.sdx.dr.SdxBackupRestoreService;
import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.flow.reactor.api.handler.ExceptionCatcherEventHandler;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;

@Component
public class SubmitDatalakeDataInfoHandler extends ExceptionCatcherEventHandler<SubmitDatalakeDataInfoRequest> {
    @Inject
    private SdxBackupRestoreService sdxBackupRestoreService;

    @Override
    public String selector() {
        return EventSelectorUtil.selector(SubmitDatalakeDataInfoRequest.class);
    }

    @Override
    protected Selectable defaultFailureEvent(Long resourceId, Exception e, Event<SubmitDatalakeDataInfoRequest> event) {
        return new SubmitDatalakeDataInfoFailedEvent(resourceId, null, e);
    }

    @Override
    protected Selectable doAccept(HandlerEvent<SubmitDatalakeDataInfoRequest> event) {
        SubmitDatalakeDataInfoRequest request = event.getData();
        Selectable response;
        try {
            sdxBackupRestoreService.submitDatalakeDataInfo(request.getOperationId(), request.getDataInfoJSON(), request.getUserId());
            response = new SdxEvent(SUBMIT_DATALAKE_DATA_INFO_SUCCESS_EVENT.event(), request.getResourceId(), request.getUserId());
        } catch (Exception ex) {
            response = new SubmitDatalakeDataInfoFailedEvent(request.getResourceId(), request.getUserId(), ex);
        }
        return response;
    }
}
