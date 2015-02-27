package com.sequenceiq.cloudbreak.core.flow;

import com.sequenceiq.cloudbreak.core.CloudbreakException;

/**
 * Contract for stack provisioning on supported cloud providers.
 */
public interface ProvisioningFacade {

    ProvisioningContext setup(ProvisioningContext provisioningContext) throws CloudbreakException;

    ProvisioningContext provision(ProvisioningContext provisioningContext) throws CloudbreakException;

    ProvisioningContext setupMetadata(ProvisioningContext provisioningContext) throws CloudbreakException;

    ProvisioningContext allocateAmbariRoles(ProvisioningContext provisioningContext) throws CloudbreakException;

    ProvisioningContext startAmbari(ProvisioningContext provisioningContext) throws CloudbreakException;

    ProvisioningContext buildAmbariCluster(ProvisioningContext provisioningContext) throws CloudbreakException;
}
