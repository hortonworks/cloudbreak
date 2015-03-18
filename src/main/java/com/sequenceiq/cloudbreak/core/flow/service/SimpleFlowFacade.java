package com.sequenceiq.cloudbreak.core.flow.service;

import java.util.Map;

import javax.annotation.Resource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.core.CloudbreakException;
import com.sequenceiq.cloudbreak.core.flow.FlowContextFactory;
import com.sequenceiq.cloudbreak.core.flow.context.FlowContext;
import com.sequenceiq.cloudbreak.core.flow.context.ProvisioningContext;
import com.sequenceiq.cloudbreak.core.flow.context.StackStatusUpdateContext;
import com.sequenceiq.cloudbreak.domain.CloudPlatform;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.cloudbreak.service.stack.connector.MetadataSetup;
import com.sequenceiq.cloudbreak.service.stack.connector.ProvisionSetup;
import com.sequenceiq.cloudbreak.service.stack.event.MetadataSetupComplete;
import com.sequenceiq.cloudbreak.service.stack.event.ProvisionComplete;
import com.sequenceiq.cloudbreak.service.stack.event.ProvisionSetupComplete;
import com.sequenceiq.cloudbreak.service.stack.flow.ProvisionContext;

@Service
public class SimpleFlowFacade implements FlowFacade {
    private static final Logger LOGGER = LoggerFactory.getLogger(SimpleFlowFacade.class);

    @Resource
    private Map<CloudPlatform, ProvisionSetup> provisionSetups;

    @Resource
    private Map<CloudPlatform, MetadataSetup> metadataSetups;

    @Autowired
    private ClusterFacade clusterFacade;

    @Autowired
    private StackFacade stackFacade;

    @Autowired
    private ProvisionContext provisioningService;

    @Autowired
    private StackService stackService;

    @Override
    public FlowContext setup(FlowContext context) throws CloudbreakException {
        LOGGER.debug("Provisioning setup. Context: {}", context);
        try {
            ProvisioningContext provisioningContext = (ProvisioningContext) context;
            ProvisionSetupComplete setupComplete = (ProvisionSetupComplete) provisionSetups.get(provisioningContext.getCloudPlatform())
                    .setupProvisioning(stackService.getById(provisioningContext.getStackId()));
            LOGGER.debug("Provisioning setup DONE.");
            return FlowContextFactory.createProvisioningContext(setupComplete.getCloudPlatform(), setupComplete.getStackId(),
                    setupComplete.getSetupProperties(), setupComplete.getUserDataParams());
        } catch (Exception e) {
            LOGGER.error("Exception during provisioning setup: {}", e.getMessage());
            throw new CloudbreakException(e);
        }
    }

    @Override
    public FlowContext provision(FlowContext context) throws CloudbreakException {
        LOGGER.debug("Provisioning. Context: {}", context);
        try {
            ProvisioningContext provisioningContext = (ProvisioningContext) context;
            ProvisionComplete provisionResult = provisioningService.buildStack(provisioningContext.getCloudPlatform(), provisioningContext.getStackId(),
                    provisioningContext.getSetupProperties(), provisioningContext.getUserDataParams());
            LOGGER.debug("Provisioning DONE.");
            return FlowContextFactory
                    .createProvisionCompleteContext(provisionResult.getCloudPlatform(), provisionResult.getStackId(), provisionResult.getResources());
        } catch (Exception e) {
            LOGGER.error("Exception during provisioning setup: {}", e.getMessage());
            throw new CloudbreakException(e);
        }
    }

    @Override
    public FlowContext setupMetadata(FlowContext context) throws CloudbreakException {
        LOGGER.debug("Metadata setup. Context: {}", context);
        try {
            ProvisioningContext provisioningContext = (ProvisioningContext) context;
            MetadataSetupComplete metadataSetupComplete = (MetadataSetupComplete) metadataSetups.get(provisioningContext.getCloudPlatform())
                    .setupMetadata(stackService.getById(provisioningContext.getStackId()));
            LOGGER.debug("Metadata setup DONE.");
            return FlowContextFactory.createMetadataSetupContext(provisioningContext.getCloudPlatform(), provisioningContext.getStackId(),
                    metadataSetupComplete.getCoreInstanceMetaData());
        } catch (Exception e) {
            LOGGER.error("Exception during metadata setup: {}", e.getMessage());
            throw new CloudbreakException(e);
        }
    }

    @Override
    public FlowContext allocateAmbariRoles(FlowContext context) throws CloudbreakException {
        LOGGER.debug("Allocating Ambari roles. Context: {}", context);
        try {
            ProvisioningContext ambariRoleAllocationContext = (ProvisioningContext) clusterFacade.allocateAmbariRoles(context);
            LOGGER.debug("Allocating Ambari roles DONE.");
            return ambariRoleAllocationContext;
        } catch (Exception e) {
            LOGGER.error("Exception during metadata setup: {}", e.getMessage());
            throw new CloudbreakException(e);
        }
    }

    @Override
    public FlowContext startAmbari(FlowContext context) throws CloudbreakException {
        LOGGER.debug("Starting Ambari. Context: {}", context);
        try {
            FlowContext ambariStartContext = clusterFacade.startAmbari(context);
            LOGGER.debug("Ambari start DONE.");
            return ambariStartContext;
        } catch (Exception e) {
            LOGGER.error("Exception during metadata setup: {}", e.getMessage());
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
    public FlowContext clusterCreationFailed(FlowContext context) throws CloudbreakException {
        LOGGER.debug("Cluster creation failed. Context: {}", context);
        try {
            context = clusterFacade.clusterCreationFailed(context);
            LOGGER.debug("Cluster creation failure handled.");
            return context;
        } catch (Exception e) {
            LOGGER.error("Exception during cluster creation failure handling!: {}", e.getMessage());
            throw new CloudbreakException(e);
        }
    }

    @Override
    public FlowContext startCluster(FlowContext flowContext) throws CloudbreakException {
        LOGGER.debug("Starting cluster. Context: {}", flowContext);
        try {
            flowContext = clusterFacade.startCluster(flowContext);
            LOGGER.debug("Cluster started.");
            return flowContext;
        } catch (Exception e) {
            LOGGER.error("Exception during cluster start!: {}", e.getMessage());
            throw new CloudbreakException(e);
        }
    }

    @Override
    public FlowContext clusterStartError(FlowContext flowContext) throws CloudbreakException {
        LOGGER.debug("Starting cluster. Context: {}", flowContext);
        try {
            flowContext = (StackStatusUpdateContext) clusterFacade.clusterStartError(flowContext);
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
            flowContext = (StackStatusUpdateContext) clusterFacade.stopCluster(flowContext);
            LOGGER.debug("Cluster stopped.");
            return flowContext;
        } catch (Exception e) {
            LOGGER.error("Exception during cluster stop!: {}", e.getMessage());
            throw new CloudbreakException(e);
        }
    }

    @Override
    public FlowContext clusterStopError(FlowContext flowContext) throws CloudbreakException {
        LOGGER.debug("Handling cluster stop failure. Context: {}", flowContext);
        try {
            flowContext = (StackStatusUpdateContext) clusterFacade.clusterStopError(flowContext);
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

    public FlowContext upscaleStack(FlowContext context) throws CloudbreakException {
        LOGGER.debug("Upscaling of stack. Context: {}", context);
        try {
            context = stackFacade.upscaleStack(context);
            LOGGER.debug("Upscaling of stack is DONE");
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
}
