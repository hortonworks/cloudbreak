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

    FlowContext clusterCreationFailed(FlowContext flowContext) throws CloudbreakException;

    FlowContext startCluster(FlowContext flowContext) throws CloudbreakException;

    FlowContext clusterStartError(FlowContext flowContext) throws CloudbreakException;

    FlowContext stopCluster(FlowContext flowContext) throws CloudbreakException;

    FlowContext clusterStopError(FlowContext flowContext) throws CloudbreakException;

    FlowContext upscaleStack(FlowContext context) throws CloudbreakException;

    FlowContext downscaleStack(FlowContext context) throws CloudbreakException;

    FlowContext handleStackScalingFailure(FlowContext context) throws CloudbreakException;

    FlowContext upscaleCluster(FlowContext context) throws CloudbreakException;

    FlowContext downscaleCluster(FlowContext context) throws CloudbreakException;

    FlowContext terminateStack(FlowContext flowContext) throws CloudbreakException;

    FlowContext handleStackTerminationFailure(FlowContext context) throws CloudbreakException;

    FlowContext handleClusterScalingFailure(FlowContext context) throws CloudbreakException;

    FlowContext updateAllowedSubnets(FlowContext context) throws CloudbreakException;

    FlowContext handleUpdateAllowedSubnetsFailure(FlowContext context) throws CloudbreakException;
}
