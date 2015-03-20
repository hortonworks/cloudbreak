package com.sequenceiq.cloudbreak.core.flow.context;

import java.util.List;

import com.sequenceiq.cloudbreak.domain.CloudPlatform;
import com.sequenceiq.cloudbreak.domain.Subnet;
import com.sequenceiq.cloudbreak.service.stack.event.UpdateAllowedSubnetsRequest;

public class UpdateAllowedSubnetsContext implements FlowContext {

    private Long stackId;
    private CloudPlatform cloudPlatform;
    private List<Subnet> allowedSubnets;
    private String errorMessage = "";

    public UpdateAllowedSubnetsContext() { }

    public UpdateAllowedSubnetsContext(UpdateAllowedSubnetsRequest request) {
        this.stackId = request.getStackId();
        this.cloudPlatform = request.getCloudPlatform();
        this.allowedSubnets = request.getAllowedSubnets();
    }

    public Long getStackId() {
        return stackId;
    }

    public void setStackId(Long stackId) {
        this.stackId = stackId;
    }

    public CloudPlatform getCloudPlatform() {
        return cloudPlatform;
    }

    public void setCloudPlatform(CloudPlatform cloudPlatform) {
        this.cloudPlatform = cloudPlatform;
    }

    public List<Subnet> getAllowedSubnets() {
        return allowedSubnets;
    }

    public void setAllowedSubnets(List<Subnet> allowedSubnets) {
        this.allowedSubnets = allowedSubnets;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }
}
