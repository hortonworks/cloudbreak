package com.sequenceiq.freeipa.flow.instance.reboot.action;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.cloudbreak.common.event.Payload;
import com.sequenceiq.freeipa.flow.instance.reboot.RebootContext;
import com.sequenceiq.freeipa.flow.instance.reboot.RebootEvent;
import com.sequenceiq.freeipa.flow.instance.reboot.RebootState;
import com.sequenceiq.freeipa.flow.stack.AbstractStackAction;

public abstract class AbstractRebootAction<P extends Payload>
        extends AbstractStackAction<RebootState, RebootEvent, RebootContext, P>  {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractRebootAction.class);

    protected AbstractRebootAction(Class<P> payloadClass) {
        super(payloadClass);
    }
}
