package com.sequenceiq.freeipa.api.v1.freeipa.user.model;

import java.util.Set;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sequenceiq.freeipa.api.v1.freeipa.user.doc.UserModelDescriptions;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel("SetPasswordV1Request")
@JsonIgnoreProperties(ignoreUnknown = true)
public class SetPasswordRequest extends SynchronizeOperationRequestBase {
    @ApiModelProperty(value = UserModelDescriptions.USER_PASSWORD)
    private String password;

    public SetPasswordRequest() {
    }

    public SetPasswordRequest(Set<String> environments, String password) {
        super(environments);
        this.password = password;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    @Override
    public String toString() {
        return "SetPasswordRequest{"
                + fieldsToString()
                + '}';
    }
}
