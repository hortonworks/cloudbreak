package com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.environment;

import static com.sequenceiq.cloudbreak.doc.ModelDescriptions.StackModelDescription.CREDENTIAL_NAME;
import static com.sequenceiq.cloudbreak.doc.ModelDescriptions.StackModelDescription.ENVIRONMENT;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.sequenceiq.cloudbreak.api.endpoint.v4.JsonEntity;
import com.sequenceiq.cloudbreak.api.endpoint.v4.credentials.responses.CredentialV4Response;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(Include.NON_NULL)
public class EnvironmentSettingsV4Response implements JsonEntity {

    @ApiModelProperty(ENVIRONMENT)
    private String name;

    @ApiModelProperty(CREDENTIAL_NAME)
    private CredentialV4Response credential;

    private String cloudPlatform;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public CredentialV4Response getCredential() {
        return credential;
    }

    public void setCredential(CredentialV4Response credential) {
        this.credential = credential;
    }

    public String getCloudPlatform() {
        return cloudPlatform;
    }

    public void setCloudPlatform(String cloudPlatform) {
        this.cloudPlatform = cloudPlatform;
    }

}
