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
    private AmbariFlowFacade ambariFlowFacade;

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
            ProvisioningContext ambariRoleAllocationContext = (ProvisioningContext) ambariFlowFacade.allocateAmbariRoles(context);
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
            FlowContext ambariStartContext = ambariFlowFacade.startAmbari(context);
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
            context = ambariFlowFacade.buildAmbariCluster(context);
            LOGGER.debug("Building ambari cluster DONE");
            return context;
        } catch (Exception e) {
            LOGGER.error("Exception during the cluster build process: {}", e.getMessage());
            throw new CloudbreakException(e);
        }
    }
}
