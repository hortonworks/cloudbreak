package com.sequenceiq.cloudbreak.core.flow.service;

import com.sequenceiq.cloudbreak.core.CloudbreakException;
import com.sequenceiq.cloudbreak.core.flow.context.FlowContext;

public interface StackFacade {

    FlowContext start(FlowContext context) throws CloudbreakException;

    FlowContext stop(FlowContext context) throws CloudbreakException;

    FlowContext terminateStack(FlowContext flowContext) throws CloudbreakException;

    FlowContext stackCreationError(FlowContext context) throws CloudbreakException;

    FlowContext stackStartError(FlowContext context) throws CloudbreakException;

    FlowContext stackStopError(FlowContext context) throws CloudbreakException;

    FlowContext stackTerminationError(FlowContext context) throws CloudbreakException;
}
