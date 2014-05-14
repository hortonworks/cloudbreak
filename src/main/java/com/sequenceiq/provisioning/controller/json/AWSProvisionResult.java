package com.sequenceiq.provisioning.controller.json;

import com.amazonaws.services.cloudformation.model.CreateStackResult;

public class AWSProvisionResult extends ProvisionResult {

    private CreateStackResult createStackResult;

    public AWSProvisionResult(String status, CreateStackResult createStackResult) {
        super(status);
        this.createStackResult = createStackResult;
    }

    public CreateStackResult getCreateStackResult() {
        return createStackResult;
    }

    public void setCreateStackResult(CreateStackResult createStackResult) {
        this.createStackResult = createStackResult;
    }

}
