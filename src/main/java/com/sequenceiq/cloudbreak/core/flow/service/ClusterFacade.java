package com.sequenceiq.cloudbreak.core.flow.service;

import com.sequenceiq.cloudbreak.core.CloudbreakException;
import com.sequenceiq.cloudbreak.core.flow.context.FlowContext;

/**
 * Contract for Flow related ambari operations.
 */
public interface ClusterFacade {

    FlowContext allocateAmbariRoles(FlowContext context) throws Exception;

    FlowContext startAmbari(FlowContext context) throws Exception;

    FlowContext buildAmbariCluster(FlowContext context) throws Exception;

    FlowContext startCluster(FlowContext context) throws CloudbreakException;

    FlowContext stopCluster(FlowContext context) throws CloudbreakException;

    FlowContext clusterStartError(FlowContext context) throws CloudbreakException;

    FlowContext clusterStopError(FlowContext context) throws CloudbreakException;

}
