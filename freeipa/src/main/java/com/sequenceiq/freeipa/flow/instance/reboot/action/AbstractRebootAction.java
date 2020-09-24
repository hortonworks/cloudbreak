package com.sequenceiq.freeipa.flow.instance.reboot.action;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.cloudbreak.common.event.Payload;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
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
}
