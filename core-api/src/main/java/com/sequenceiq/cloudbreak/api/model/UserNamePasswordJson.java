package com.sequenceiq.cloudbreak.api.model;

import com.sequenceiq.cloudbreak.doc.ModelDescriptions;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel("UserNamePasswordJson")
public class UserNamePasswordJson {

    @ApiModelProperty(value = ModelDescriptions.UserNamePasswordModelDescription.NEW_USER_NAME, required = true)
    private String userName;
    @ApiModelProperty(value = ModelDescriptions.UserNamePasswordModelDescription.OLD_PASSWORD, required = true)
    private String oldPassword;
    @ApiModelProperty(value = ModelDescriptions.UserNamePasswordModelDescription.NEW_PASSWORD, required = true)
    private String password;

    public UserNamePasswordJson() {

    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getOldPassword() {
        return oldPassword;
    }

    public void setOldPassword(String oldPassword) {
        this.oldPassword = oldPassword;
    }
}
