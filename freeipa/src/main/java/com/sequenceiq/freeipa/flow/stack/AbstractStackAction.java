package com.sequenceiq.freeipa.flow.stack;

import static com.sequenceiq.cloudbreak.cloud.model.AvailabilityZone.availabilityZone;
import static com.sequenceiq.cloudbreak.cloud.model.Location.location;
import static com.sequenceiq.cloudbreak.cloud.model.Region.region;

import java.util.List;
import java.util.Map;

import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;

import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.model.Location;
import com.sequenceiq.cloudbreak.common.event.Payload;
import com.sequenceiq.cloudbreak.event.ResourceEvent;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.flow.core.AbstractAction;
import com.sequenceiq.flow.core.CommonContext;
import com.sequenceiq.flow.core.FlowEvent;
import com.sequenceiq.flow.core.FlowState;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.DetailedStackStatus;
import com.sequenceiq.freeipa.api.v1.operation.model.OperationType;
import com.sequenceiq.freeipa.entity.Operation;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.events.EventSenderService;
import com.sequenceiq.freeipa.service.stack.StackUpdater;

public abstract class AbstractStackAction<S extends FlowState, E extends FlowEvent, C extends CommonContext, P extends Payload>
        extends AbstractAction<S, E, C, P> {

    private static final Map<OperationType, ResourceEvent> FAILED_OPERATION_RESOURCE_EVENT_MAP = Map.of(
            OperationType.UPGRADE, ResourceEvent.FREEIPA_UPGRADE_FAILED,
            OperationType.MIGRATE_TO_MULTI_AZ, ResourceEvent.FREEIPA_MULTI_AZ_MIGRATION_FAILED
    );

    private static final Map<OperationType, DetailedStackStatus> FAILED_OPERATION_STACK_STATUS_MAP = Map.of(
            OperationType.UPGRADE, DetailedStackStatus.UPGRADE_FAILED,
            OperationType.MIGRATE_TO_MULTI_AZ, DetailedStackStatus.MULTI_AZ_MIGRATION_FAILED
    );

    @Inject
    private EventSenderService eventService;

    @Inject
    private StackUpdater stackUpdater;

    protected AbstractStackAction(Class<P> payloadClass) {
        super(payloadClass);
    }

    @PostConstruct
    public void init() {
        super.init();
    }

    protected String getErrorReason(Exception payloadException) {
        return payloadException == null || payloadException.getMessage() == null ? "Unknown error" : payloadException.getMessage();
    }

    protected CloudContext buildContext(Stack stack) {
        MDCBuilder.buildMdcContext(stack);
        Location location = location(region(stack.getRegion()), availabilityZone(stack.getAvailabilityZone()));
        return CloudContext.Builder.builder()
                .withId(stack.getId())
                .withName(stack.getName())
                .withCrn(stack.getResourceCrn())
                .withPlatform(stack.getCloudPlatform())
                .withVariant(stack.getPlatformvariant())
                .withLocation(location)
                .withUserName(stack.getOwner())
                .withAccountId(stack.getAccountId())
                .build();
    }

    protected void sendFailedOperationNotificationIfApplicable(Stack stack, String flowTriggerUserCrn, Operation operation, String errorReason) {
        if (operation != null) {
            OperationType operationType = operation.getOperationType();
            ResourceEvent event = FAILED_OPERATION_RESOURCE_EVENT_MAP.get(operationType);
            if (event != null) {
                getEventService().sendEventAndNotification(stack, flowTriggerUserCrn, event, List.of(errorReason));
            }
        }
    }

    protected void updateFailedStackStatusIfApplicable(Stack stack, Operation operation, String statusReason) {
        if (operation != null) {
            DetailedStackStatus status = FAILED_OPERATION_STACK_STATUS_MAP.get(operation.getOperationType());
            if (status != null) {
                stackUpdater.updateStackStatus(stack, status, statusReason);
            }
        }
    }

    public EventSenderService getEventService() {
        return eventService;
    }

    public StackUpdater getStackUpdater() {
        return stackUpdater;
    }
}
