package com.sequenceiq.freeipa.flow.instance.reboot.action;

import static com.sequenceiq.cloudbreak.cloud.model.AvailabilityZone.availabilityZone;
import static com.sequenceiq.cloudbreak.cloud.model.Location.location;
import static com.sequenceiq.cloudbreak.cloud.model.Region.region;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.model.Location;
import com.sequenceiq.cloudbreak.common.event.Payload;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.flow.instance.reboot.RebootContext;
import com.sequenceiq.freeipa.flow.instance.reboot.RebootEvent;
import com.sequenceiq.freeipa.flow.instance.reboot.RebootState;
import com.sequenceiq.freeipa.flow.stack.AbstractStackAction;

public abstract class AbstractRebootAction<P extends Payload>
        extends AbstractStackAction<RebootState, RebootEvent, RebootContext, P> {

    protected static final String OPERATION_ID = "OPERATION_ID";

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractRebootAction.class);

    protected AbstractRebootAction(Class<P> payloadClass) {
        super(payloadClass);
    }

    protected void setOperationId(Map<Object, Object> variables, String operationId) {
        variables.put(OPERATION_ID, operationId);
        addMdcOperationId(variables);
    }

    protected String getOperationId(Map<Object, Object> variables) {
        return (String) variables.get(OPERATION_ID);
    }

    protected void addMdcOperationId(Map<Object, Object> varialbes) {
        String operationId = getOperationId(varialbes);
        MDCBuilder.addOperationId(operationId);
    }

    protected CloudContext getCloudContext(Stack stack) {
        Location location = location(region(stack.getRegion()), availabilityZone(stack.getAvailabilityZone()));
        return CloudContext.Builder.builder()
                .withId(stack.getId())
                .withName(stack.getName())
                .withCrn(stack.getResourceCrn())
                .withPlatform(stack.getCloudPlatform())
                .withVariant(stack.getCloudPlatform())
                .withLocation(location)
                .withUserName(stack.getOwner())
                .withAccountId(stack.getAccountId())
                .build();
    }
}
