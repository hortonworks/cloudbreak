package com.sequenceiq.cloudbreak.core.flow;

import java.util.Map;

import javax.annotation.Resource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.core.CloudbreakException;
import com.sequenceiq.cloudbreak.domain.CloudPlatform;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.cloudbreak.service.stack.connector.MetadataSetup;
import com.sequenceiq.cloudbreak.service.stack.connector.ProvisionSetup;
import com.sequenceiq.cloudbreak.service.stack.event.MetadataSetupComplete;
import com.sequenceiq.cloudbreak.service.stack.event.ProvisionComplete;
import com.sequenceiq.cloudbreak.service.stack.event.ProvisionSetupComplete;
import com.sequenceiq.cloudbreak.service.stack.flow.ProvisionContext;

@Service
public class SimpleProvisioningFacade implements ProvisioningFacade {
    private static final Logger LOGGER = LoggerFactory.getLogger(SimpleProvisioningFacade.class);

    @Resource
    private Map<CloudPlatform, ProvisionSetup> provisioningSetupServices;

    @Resource
    private Map<CloudPlatform, MetadataSetup> metadataSetups;

    @Autowired
    private ProvisionContext provisioningService;

    @Autowired
    private StackService stackService;

    @Override
    public ProvisioningContext setup(ProvisioningContext provisioningContext) throws CloudbreakException {
        LOGGER.debug("Provisioning setup. Context: {}", provisioningContext);
        ProvisionSetupComplete provisioningSetupResult = null;
        try {
            provisioningSetupResult = (ProvisionSetupComplete) provisioningSetupServices.get(provisioningContext.getCloudPlatform())
                    .setupProvisioning(stackService.getById(provisioningContext.getStackId()));
            LOGGER.debug("Provisioning setup DONE.");
        } catch (Exception e) {
            LOGGER.error("Exception during provisioning setup: {}", e.getMessage());
            throw new CloudbreakException(e);
        }
        return ProvisioningContextFactory.createProvisioningContext(provisioningSetupResult.getCloudPlatform(), provisioningSetupResult.getStackId(),
                provisioningSetupResult.getSetupProperties(), provisioningSetupResult.getUserDataParams());
    }

    @Override
    public ProvisioningContext provision(ProvisioningContext provisioningContext) throws CloudbreakException {
        LOGGER.debug("Provisioning. Context: {}", provisioningContext);
        ProvisionComplete provisionResult = null;
        try {
            provisionResult = provisioningService.buildStack(provisioningContext.getCloudPlatform(), provisioningContext.getStackId(),
                    provisioningContext.getSetupProperties(), provisioningContext.getUserDataParams());
        } catch (Exception e) {
            //LOGGER.info("Publishing {} event.", ReactorConfig.STACK_CREATE_FAILED_EVENT);
            LOGGER.error("Exception during provisioning setup: {}", e.getMessage());
            throw new CloudbreakException(e);
        }
        return provisioningContext;
    }

    @Override
    public ProvisioningContext setupMetadata(ProvisioningContext provisioningContext) throws CloudbreakException {
        LOGGER.debug("Metadata setup. Context: {}", provisioningContext);
        MetadataSetupComplete metadataSetupComplete = null;
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
    public ProvisioningContext allocateRoles(ProvisioningContext provisioningContext) throws CloudbreakException {
        LOGGER.debug("Allocate roles. Context: {}", provisioningContext);
        return provisioningContext;
    }

    @Override public ProvisioningContext startAmbari(ProvisioningContext provisioningContext) throws CloudbreakException {
        LOGGER.debug("Start Ambari. Context: {}", provisioningContext);
        return provisioningContext;
    }

    @Override
    public ProvisioningContext buildAmbariCluster(ProvisioningContext provisioningContext) throws CloudbreakException {
        LOGGER.debug("Building ambari cluster. Context: {}", provisioningContext);
        return provisioningContext;
    }
}
