package com.sequenceiq.environment.api.credential.model.response;

import java.io.Serializable;

import com.sequenceiq.environment.api.credential.doc.CredentialModelDescription;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel
public class CodeGrantFlowInitResult implements Serializable {

    @ApiModelProperty(value = CredentialModelDescription.CODE_GRANT_FLOW_LOGIN_URL, required = true)
    private String loginURL;

    public CodeGrantFlowInitResult(String loginURL) {
        this.loginURL = loginURL;
    }

    public String getLoginURL() {
        return loginURL;
    }
}
