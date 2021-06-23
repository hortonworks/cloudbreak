package com.sequenceiq.environment.api.v1.environment.model.request;

import com.sequenceiq.environment.api.doc.environment.EnvironmentModelDescription;
import com.sequenceiq.flow.api.model.FlowIdentifier;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel(value = "EnvironmentLbUpdateStatusRequest")
public class EnvironmentLbUpdateStatusRequest {

    @ApiModelProperty(EnvironmentModelDescription.LB_UPDATE_FLOWID)
    private String environmentFlowId;

    public String getEnvironmentFlowId() {
        return environmentFlowId;
    }

    public void setEnvironmentFlowId(String environmentFlowId) {
        this.environmentFlowId = environmentFlowId;
    }
}
