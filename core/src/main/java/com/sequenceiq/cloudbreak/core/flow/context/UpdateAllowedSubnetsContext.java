package com.sequenceiq.cloudbreak.core.flow.context;

import java.util.List;

import com.sequenceiq.cloudbreak.domain.CloudPlatform;
import com.sequenceiq.cloudbreak.domain.SecurityRule;
import com.sequenceiq.cloudbreak.service.stack.event.UpdateAllowedSubnetsRequest;

public class UpdateAllowedSubnetsContext extends DefaultFlowContext implements FlowContext {

    private List<SecurityRule> allowedSecurityRules;

    public UpdateAllowedSubnetsContext(Long stackId, CloudPlatform cloudPlatform, List<SecurityRule> allowedSecurityRules) {
        super(stackId, cloudPlatform);
        this.allowedSecurityRules = allowedSecurityRules;
    }

    public UpdateAllowedSubnetsContext(UpdateAllowedSubnetsRequest request) {
        super(request.getStackId(), request.getCloudPlatform());
        this.allowedSecurityRules = request.getAllowedSecurityRules();
    }

    public List<SecurityRule> getAllowedSecurityRules() {
        return allowedSecurityRules;
    }
}
