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

    FlowContext handleStartFailure(FlowContext context) throws CloudbreakException;

    FlowContext handleStopFailure(FlowContext context) throws CloudbreakException;

    FlowContext handleClusterCreationFailure(FlowContext context) throws CloudbreakException;

    FlowContext handleSecurityEnableFailure(FlowContext context) throws CloudbreakException;

    FlowContext upscaleCluster(FlowContext context) throws CloudbreakException;

    FlowContext downscaleCluster(FlowContext context) throws CloudbreakException;

    FlowContext handleScalingFailure(FlowContext context) throws CloudbreakException;

    FlowContext resetAmbariCluster(FlowContext context) throws CloudbreakException;

    FlowContext enableSecurity(FlowContext context) throws CloudbreakException;
}
