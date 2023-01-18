package com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.environment;

import static com.sequenceiq.cloudbreak.doc.ModelDescriptions.StackModelDescription.CREDENTIAL_NAME;
import static com.sequenceiq.cloudbreak.doc.ModelDescriptions.StackModelDescription.ENVIRONMENT_CRN;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.sequenceiq.cloudbreak.validation.ValidEnvironmentSettings;
import com.sequenceiq.common.model.JsonEntity;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema
@ValidEnvironmentSettings
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(Include.NON_NULL)
public class EnvironmentSettingsV4Request implements JsonEntity {

    @Schema(description = ENVIRONMENT_CRN)
    private String name;

    @Schema(description = CREDENTIAL_NAME)
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
