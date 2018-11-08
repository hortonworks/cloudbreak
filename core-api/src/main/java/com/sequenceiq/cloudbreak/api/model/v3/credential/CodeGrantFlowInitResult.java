package com.sequenceiq.cloudbreak.api.model.v3.credential;

import com.sequenceiq.cloudbreak.api.model.JsonEntity;
import com.sequenceiq.cloudbreak.api.model.annotations.Immutable;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@Immutable
@ApiModel
public class CodeGrantFlowInitResult implements JsonEntity {

    @ApiModelProperty(value = ModelDescriptions.CredentialModelDescription.CODE_GRANT_FLOW_LOGIN_URL, required = true)
    private String loginURL;

    public CodeGrantFlowInitResult(String loginURL) {
        this.loginURL = loginURL;
    }

    public String getLoginURL() {
        return loginURL;
    }
}
