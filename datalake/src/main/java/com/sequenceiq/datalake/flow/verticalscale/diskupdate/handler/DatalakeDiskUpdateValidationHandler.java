package com.sequenceiq.datalake.flow.verticalscale.diskupdate.handler;

import static com.sequenceiq.datalake.flow.verticalscale.diskupdate.event.DatalakeDiskUpdateStateSelectors.DATALAKE_DISK_UPDATE_VALIDATION_HANDLER_EVENT;

import java.util.List;
import java.util.Set;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.StackV4Endpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackV4Response;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.cloudbreak.eventbus.Promise;
import com.sequenceiq.datalake.entity.DatalakeStatusEnum;
import com.sequenceiq.datalake.flow.verticalscale.diskupdate.event.DatalakeDiskUpdateEvent;
import com.sequenceiq.datalake.flow.verticalscale.diskupdate.event.DatalakeDiskUpdateFailedEvent;
import com.sequenceiq.datalake.flow.verticalscale.diskupdate.event.DatalakeDiskUpdateStateSelectors;
import com.sequenceiq.flow.reactor.api.event.EventSender;
import com.sequenceiq.flow.reactor.api.handler.EventSenderAwareHandler;

@Component
public class DatalakeDiskUpdateValidationHandler extends EventSenderAwareHandler<DatalakeDiskUpdateEvent> {

    private static final Logger LOGGER = LoggerFactory.getLogger(DatalakeDiskUpdateValidationHandler.class);

    private static final long WORKSPACE_ID = 0L;

    @Inject
    private StackV4Endpoint stackV4Endpoint;

    public DatalakeDiskUpdateValidationHandler(EventSender eventSender) {
        super(eventSender);
    }

    @Override
    public String selector() {
        return DATALAKE_DISK_UPDATE_VALIDATION_HANDLER_EVENT.selector();
    }

    @Override
    public void accept(Event<DatalakeDiskUpdateEvent> datalakeDiskUpdateEvent) {
        LOGGER.debug("In DatalakeDiskUpdateValidationHandler.accept");
        DatalakeDiskUpdateEvent payload = datalakeDiskUpdateEvent.getData();
        try {
            StackV4Response stackV4Response = ThreadBasedUserCrnProvider.doAsInternalActor(
                    () -> stackV4Endpoint.getWithResources(WORKSPACE_ID, payload.getClusterName(), Set.of(), payload.getAccountId()));
            // Validation runs synchronously in the core API path now (CB-32862); this state is kept only as the flow's init
            // state and forwards the request unchanged. The volumes to update are recomputed from the stack in core's
            // datalakeUpdateDisks, so forward an empty (never null) list - DiskModificationRequest#volumesToUpdate is @NotNull.
            DatalakeDiskUpdateEvent diskUpdateEvent = DatalakeDiskUpdateEvent.builder()
                    .withAccepted(new Promise<>())
                    .withDatalakeDiskUpdateEvent(payload)
                    .withStackV4Response(stackV4Response)
                    .withSelector(DatalakeDiskUpdateStateSelectors.DATALAKE_DISK_UPDATE_EVENT.selector())
                    .withVolumesToBeUpdated(List.of())
                    .build();
            eventSender().sendEvent(diskUpdateEvent, datalakeDiskUpdateEvent.getHeaders());
        } catch (Exception e) {
            DatalakeDiskUpdateFailedEvent failedEvent = DatalakeDiskUpdateFailedEvent.builder()
                    .withDatalakeDiskUpdateEvent(payload)
                    .withException(e)
                    .withDatalakeStatus(DatalakeStatusEnum.DATALAKE_DISK_UPDATE_VALIDATION_FAILED).build();
            LOGGER.error("Exception while forwarding datalake disk update: {} on stack {}", e.getMessage(), payload.getStackCrn(), e);
            eventSender().sendEvent(failedEvent, datalakeDiskUpdateEvent.getHeaders());
        }
    }
}
