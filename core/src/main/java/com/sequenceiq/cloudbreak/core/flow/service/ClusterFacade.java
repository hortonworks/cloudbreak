package com.sequenceiq.cloudbreak.core.flow.service;

import com.sequenceiq.cloudbreak.core.CloudbreakException;
import com.sequenceiq.cloudbreak.core.flow.context.FlowContext;

/**
 * Contract for Flow related ambari operations.
 */
public interface ClusterFacade {
    FlowContext handleClusterInstallationFailure(FlowContext context) throws CloudbreakException;

    FlowContext resetAmbariCluster(FlowContext context) throws CloudbreakException;

    FlowContext credentialChange(FlowContext context) throws CloudbreakException;
}
