package com.sequenceiq.cloudbreak.core.flow.service;

import com.sequenceiq.cloudbreak.core.CloudbreakException;
import com.sequenceiq.cloudbreak.core.flow.context.FlowContext;

/**
 * Contract for stack provisioning on supported cloud providers.
 */
public interface FlowFacade {

    FlowContext collectMetadata(FlowContext flowContext) throws CloudbreakException;

    FlowContext bootstrapCluster(FlowContext context) throws CloudbreakException;

    FlowContext setupConsulMetadata(FlowContext flowContext) throws CloudbreakException;

    FlowContext runClusterContainers(FlowContext context) throws CloudbreakException;

    FlowContext startAmbari(FlowContext flowContext) throws CloudbreakException;

    FlowContext buildAmbariCluster(FlowContext flowContext) throws CloudbreakException;

    FlowContext resetAmbariCluster(FlowContext flowContext) throws CloudbreakException;

    FlowContext handleStackCreationFailure(FlowContext context) throws CloudbreakException;

    FlowContext handleClusterCreationFailure(FlowContext flowContext) throws CloudbreakException;

    FlowContext handleClusterInstallationFailure(FlowContext flowContext) throws CloudbreakException;

    FlowContext stopStackRequested(FlowContext context) throws CloudbreakException;

    FlowContext handleStackStatusUpdateFailure(FlowContext context) throws CloudbreakException;

    FlowContext startCluster(FlowContext flowContext) throws CloudbreakException;

    FlowContext handleClusterStartFailure(FlowContext flowContext) throws CloudbreakException;

    FlowContext stopCluster(FlowContext flowContext) throws CloudbreakException;

    FlowContext handleClusterStopFailure(FlowContext flowContext) throws CloudbreakException;

    FlowContext addInstances(FlowContext context) throws CloudbreakException;

    FlowContext extendMetadata(FlowContext context) throws CloudbreakException;

    FlowContext bootstrapNewNodes(FlowContext context) throws CloudbreakException;

    FlowContext extendConsulMetadata(FlowContext context) throws CloudbreakException;

    FlowContext addClusterContainers(FlowContext context) throws CloudbreakException;

    FlowContext downscaleStack(FlowContext context) throws CloudbreakException;

    FlowContext handleStackScalingFailure(FlowContext context) throws CloudbreakException;

    FlowContext upscaleCluster(FlowContext context) throws CloudbreakException;

    FlowContext downscaleCluster(FlowContext context) throws CloudbreakException;

    FlowContext handleClusterScalingFailure(FlowContext context) throws CloudbreakException;

    FlowContext updateAllowedSubnets(FlowContext context) throws CloudbreakException;

    FlowContext handleUpdateAllowedSubnetsFailure(FlowContext context) throws CloudbreakException;

    FlowContext startClusterRequested(FlowContext context) throws CloudbreakException;

    FlowContext handleClusterSync(FlowContext context) throws CloudbreakException;

    FlowContext handleStackSync(FlowContext context) throws CloudbreakException;

    FlowContext credentialChange(FlowContext context) throws CloudbreakException;
}