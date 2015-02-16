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
import com.sequenceiq.cloudbreak.service.stack.connector.ProvisionSetup;

@Service
public class SimpleProvisioningFacade implements ProvisioningFacade {
    private static final Logger LOGGER = LoggerFactory.getLogger(SimpleProvisioningFacade.class);

    @Resource
    private Map<CloudPlatform, ProvisionSetup> provisionSetups;

    @Autowired
    private StackService stackService;

    @Override
    public ProvisioningContext setup(ProvisioningContext provisioningContext) throws CloudbreakException {
        LOGGER.debug("Set up provisioning. Context: {}", provisioningContext);
        try {
            provisionSetups.get(provisioningContext.getCloudPlatform()).setupProvisioning(stackService.get(provisioningContext.getStackId()));
            LOGGER.debug("Provisioning setup done.");
        } catch (Exception e) {
            LOGGER.error("Exception during provisioning setup" + e.getMessage());
            throw new CloudbreakException(e);
        }
        return provisioningContext;
    }

    @Override
    public ProvisioningContext provision(ProvisioningContext provisioningContext) throws CloudbreakException {
        LOGGER.debug("Provisioning. Context: {}", provisioningContext);
        return provisioningContext;
    }

    @Override
    public ProvisioningContext setupMetadata(ProvisioningContext provisioningContext) throws CloudbreakException {
        LOGGER.debug("Set up metadata. Context: {}", provisioningContext);
        return provisioningContext;
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
