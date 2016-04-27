package com.sequenceiq.cloudbreak.core.flow.service;

import com.sequenceiq.cloudbreak.core.CloudbreakException;
import com.sequenceiq.cloudbreak.core.flow.context.FlowContext;

public interface StackFacade {

    FlowContext bootstrapCluster(FlowContext context) throws CloudbreakException;

    FlowContext setupConsulMetadata(FlowContext context) throws CloudbreakException;

    FlowContext handleCreationFailure(FlowContext context) throws CloudbreakException;

    FlowContext handleStatusUpdateFailure(FlowContext context) throws CloudbreakException;

    FlowContext updateAllowedSubnets(FlowContext context) throws CloudbreakException;

    FlowContext handleUpdateAllowedSubnetsFailure(FlowContext context) throws CloudbreakException;

    FlowContext stopRequested(FlowContext context) throws CloudbreakException;

    FlowContext sync(FlowContext context) throws CloudbreakException;

}
