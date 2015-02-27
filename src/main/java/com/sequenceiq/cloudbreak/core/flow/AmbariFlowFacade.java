package com.sequenceiq.cloudbreak.core.flow;

/**
 * Contract for Flow related ambari operations.
 */
public interface AmbariFlowFacade {

    ProvisioningContext allocateAmbariRoles(ProvisioningContext context) throws Exception;

    ProvisioningContext startAmbari(ProvisioningContext context) throws Exception;


}
