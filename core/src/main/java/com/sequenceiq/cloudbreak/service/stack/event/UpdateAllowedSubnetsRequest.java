package com.sequenceiq.cloudbreak.service.stack.event;

import java.util.List;

import com.sequenceiq.cloudbreak.domain.CloudPlatform;
import com.sequenceiq.cloudbreak.domain.SecurityRule;

public class UpdateAllowedSubnetsRequest extends ProvisionEvent {

    private List<SecurityRule> allowedSecurityRules;

    public UpdateAllowedSubnetsRequest(CloudPlatform cloudPlatform, Long stackId, List<SecurityRule> allowedSecurityRules) {
        super(cloudPlatform, stackId);
        this.allowedSecurityRules = allowedSecurityRules;
    }

    public List<SecurityRule> getAllowedSecurityRules() {
        return allowedSecurityRules;
    }
}
