package com.sequenceiq.cloudbreak.core.flow.service;

import com.sequenceiq.cloudbreak.core.CloudbreakException;
import com.sequenceiq.cloudbreak.core.flow.context.FlowContext;

/**
 * Contract for stack provisioning on supported cloud providers.
 */
public interface FlowFacade {

    FlowContext collectMetadata(FlowContext flowContext) throws CloudbreakException;

    FlowContext runClusterContainers(FlowContext context) throws CloudbreakException;

    FlowContext startAmbari(FlowContext flowContext) throws CloudbreakException;

    FlowContext buildAmbariCluster(FlowContext flowContext) throws CloudbreakException;

    FlowContext resetAmbariCluster(FlowContext flowContext) throws CloudbreakException;

    FlowContext handleClusterCreationFailure(FlowContext flowContext) throws CloudbreakException;

    FlowContext handleClusterInstallationFailure(FlowContext flowContext) throws CloudbreakException;

    FlowContext stopStackRequested(FlowContext context) throws CloudbreakException;

    FlowContext handleStackStatusUpdateFailure(FlowContext context) throws CloudbreakException;

    FlowContext updateAllowedSubnets(FlowContext context) throws CloudbreakException;

    FlowContext handleUpdateAllowedSubnetsFailure(FlowContext context) throws CloudbreakException;

    FlowContext startClusterRequested(FlowContext context) throws CloudbreakException;

    FlowContext handleClusterSync(FlowContext context) throws CloudbreakException;

    FlowContext handleStackSync(FlowContext context) throws CloudbreakException;

    FlowContext credentialChange(FlowContext context) throws CloudbreakException;
}