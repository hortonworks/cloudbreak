package com.sequenceiq.cloudbreak.core.flow.service;

import com.sequenceiq.cloudbreak.core.CloudbreakException;
import com.sequenceiq.cloudbreak.core.flow.context.FlowContext;

public interface StackFacade {

    FlowContext bootstrapCluster(FlowContext context) throws CloudbreakException;

    FlowContext setupConsulMetadata(FlowContext context) throws CloudbreakException;

    FlowContext start(FlowContext context) throws CloudbreakException;

    FlowContext stop(FlowContext context) throws CloudbreakException;

    FlowContext terminateStack(FlowContext flowContext) throws CloudbreakException;

    FlowContext handleCreationFailure(FlowContext context) throws CloudbreakException;

    FlowContext handleStatusUpdateFailure(FlowContext context) throws CloudbreakException;

    FlowContext handleTerminationFailure(FlowContext context) throws CloudbreakException;

    FlowContext addInstances(FlowContext context) throws CloudbreakException;

    FlowContext extendMetadata(FlowContext context) throws CloudbreakException;

    FlowContext bootstrapNewNodes(FlowContext context) throws CloudbreakException;

    FlowContext extendConsulMetadata(FlowContext context) throws CloudbreakException;

    FlowContext downscaleStack(FlowContext context) throws CloudbreakException;

    FlowContext handleScalingFailure(FlowContext context) throws CloudbreakException;

    FlowContext updateAllowedSubnets(FlowContext context) throws CloudbreakException;

    FlowContext handleUpdateAllowedSubnetsFailure(FlowContext context) throws CloudbreakException;

    FlowContext stopRequested(FlowContext context) throws CloudbreakException;

    FlowContext provision(FlowContext context) throws CloudbreakException;

    FlowContext sync(FlowContext context) throws CloudbreakException;
}
