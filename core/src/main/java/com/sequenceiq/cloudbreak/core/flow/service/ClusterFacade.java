package com.sequenceiq.cloudbreak.core.flow.service;

import com.sequenceiq.cloudbreak.core.CloudbreakException;
import com.sequenceiq.cloudbreak.core.flow.context.FlowContext;

/**
 * Contract for Flow related ambari operations.
 */
public interface ClusterFacade {

    FlowContext runClusterContainers(FlowContext context) throws CloudbreakException;

    FlowContext startAmbari(FlowContext context) throws Exception;

    FlowContext buildAmbariCluster(FlowContext context) throws Exception;

    FlowContext startCluster(FlowContext context) throws CloudbreakException;

    FlowContext stopCluster(FlowContext context) throws CloudbreakException;

    FlowContext handleStartFailure(FlowContext context) throws CloudbreakException;

    FlowContext handleStopFailure(FlowContext context) throws CloudbreakException;

    FlowContext handleClusterCreationFailure(FlowContext context) throws CloudbreakException;

    FlowContext handleClusterInstallationFailure(FlowContext context) throws CloudbreakException;

    FlowContext resetAmbariCluster(FlowContext context) throws CloudbreakException;

    FlowContext startRequested(FlowContext context) throws CloudbreakException;

    FlowContext sync(FlowContext context) throws CloudbreakException;

    FlowContext credentialChange(FlowContext context) throws CloudbreakException;
}
