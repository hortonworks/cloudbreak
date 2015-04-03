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

    FlowContext finalizeMetadata(FlowContext flowContext) throws CloudbreakException;

    FlowContext startAmbari(FlowContext flowContext) throws CloudbreakException;

    FlowContext buildAmbariCluster(FlowContext flowContext) throws CloudbreakException;

    FlowContext resetAmbariCluster(FlowContext flowContext) throws CloudbreakException;

    FlowContext handleStackCreationFailure(FlowContext context) throws CloudbreakException;

    FlowContext handleClusterCreationFailure(FlowContext flowContext) throws CloudbreakException;

    FlowContext handleSecurityEnableFailure(FlowContext flowContext) throws CloudbreakException;

    FlowContext startStack(FlowContext context) throws CloudbreakException;

    FlowContext stopStack(FlowContext context) throws CloudbreakException;

    FlowContext handleStackStatusUpdateFailure(FlowContext context) throws CloudbreakException;

    FlowContext startCluster(FlowContext flowContext) throws CloudbreakException;

    FlowContext handleClusterStartFailure(FlowContext flowContext) throws CloudbreakException;

    FlowContext stopCluster(FlowContext flowContext) throws CloudbreakException;

    FlowContext handleClusterStopFailure(FlowContext flowContext) throws CloudbreakException;

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

    FlowContext enableSecurity(FlowContext context) throws CloudbreakException;

    FlowContext setupCluster(FlowContext context) throws CloudbreakException;

    FlowContext upscaleClusterPrepare(FlowContext context) throws CloudbreakException;

    FlowContext upscaleClusterNodes(FlowContext context) throws CloudbreakException;
}
