package com.sequenceiq.cloudbreak.core.flow.service;

import com.sequenceiq.cloudbreak.core.flow.context.FlowContext;

/**
 * Contract for Flow related ambari operations.
 */
public interface AmbariFlowFacade {

    FlowContext allocateAmbariRoles(FlowContext context) throws Exception;

    FlowContext startAmbari(FlowContext context) throws Exception;

    FlowContext buildAmbariCluster(FlowContext context) throws Exception;

}
