package com.sequenceiq.freeipa.flow.stack;

import static com.sequenceiq.cloudbreak.cloud.model.AvailabilityZone.availabilityZone;
import static com.sequenceiq.cloudbreak.cloud.model.Location.location;
import static com.sequenceiq.cloudbreak.cloud.model.Region.region;

import jakarta.annotation.PostConstruct;

import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.model.Location;
import com.sequenceiq.cloudbreak.common.event.Payload;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.flow.core.AbstractAction;
import com.sequenceiq.flow.core.CommonContext;
import com.sequenceiq.flow.core.FlowEvent;
import com.sequenceiq.flow.core.FlowState;
import com.sequenceiq.freeipa.entity.Stack;

public abstract class AbstractStackAction<S extends FlowState, E extends FlowEvent, C extends CommonContext, P extends Payload>
        extends AbstractAction<S, E, C, P> {

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
        CloudContext cloudContext = CloudContext.Builder.builder()
                .withId(stack.getId())
                .withName(stack.getName())
                .withCrn(stack.getResourceCrn())
                .withPlatform(stack.getCloudPlatform())
                .withVariant(stack.getPlatformvariant())
                .withLocation(location)
                .withUserName(stack.getOwner())
                .withAccountId(stack.getAccountId())
                .build();
        return cloudContext;
    }
}
