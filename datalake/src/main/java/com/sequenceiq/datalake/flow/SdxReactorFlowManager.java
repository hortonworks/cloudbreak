package com.sequenceiq.datalake.flow;

import static com.sequenceiq.datalake.flow.create.SdxCreateEvent.ENV_WAIT_EVENT;
import static com.sequenceiq.datalake.flow.delete.SdxDeleteEvent.SDX_DELETE_EVENT;
import static com.sequenceiq.datalake.flow.repair.SdxRepairEvent.SDX_REPAIR_EVENT;

import java.util.Map;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.common.event.Acceptable;
import com.sequenceiq.cloudbreak.exception.CloudbreakApiException;
import com.sequenceiq.cloudbreak.exception.FlowsAlreadyRunningException;
import com.sequenceiq.datalake.entity.SdxCluster;
import com.sequenceiq.datalake.flow.repair.event.SdxRepairStartEvent;
import com.sequenceiq.datalake.logger.ThreadBasedRequestIdProvider;
import com.sequenceiq.datalake.service.sdx.SdxService;
import com.sequenceiq.flow.core.Flow2Handler;
import com.sequenceiq.flow.core.FlowConstants;
import com.sequenceiq.flow.reactor.ErrorHandlerAwareReactorEventFactory;
import com.sequenceiq.sdx.api.model.SdxRepairRequest;

import reactor.bus.Event;
import reactor.bus.EventBus;

@Service
public class SdxReactorFlowManager {

    private static final long WAIT_FOR_ACCEPT = 10L;

    @Inject
    private EventBus reactor;

    @Inject
    private ErrorHandlerAwareReactorEventFactory eventFactory;

    @Inject
    private SdxService sdxService;

    @Inject
    private ThreadBasedUserCrnProvider threadBasedUserCrnProvider;

    @Inject
    private ThreadBasedRequestIdProvider threadBasedRequestIdProvider;

    public void triggerSdxCreation(Long sdxId) {
        String selector = ENV_WAIT_EVENT.event();
        String userId = threadBasedUserCrnProvider.getUserCrn();
        String requestId = threadBasedRequestIdProvider.getRequestId();
        notify(selector, new SdxEvent(selector, sdxId, userId, requestId));
    }

    public void triggerSdxDeletion(Long sdxId) {
        String selector = SDX_DELETE_EVENT.event();
        String userId = threadBasedUserCrnProvider.getUserCrn();
        String requestId = threadBasedRequestIdProvider.getRequestId();
        notify(selector, new SdxEvent(selector, sdxId, userId, requestId));
    }

    public void triggerSdxRepairFlow(Long sdxId, SdxRepairRequest repairRequest) {
        String selector = SDX_REPAIR_EVENT.event();
        String userId = threadBasedUserCrnProvider.getUserCrn();
        String requestId = threadBasedRequestIdProvider.getRequestId();
        notify(selector, new SdxRepairStartEvent(selector, sdxId, userId, requestId, repairRequest));
    }

    public void cancelRunningFlows(Long sdxId) {
        String userId = threadBasedUserCrnProvider.getUserCrn();
        String requestId = threadBasedRequestIdProvider.getRequestId();
        SdxEvent cancelEvent = new SdxEvent(Flow2Handler.FLOW_CANCEL, sdxId, userId, requestId);
        reactor.notify(Flow2Handler.FLOW_CANCEL, eventFactory.createEventWithErrHandler(cancelEvent));
    }

    private void notify(String selector, SdxEvent acceptable) {
        Map<String, Object> flowTriggerUserCrnHeader = Map.of(FlowConstants.FLOW_TRIGGER_USERCRN, acceptable.getUserId());
        Event<Acceptable> event = eventFactory.createEventWithErrHandler(flowTriggerUserCrnHeader, acceptable);

        SdxCluster sdxCluster = sdxService.getById(event.getData().getResourceId());

        reactor.notify(selector, event);
        try {
            Boolean accepted = true;
            if (event.getData().accepted() != null) {
                accepted = event.getData().accepted().await(WAIT_FOR_ACCEPT, TimeUnit.SECONDS);
            }
            if (accepted == null || !accepted) {
                throw new FlowsAlreadyRunningException(String.format("Sdx cluster %s has flows under operation, request not allowed.", sdxCluster.getId()));
            }
        } catch (InterruptedException e) {
            throw new CloudbreakApiException(e.getMessage());
        }

    }
}
