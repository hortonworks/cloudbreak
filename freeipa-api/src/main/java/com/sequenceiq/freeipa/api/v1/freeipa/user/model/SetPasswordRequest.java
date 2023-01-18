package com.sequenceiq.freeipa.api.v1.freeipa.user.model;

import java.util.Set;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sequenceiq.freeipa.api.v1.freeipa.user.doc.UserModelDescriptions;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "SetPasswordV1Request")
@JsonIgnoreProperties(ignoreUnknown = true)
public class SetPasswordRequest extends SynchronizeOperationRequestBase {
    @Schema(description = UserModelDescriptions.USER_PASSWORD)
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
