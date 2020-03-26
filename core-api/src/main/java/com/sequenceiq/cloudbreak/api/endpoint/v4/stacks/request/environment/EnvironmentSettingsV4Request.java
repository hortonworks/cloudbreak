package com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.environment;

import static com.sequenceiq.cloudbreak.doc.ModelDescriptions.StackModelDescription.CREDENTIAL_NAME;
import static com.sequenceiq.cloudbreak.doc.ModelDescriptions.StackModelDescription.ENVIRONMENT_CRN;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.sequenceiq.common.model.JsonEntity;
import com.sequenceiq.cloudbreak.validation.ValidEnvironmentSettings;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel
@ValidEnvironmentSettings
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(Include.NON_NULL)
public class EnvironmentSettingsV4Request implements JsonEntity {

    @ApiModelProperty(ENVIRONMENT_CRN)
    private String name;

    @ApiModelProperty(CREDENTIAL_NAME)
    private String credentialName;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCredentialName() {
        return credentialName;
    }

    public void setCredentialName(String credentialName) {
        this.credentialName = credentialName;
    }
}
