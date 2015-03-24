package com.sequenceiq.cloudbreak.core.flow.context;

import java.util.List;

import com.sequenceiq.cloudbreak.domain.CloudPlatform;
import com.sequenceiq.cloudbreak.domain.Subnet;
import com.sequenceiq.cloudbreak.service.stack.event.UpdateAllowedSubnetsRequest;

public class UpdateAllowedSubnetsContext extends DefaultFlowContext implements FlowContext {

    private List<Subnet> allowedSubnets;

    public UpdateAllowedSubnetsContext(Long stackId, CloudPlatform cloudPlatform, List<Subnet> allowedSubnets) {
        super(stackId, cloudPlatform);
        this.allowedSubnets = allowedSubnets;
    }

    public UpdateAllowedSubnetsContext(UpdateAllowedSubnetsRequest request) {
        super(request.getStackId(), request.getCloudPlatform());
        this.allowedSubnets = request.getAllowedSubnets();
    }

    public List<Subnet> getAllowedSubnets() {
        return allowedSubnets;
    }
}
