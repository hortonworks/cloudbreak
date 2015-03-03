package com.sequenceiq.cloudbreak.core.flow.service;

import java.util.Map;

import javax.annotation.Resource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.core.CloudbreakException;
import com.sequenceiq.cloudbreak.core.flow.ProvisioningContextFactory;
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
    private Map<CloudPlatform, ProvisionSetup> provisioningSetupServices;

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
        ProvisionSetupComplete flowContext = (ProvisionSetupComplete) context;
        try {
            flowContext = (ProvisionSetupComplete) provisioningSetupServices.get(flowContext.getCloudPlatform())
                    .setupProvisioning(stackService.getById(flowContext.getStackId()));
            LOGGER.debug("Provisioning setup DONE.");
        } catch (Exception e) {
            LOGGER.error("Exception during provisioning setup: {}", e.getMessage());
            throw new CloudbreakException(e);
        }
        return ProvisioningContextFactory.createProvisioningContext(flowContext.getCloudPlatform(), flowContext.getStackId(),
                flowContext.getSetupProperties(), flowContext.getUserDataParams());
    }

    @Override
    public FlowContext provision(FlowContext provisioningContext) throws CloudbreakException {
        LOGGER.debug("Provisioning. Context: {}", provisioningContext);
        ProvisionComplete provisionResult = null;
        ProvisioningContext context = null;

        try {
            context = (ProvisioningContext) provisioningContext;
            provisionResult = provisioningService.buildStack(context.getCloudPlatform(), context.getStackId(),
                    context.getSetupProperties(), context.getUserDataParams());
        } catch (Exception e) {
            //LOGGER.info("Publishing {} event.", ReactorConfig.STACK_CREATE_FAILED_EVENT);
            LOGGER.error("Exception during provisioning setup: {}", e.getMessage());
            throw new CloudbreakException(e);
        }
        //todo change the return value!
        return context;
    }

    @Override
    public FlowContext setupMetadata(FlowContext context) throws CloudbreakException {
        LOGGER.debug("Metadata setup. Context: {}", context);
        MetadataSetupComplete metadataSetupComplete = null;
        ProvisioningContext provisioningContext = (ProvisioningContext) context;
        try {
            metadataSetupComplete = (MetadataSetupComplete) metadataSetups.get(provisioningContext.getCloudPlatform())
                    .setupMetadata(stackService.getById(provisioningContext.getStackId()));
            LOGGER.debug("Metadata setup DONE.");
        } catch (Exception e) {
            LOGGER.error("Exception during metadata setup: {}", e.getMessage());
            throw new CloudbreakException(e);
        }
        return ProvisioningContextFactory.createProvisioningSetupContext(metadataSetupComplete.getCloudPlatform(), metadataSetupComplete.getStackId());
    }

    @Override
    public ProvisioningContext allocateAmbariRoles(FlowContext context) throws CloudbreakException {
        LOGGER.debug("Allocating Ambari roles. Context: {}", context);

        ProvisioningContext ambariRoleAllocationContext = null;
        try {
            ambariRoleAllocationContext = (ProvisioningContext) ambariFlowFacade.allocateAmbariRoles(context);
            LOGGER.debug("Metadata setup DONE.");
        } catch (Exception e) {
            LOGGER.error("Exception during metadata setup: {}", e.getMessage());
            throw new CloudbreakException(e);
        }
        return ambariRoleAllocationContext;
    }

    @Override
    public FlowContext startAmbari(FlowContext context) throws CloudbreakException {
        LOGGER.debug("Starting Ambari. Context: {}", context);
        FlowContext ambariStartContext = null;
        try {
            ambariStartContext = ambariFlowFacade.startAmbari(context);
            LOGGER.debug("Ambari start DONE.");
        } catch (Exception e) {
            LOGGER.error("Exception during metadata setup: {}", e.getMessage());
            throw new CloudbreakException(e);
        }
        return ambariStartContext;
    }

    @Override
    public FlowContext buildAmbariCluster(FlowContext context) throws CloudbreakException {
        LOGGER.debug("Building ambari cluster. Context: {}", context);
        try {
            context = ambariFlowFacade.buildAmbariCluster(context);
        } catch (Exception e) {
            LOGGER.error("Exception during the cluster build process: {}", e.getMessage());
            throw new CloudbreakException(e);
        }
        return context;
    }
}
