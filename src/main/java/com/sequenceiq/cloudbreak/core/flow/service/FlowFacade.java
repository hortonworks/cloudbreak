package com.sequenceiq.cloudbreak.core.flow.service;

import com.sequenceiq.cloudbreak.core.CloudbreakException;
import com.sequenceiq.cloudbreak.core.flow.context.FlowContext;

/**
 * Contract for stack provisioning on supported cloud providers.
 */
public interface FlowFacade {

    FlowContext setup(FlowContext flowContext) throws CloudbreakException;

    FlowContext provision(FlowContext flowContext) throws CloudbreakException;

    FlowContext setupMetadata(FlowContext flowContext) throws CloudbreakException;

    FlowContext allocateAmbariRoles(FlowContext flowContext) throws CloudbreakException;

    FlowContext startAmbari(FlowContext flowContext) throws CloudbreakException;

    FlowContext buildAmbariCluster(FlowContext flowContext) throws CloudbreakException;

}
