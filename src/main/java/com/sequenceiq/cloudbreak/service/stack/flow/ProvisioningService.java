package com.sequenceiq.cloudbreak.service.stack.flow;

import com.sequenceiq.cloudbreak.core.CloudbreakException;

/**
 * Contract for stack provisioning on supported cloud provider.
 */
public interface ProvisioningService {

    void setup(ProvisioningContext context) throws CloudbreakException;

    void provision(ProvisioningContext context) throws CloudbreakException;
}
