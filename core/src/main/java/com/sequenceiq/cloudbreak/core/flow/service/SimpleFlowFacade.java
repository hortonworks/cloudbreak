package com.sequenceiq.cloudbreak.core.flow.service;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.core.CloudbreakException;
import com.sequenceiq.cloudbreak.core.flow.context.FlowContext;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.cloudbreak.service.stack.flow.MetadataSetupService;

@Service
public class SimpleFlowFacade implements FlowFacade {
    private static final Logger LOGGER = LoggerFactory.getLogger(SimpleFlowFacade.class);

    @Inject
    private ClusterFacade clusterFacade;

    @Inject
    private StackFacade stackFacade;

    @Inject
    private MetadataSetupService metadataSetupService;

    @Inject
    private StackService stackService;

    @Override
    public FlowContext collectMetadata(FlowContext context) throws CloudbreakException {
        LOGGER.debug("Metadata collect. Context: {}", context);
        try {
            Long stackId = context.getStackId();
            Stack stack = stackService.getById(stackId);
            MDCBuilder.buildMdcContext(stack);
            metadataSetupService.collectMetadata(stack);
            LOGGER.debug("Metadata collect DONE.");
            return context;
        } catch (Exception e) {
            LOGGER.error("Exception during metadata collect: {}", e.getMessage());
            throw new CloudbreakException(e);
        }
    }

    @Override
    public FlowContext runClusterContainers(FlowContext context) throws CloudbreakException {
        LOGGER.debug("Running cluster containers. Context: {}", context);
        try {
            return clusterFacade.runClusterContainers(context);
        } catch (Exception e) {
            LOGGER.error("Exception while setting up cluster containers.", e.getMessage());
            throw new CloudbreakException(e);
        }
    }

    @Override
    public FlowContext startAmbari(FlowContext context) throws CloudbreakException {
        LOGGER.debug("Starting Ambari. Context: {}", context);
        try {
            return clusterFacade.startAmbari(context);
        } catch (Exception e) {
            LOGGER.error("Exception while starting Ambari : {}", e.getMessage());
            throw new CloudbreakException(e);
        }
    }

    @Override
    public FlowContext buildAmbariCluster(FlowContext context) throws CloudbreakException {
        LOGGER.debug("Building ambari cluster. Context: {}", context);
        try {
            context = clusterFacade.buildAmbariCluster(context);
            LOGGER.debug("Building ambari cluster DONE");
            return context;
        } catch (Exception e) {
            LOGGER.error("Exception during the cluster build process: {}", e.getMessage());
            throw new CloudbreakException(e);
        }
    }

    @Override
    public FlowContext resetAmbariCluster(FlowContext context) throws CloudbreakException {
        LOGGER.debug("Reset Ambari cluster. Context: {}", context);
        try {
            context = clusterFacade.resetAmbariCluster(context);
            LOGGER.debug("Reset Ambari cluster DONE");
            return context;
        } catch (Exception e) {
            LOGGER.error("Exception during the cluster reset process: {}", e.getMessage());
            throw new CloudbreakException(e);
        }
    }

    @Override
    public FlowContext handleClusterCreationFailure(FlowContext context) throws CloudbreakException {
        LOGGER.debug("Cluster creation failed. Context: {}", context);
        try {
            context = clusterFacade.handleClusterCreationFailure(context);
            LOGGER.debug("Cluster creation failure handled.");
            return context;
        } catch (Exception e) {
            LOGGER.error("Exception during cluster creation!: {}", e.getMessage());
            throw new CloudbreakException(e);
        }
    }

    @Override
    public FlowContext handleClusterInstallationFailure(FlowContext context) throws CloudbreakException {
        LOGGER.debug("Cluster installation failed. Context: {}", context);
        try {
            context = clusterFacade.handleClusterInstallationFailure(context);
            LOGGER.debug("Cluster installation failure handled.");
            return context;
        } catch (Exception e) {
            LOGGER.error("Exception during cluster installation!: {}", e.getMessage());
            throw new CloudbreakException(e);
        }
    }

    @Override
    public FlowContext stopStackRequested(FlowContext context) throws CloudbreakException {
        LOGGER.debug("Stopping stack requested. Context: {}", context);
        try {
            return stackFacade.stopRequested(context);
        } catch (Exception e) {
            LOGGER.error("Exception during stack stop requested!: {}", e.getMessage());
            throw new CloudbreakException(e);
        }
    }

    @Override
    public FlowContext handleStackStatusUpdateFailure(FlowContext context) throws CloudbreakException {
        LOGGER.debug("Handling stack start/stop failure. Context: {}", context);
        try {
            context = stackFacade.handleStatusUpdateFailure(context);
            LOGGER.debug("Stack start/stop failure is handled.");
            return context;
        } catch (Exception e) {
            LOGGER.error("Exception during handling stack start/stop failure!: {}", e.getMessage());
            throw new CloudbreakException(e);
        }
    }

    @Override
    public FlowContext updateAllowedSubnets(FlowContext context) throws CloudbreakException {
        LOGGER.debug("Update allowed subnet. Context: {}", context);
        try {
            context = stackFacade.updateAllowedSubnets(context);
            LOGGER.debug("Updating of allowed subnet is DONE");
            return context;
        } catch (CloudbreakException e) {
            throw e;
        } catch (Exception e) {
            LOGGER.error("Exception during the updating of allowed subnet: {}", e.getMessage());
            throw new CloudbreakException(e);
        }
    }

    @Override
    public FlowContext handleUpdateAllowedSubnetsFailure(FlowContext context) throws CloudbreakException {
        LOGGER.debug("Handling 'update allowed subnet' failure. Context: {}", context);
        try {
            context = stackFacade.handleUpdateAllowedSubnetsFailure(context);
            LOGGER.debug("Handling of 'update allowed subnet' failure is DONE");
            return context;
        } catch (CloudbreakException e) {
            throw e;
        } catch (Exception e) {
            LOGGER.error("Exception during the handling of update allowed subnet failure: {}", e.getMessage());
            throw new CloudbreakException(e);
        }
    }

    @Override
    public FlowContext startClusterRequested(FlowContext context) throws CloudbreakException {
        LOGGER.debug("Starting cluster requested. Context: {}", context);
        try {
            return clusterFacade.startRequested(context);
        } catch (Exception e) {
            LOGGER.error("Exception during cluster start requested!: {}", e.getMessage());
            throw new CloudbreakException(e);
        }
    }

    @Override
    public FlowContext handleClusterSync(FlowContext context) throws CloudbreakException {
        LOGGER.debug("Cluster sync requested. Context: {}", context);
        try {
            return clusterFacade.sync(context);
        } catch (Exception e) {
            LOGGER.error("Exception during cluster start sync!: {}", e.getMessage());
            throw new CloudbreakException(e);
        }
    }

    @Override
    public FlowContext handleStackSync(FlowContext context) throws CloudbreakException {
        LOGGER.debug("Stack sync requested. Context: {}", context);
        try {
            return stackFacade.sync(context);
        } catch (Exception e) {
            LOGGER.error("Exception during cluster start sync!: {}", e.getMessage());
            throw new CloudbreakException(e);
        }
    }

    @Override
    public FlowContext credentialChange(FlowContext context) throws CloudbreakException {
        LOGGER.debug("Authentication change for cluster requested. Context: {}", context);
        try {
            return clusterFacade.credentialChange(context);
        } catch (Exception e) {
            LOGGER.error("Exception during cluster authentication change!: {}", e.getMessage());
            throw new CloudbreakException(e);
        }
    }
}
