package com.sequenceiq.environment.api.v1.credential.model.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.sequenceiq.environment.api.doc.ModelDescriptions;
import com.sequenceiq.environment.api.doc.credential.CredentialDescriptor;
import com.sequenceiq.environment.api.doc.credential.CredentialModelDescription;
import com.sequenceiq.environment.api.v1.credential.model.CredentialBase;
import com.sequenceiq.environment.api.v1.credential.model.parameters.azure.AzureCredentialRequestParameters;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = CredentialDescriptor.CREDENTIAL_NOTES, allOf = CredentialBase.class, name = "CredentialV1Request")
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(Include.NON_NULL)
public class CredentialRequest extends CredentialBase {

    @Size(max = 100, min = 5, message = "The length of the credential's name has to be in range of 5 to 100")
    @Pattern(regexp = "(^[a-z][-a-z0-9]*[a-z0-9]$)",
            message = "The name of the credential can only contain lowercase alphanumeric characters and hyphens and has start with an alphanumeric character")
    @Schema(description = ModelDescriptions.NAME, required = true)
    private String name;

    @Valid
    @Schema(description = CredentialModelDescription.AZURE_PARAMETERS)
    private AzureCredentialRequestParameters azure;

    public AzureCredentialRequestParameters getAzure() {
        return azure;
    }

    public void setAzure(AzureCredentialRequestParameters azure) {
        this.azure = azure;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return "CredentialRequest{"
            + "name='" + getName()
            + "',cloudPlatform='" + getCloudPlatform()
            + "'}";
    }
}
