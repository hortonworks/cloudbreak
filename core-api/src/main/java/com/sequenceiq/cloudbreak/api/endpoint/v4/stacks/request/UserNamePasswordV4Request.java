package com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions.UserNamePasswordModelDescription;
import com.sequenceiq.common.model.JsonEntity;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema
@JsonIgnoreProperties(ignoreUnknown = true)
public class UserNamePasswordV4Request implements JsonEntity {

    @NotNull
    @Size(min = 1, message = "The length of the name has to be greater than 1")
    @Schema(description = UserNamePasswordModelDescription.NEW_USER_NAME, required = true)
    private String userName;

    @NotNull
    @Size(min = 1, message = "The length of the old password has to be greater than 1")
    @Schema(description = UserNamePasswordModelDescription.OLD_PASSWORD, required = true)
    private String oldPassword;

    @NotNull
    @Size(min = 1, message = "The length of the password has to be greater than 1")
    @Schema(description = UserNamePasswordModelDescription.NEW_PASSWORD, required = true)
    private String password;

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
