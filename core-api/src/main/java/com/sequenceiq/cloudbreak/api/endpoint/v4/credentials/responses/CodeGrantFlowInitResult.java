package com.sequenceiq.cloudbreak.api.endpoint.v4.credentials.responses;

import com.sequenceiq.cloudbreak.api.endpoint.v4.JsonEntity;
import com.sequenceiq.cloudbreak.api.model.annotations.Immutable;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions.CredentialModelDescription;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@Immutable
@ApiModel
public class CodeGrantFlowInitResult implements JsonEntity {

    @ApiModelProperty(value = CredentialModelDescription.CODE_GRANT_FLOW_LOGIN_URL, required = true)
    private String loginURL;

    public CodeGrantFlowInitResult(String loginURL) {
        this.loginURL = loginURL;
    }

    public String getLoginURL() {
        return loginURL;
    }
}
