package com.sequenceiq.cloudbreak.service.stack.event;

import java.util.List;

import com.sequenceiq.cloudbreak.domain.CloudPlatform;
import com.sequenceiq.cloudbreak.domain.Subnet;

public class UpdateAllowedSubnetsRequest extends ProvisionEvent {

    private List<Subnet> allowedSubnets;

    public UpdateAllowedSubnetsRequest(CloudPlatform cloudPlatform, Long stackId, List<Subnet> allowedSubnets) {
        super(cloudPlatform, stackId);
        this.allowedSubnets = allowedSubnets;
    }

    public List<Subnet> getAllowedSubnets() {
        return allowedSubnets;
    }
}
