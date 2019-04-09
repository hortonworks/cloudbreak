package com.sequenceiq.cloudbreak.api.endpoint.v4.events.responses;

import com.sequenceiq.cloudbreak.api.endpoint.v4.JsonEntity;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.views.CredentialViewV4Response;

import io.swagger.annotations.ApiModel;

@ApiModel
public class InteractiveCreationStatusV4Event implements JsonEntity {

    private CredentialViewV4Response credential;

    private String message;

    public CredentialViewV4Response getCredential() {
        return credential;
    }

    public void setCredential(CredentialViewV4Response credential) {
        this.credential = credential;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
