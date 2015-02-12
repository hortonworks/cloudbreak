package com.sequenceiq.cloudbreak.core.flow;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.core.CloudbreakException;

@Service
public class SimpleProvisioningFacade implements ProvisioningFacade {
    private static final Logger LOGGER = LoggerFactory.getLogger(SimpleProvisioningFacade.class);

    @Override
    public ProvisioningContext setup(ProvisioningContext provisioningContext) throws CloudbreakException {
        LOGGER.debug("Set up provisioning. Context: {}", provisioningContext);
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
        LOGGER.debug("Building ambari cluster. Context: {}", provisioningContext);
        return provisioningContext;
    }

    @Override
    public ProvisioningContext buildAmbariCluster(ProvisioningContext provisioningContext) throws CloudbreakException {
        LOGGER.debug("Building ambari cluster. Context: {}", provisioningContext);
        return provisioningContext;
    }
}
