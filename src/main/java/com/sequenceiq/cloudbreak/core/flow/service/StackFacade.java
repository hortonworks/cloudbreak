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

    FlowContext handleTerminationFailure(FlowContext context) throws CloudbreakException;

    FlowContext upscaleStack(FlowContext context) throws CloudbreakException;

    FlowContext downscaleStack(FlowContext context) throws CloudbreakException;

    FlowContext handleScalingFailure(FlowContext context) throws CloudbreakException;

    FlowContext updateAllowedSubnets(FlowContext context) throws CloudbreakException;

    FlowContext handleUpdateAllowedSubnetsFailure(FlowContext context) throws CloudbreakException;
}
