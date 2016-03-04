package com.sequenceiq.cloudbreak.core.flow.service;

import com.sequenceiq.cloudbreak.concurrent.LockedMethod;
import com.sequenceiq.cloudbreak.core.CloudbreakException;
import com.sequenceiq.cloudbreak.core.flow.context.FlowContext;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.cloudbreak.service.stack.flow.MetadataSetupService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.inject.Inject;

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
    public FlowContext setupConsulMetadata(FlowContext context) throws CloudbreakException {
        LOGGER.debug("Setting up Consul metadata. Context: {}", context);
        try {
            return stackFacade.setupConsulMetadata(context);
        } catch (Exception e) {
            LOGGER.error("Exception during Consul metadata setup.", e.getMessage());
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
    public FlowContext handleStackCreationFailure(FlowContext context) throws CloudbreakException {
        LOGGER.debug("Stack creation failed. Context: {}", context);
        try {
            context = stackFacade.handleCreationFailure(context);
            LOGGER.debug("Stack creation failure handled.");
            return context;
        } catch (Exception e) {
            LOGGER.error("Exception during stack creation!: {}", e.getMessage());
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
    public FlowContext startStack(FlowContext context) throws CloudbreakException {
        LOGGER.debug("Starting stack. Context: {}", context);
        try {
            context = stackFacade.start(context);
            LOGGER.debug("Stack started.");
            return context;
        } catch (Exception e) {
            LOGGER.error("Exception during stack start!: {}", e.getMessage());
            throw new CloudbreakException(e);
        }
    }

    @Override
    @LockedMethod(lockPrefix = "stopStack")
    public FlowContext stopStack(FlowContext context) throws CloudbreakException {
        LOGGER.debug("Stopping stack. Context: {}", context);
        try {
            return stackFacade.stop(context);
        } catch (Exception e) {
            LOGGER.error("Exception during stack stop!: {}", e.getMessage());
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
    @LockedMethod(lockPrefix = "startCluster")
    public FlowContext startCluster(FlowContext flowContext) throws CloudbreakException {
        LOGGER.debug("Starting cluster. Context: {}", flowContext);
        try {
            return clusterFacade.startCluster(flowContext);
        } catch (Exception e) {
            LOGGER.error("Exception during cluster start!: {}", e.getMessage());
            throw new CloudbreakException(e);
        }
    }

    @Override
    public FlowContext handleClusterStartFailure(FlowContext flowContext) throws CloudbreakException {
        LOGGER.debug("Starting cluster. Context: {}", flowContext);
        try {
            flowContext = clusterFacade.handleStartFailure(flowContext);
            LOGGER.debug("Cluster started.");
            return flowContext;
        } catch (Exception e) {
            LOGGER.error("Exception during cluster start!: {}", e.getMessage());
            throw new CloudbreakException(e);
        }
    }

    @Override
    public FlowContext stopCluster(FlowContext flowContext) throws CloudbreakException {
        LOGGER.debug("Stopping cluster. Context: {}", flowContext);
        try {
            flowContext = clusterFacade.stopCluster(flowContext);
            LOGGER.debug("Cluster stopped.");
            return flowContext;
        } catch (Exception e) {
            LOGGER.error("Exception during cluster stop!: {}", e.getMessage());
            throw new CloudbreakException(e);
        }
    }

    @Override
    public FlowContext handleClusterStopFailure(FlowContext flowContext) throws CloudbreakException {
        LOGGER.debug("Handling cluster stop failure. Context: {}", flowContext);
        try {
            flowContext = clusterFacade.handleStopFailure(flowContext);
            LOGGER.debug("Cluster stop failure handled.");
            return flowContext;
        } catch (Exception e) {
            LOGGER.error("Exception during cluster stop failure!: {}", e.getMessage());
            throw new CloudbreakException(e);
        }
    }

    public FlowContext downscaleStack(FlowContext context) throws CloudbreakException {
        LOGGER.debug("Downscaling of stack. Context: {}", context);
        try {
            context = stackFacade.downscaleStack(context);
            LOGGER.debug("Downscaling of stack is DONE");
            return context;
        } catch (CloudbreakException e) {
            throw e;
        } catch (Exception e) {
            LOGGER.error("Exception during the downscaling of stack: {}", e.getMessage());
            throw new CloudbreakException(e);
        }
    }

    public FlowContext addInstances(FlowContext context) throws CloudbreakException {
        LOGGER.debug("Adding new instances to the stack. Context: {}", context);
        try {
            context = stackFacade.addInstances(context);
            LOGGER.debug("Adding new instances to the stack is DONE");
            return context;
        } catch (CloudbreakException e) {
            throw e;
        } catch (Exception e) {
            LOGGER.error("Exception during the upscaling of stack: {}", e.getMessage());
            throw new CloudbreakException(e);
        }
    }

    @Override
    public FlowContext removeInstance(FlowContext context) throws CloudbreakException {
        LOGGER.debug("Remove instance from the stack. Context: {}", context);
        try {
            context = stackFacade.removeInstance(context);
            LOGGER.debug("Removing instance from the stack is DONE");
            return context;
        } catch (CloudbreakException e) {
            throw e;
        } catch (Exception e) {
            LOGGER.error("Exception during removing instance from the stack: {}", e.getMessage());
            throw new CloudbreakException(e);
        }
    }

    @Override
    public FlowContext extendMetadata(FlowContext context) throws CloudbreakException {
        LOGGER.debug("Extending metadata with new instances. Context: {}", context);
        try {
            context = stackFacade.extendMetadata(context);
            LOGGER.debug("Extending metadata with new instances is DONE");
            return context;
        } catch (CloudbreakException e) {
            throw e;
        } catch (Exception e) {
            LOGGER.error("Exception during the upscaling of stack: {}", e.getMessage());
            throw new CloudbreakException(e);
        }
    }

    @Override
    public FlowContext handleStackScalingFailure(FlowContext context) throws CloudbreakException {
        LOGGER.debug("Handling stack scaling failure. Context: {}", context);
        try {
            context = stackFacade.handleScalingFailure(context);
            LOGGER.debug("Handling of stack scaling failure is DONE");
            return context;
        } catch (Exception e) {
            LOGGER.error("Exception during the downscaling of stack: {}", e.getMessage());
            throw new CloudbreakException(e);
        }
    }

    @Override
    public FlowContext upscaleCluster(FlowContext context) throws CloudbreakException {
        LOGGER.debug("Upscaling of cluster. Context: {}", context);
        try {
            context = clusterFacade.upscaleCluster(context);
            LOGGER.debug("Upscaling of cluster is DONE");
            return context;
        } catch (CloudbreakException e) {
            throw e;
        } catch (Exception e) {
            LOGGER.error("Exception during the upscaling of cluster: {}", e.getMessage());
            throw new CloudbreakException(e);
        }
    }

    @Override
    public FlowContext bootstrapNewNodes(FlowContext context) throws CloudbreakException {
        LOGGER.debug("Bootstrapping new nodes. Context: {}", context);
        try {
            context = stackFacade.bootstrapNewNodes(context);
            LOGGER.debug("Bootstrap of new nodes is finished.");
            return context;
        } catch (CloudbreakException e) {
            throw e;
        } catch (Exception e) {
            LOGGER.error("Exception during the upscaling of cluster nodes prepare: {}", e.getMessage());
            throw new CloudbreakException(e);
        }
    }

    @Override
    public FlowContext extendConsulMetadata(FlowContext context) throws CloudbreakException {
        LOGGER.debug("Extending Consul metadata. Context: {}", context);
        try {
            context = stackFacade.extendConsulMetadata(context);
            LOGGER.debug("Extending Consul metadata is finished.");
            return context;
        } catch (CloudbreakException e) {
            throw e;
        } catch (Exception e) {
            LOGGER.error("Exception during the upscaling of cluster nodes prepare: {}", e.getMessage());
            throw new CloudbreakException(e);
        }
    }

    @Override
    public FlowContext addClusterContainers(FlowContext context) throws CloudbreakException {
        LOGGER.debug("Adding cluster containers. Context: {}", context);
        try {
            context = clusterFacade.addClusterContainers(context);
            LOGGER.debug("'Adding cluster containers' phase is finished.");
            return context;
        } catch (CloudbreakException e) {
            throw e;
        } catch (Exception e) {
            LOGGER.error("Exception during the upscaling of cluster nodes prepare: {}", e.getMessage());
            throw new CloudbreakException(e);
        }
    }

    @Override
    public FlowContext downscaleCluster(FlowContext context) throws CloudbreakException {
        LOGGER.debug("Downscaling of cluster. Context: {}", context);
        try {
            context = clusterFacade.downscaleCluster(context);
            LOGGER.debug("Downscaling of cluster is DONE");
            return context;
        } catch (CloudbreakException e) {
            throw e;
        } catch (Exception e) {
            LOGGER.error("Exception during the downscaling of cluster: {}", e.getMessage());
            throw new CloudbreakException(e);
        }
    }

    @Override
    public FlowContext handleClusterScalingFailure(FlowContext context) throws CloudbreakException {
        LOGGER.debug("Handling cluster scaling failure. Context: {}", context);
        try {
            context = clusterFacade.handleScalingFailure(context);
            LOGGER.debug("Handling of cluster scaling failure is DONE");
            return context;
        } catch (CloudbreakException e) {
            throw e;
        } catch (Exception e) {
            LOGGER.error("Exception during the handling of cluster scaling failure: {}", e.getMessage());
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
    public FlowContext bootstrapCluster(FlowContext context) throws CloudbreakException {
        LOGGER.debug("Handling cluster bootstrap. Context: {}", context);
        try {
            context = stackFacade.bootstrapCluster(context);
            LOGGER.debug("Cluster bootstrap is DONE");
            return context;
        } catch (CloudbreakException e) {
            throw e;
        } catch (Exception e) {
            LOGGER.error("Exception during the handling of munchausen setup failure: {}", e.getMessage());
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
