package com.sequenceiq.cloudbreak.core.flow.service;

import java.util.Map;

import javax.annotation.Resource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.core.CloudbreakException;
import com.sequenceiq.cloudbreak.core.flow.context.FlowContext;
import com.sequenceiq.cloudbreak.core.flow.context.ProvisioningContext;
import com.sequenceiq.cloudbreak.domain.BillingStatus;
import com.sequenceiq.cloudbreak.domain.CloudPlatform;
import com.sequenceiq.cloudbreak.service.events.CloudbreakEventService;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.cloudbreak.service.stack.connector.MetadataSetup;
import com.sequenceiq.cloudbreak.service.stack.connector.ProvisionSetup;
import com.sequenceiq.cloudbreak.service.stack.event.MetadataSetupComplete;
import com.sequenceiq.cloudbreak.service.stack.event.ProvisionComplete;
import com.sequenceiq.cloudbreak.service.stack.event.ProvisionSetupComplete;
import com.sequenceiq.cloudbreak.service.stack.flow.ProvisioningService;

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
    private ProvisioningService provisioningService;

    @Autowired
    private StackService stackService;

    @Autowired
    private CloudbreakEventService cloudbreakEventService;

    @Override
    public FlowContext setup(FlowContext context) throws CloudbreakException {
        LOGGER.debug("Provisioning setup. Context: {}", context);
        try {
            ProvisioningContext provisioningContext = (ProvisioningContext) context;
            ProvisionSetupComplete setupComplete = (ProvisionSetupComplete) provisionSetups.get(provisioningContext.getCloudPlatform())
                    .setupProvisioning(stackService.getById(provisioningContext.getStackId()));
            LOGGER.debug("Provisioning setup DONE.");
            return new ProvisioningContext.Builder()
                    .setDefaultParams(setupComplete.getStackId(), setupComplete.getCloudPlatform())
                    .setProvisionSetupProperties(setupComplete.getSetupProperties(), setupComplete.getUserDataParams())
                    .build();
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
            return new ProvisioningContext.Builder()
                    .setDefaultParams(provisionResult.getStackId(), provisionResult.getCloudPlatform())
                    .setProvisionedResources(provisionResult.getResources())
                    .build();
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
            cloudbreakEventService.fireCloudbreakEvent(provisioningContext.getStackId(), BillingStatus.BILLING_STARTED.name(),
                    "Provision of stack is successfully finished");
            LOGGER.debug("Metadata setup DONE.");
            return new ProvisioningContext.Builder()
                    .setDefaultParams(provisioningContext.getStackId(), provisioningContext.getCloudPlatform())
                    .setCoreInstanceMetadata(metadataSetupComplete.getCoreInstanceMetaData())
                    .build();
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
    public FlowContext resetAmbariCluster(FlowContext context) throws CloudbreakException {
        LOGGER.debug("Reset ambari cluster. Context: {}", context);
        try {
            context = clusterFacade.resetAmbariCluster(context);
            LOGGER.debug("Reset ambari cluster DONE");
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
    public FlowContext handleSecurityEnableFailure(FlowContext context) throws CloudbreakException {
        try {
            context = clusterFacade.handleSecurityEnableFailure(context);
            LOGGER.debug("Enable kerberos failure handled.");
            return context;
        } catch (Exception e) {
            LOGGER.error("Exception occurred during enabling kerberos. {}", e.getMessage());
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
    public FlowContext stopStack(FlowContext context) throws CloudbreakException {
        LOGGER.debug("Stopping stack. Context: {}", context);
        try {
            context = stackFacade.stop(context);
            return context;
        } catch (Exception e) {
            LOGGER.error("Exception during stack stop!: {}", e.getMessage());
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
    public FlowContext startCluster(FlowContext flowContext) throws CloudbreakException {
        LOGGER.debug("Starting cluster. Context: {}", flowContext);
        try {
            flowContext = clusterFacade.startCluster(flowContext);
            return flowContext;
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
    public FlowContext terminateStack(FlowContext context) throws CloudbreakException {
        LOGGER.debug("Termination of stack. Context: {}", context);
        try {
            context = stackFacade.terminateStack(context);
            LOGGER.debug("Termination of stack is DONE");
            return context;
        } catch (CloudbreakException e) {
            throw e;
        } catch (Exception e) {
            LOGGER.error("Exception during the downscaling of cluster: {}", e.getMessage());
            throw new CloudbreakException(e);
        }
    }

    @Override
    public FlowContext handleStackTerminationFailure(FlowContext context) throws CloudbreakException {
        LOGGER.debug("Handling stack termination failure. Context: {}", context);
        try {
            context = stackFacade.handleTerminationFailure(context);
            LOGGER.debug("Handling of stack termination failure is DONE");
            return context;
        } catch (CloudbreakException e) {
            throw e;
        } catch (Exception e) {
            LOGGER.error("Exception during the handling of stack termination failure: {}", e.getMessage());
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
    public FlowContext enableSecurity(FlowContext context) throws CloudbreakException {
        LOGGER.debug("Enable kerberos security. Context: {}", context);
        try {
            context = clusterFacade.enableSecurity(context);
            return context;
        } catch (CloudbreakException e) {
            throw e;
        } catch (Exception e) {
            LOGGER.error("Exception occurred during enabling kerberos security, failure: {}", e.getMessage());
            throw new CloudbreakException(e);
        }
    }

}
