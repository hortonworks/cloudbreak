package com.sequenceiq.provisioning.controller.json;

import com.amazonaws.services.cloudformation.model.CreateStackResult;

public class AWSStackResult extends StackResult {

    private CreateStackResult createStackResult;

    public AWSStackResult(String status, CreateStackResult createStackResult) {
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
