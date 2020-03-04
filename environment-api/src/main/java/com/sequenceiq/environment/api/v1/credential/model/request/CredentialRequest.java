package com.sequenceiq.environment.api.v1.credential.model.request;

import java.util.Map;

import javax.validation.Valid;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.google.common.collect.Maps;
import com.sequenceiq.authorization.resource.AuthorizableFieldInfoModel;
import com.sequenceiq.authorization.resource.AuthorizationApiRequest;
import com.sequenceiq.authorization.resource.AuthorizationResourceAction;
import com.sequenceiq.authorization.resource.AuthorizationResourceType;
import com.sequenceiq.authorization.resource.AuthorizationVariableType;
import com.sequenceiq.environment.api.doc.credential.CredentialDescriptor;
import com.sequenceiq.environment.api.doc.credential.CredentialModelDescription;
import com.sequenceiq.environment.api.v1.credential.model.CredentialBase;
import com.sequenceiq.environment.api.v1.credential.model.parameters.azure.AzureCredentialRequestParameters;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel(description = CredentialDescriptor.CREDENTIAL_NOTES, parent = CredentialBase.class, value = "CredentialV1Request")
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(Include.NON_NULL)
public class CredentialRequest extends CredentialBase implements AuthorizationApiRequest {

    @Valid
    @ApiModelProperty(CredentialModelDescription.AZURE_PARAMETERS)
    private AzureCredentialRequestParameters azure;

    public AzureCredentialRequestParameters getAzure() {
        return azure;
    }

    public void setAzure(AzureCredentialRequestParameters azure) {
        this.azure = azure;
    }

    @Override
    public String toString() {
        return "CredentialRequest{"
            + "name='" + getName()
            + "',cloudPlatform='" + getCloudPlatform()
            + "'}";
    }

    @Override
    public Map<String, AuthorizableFieldInfoModel> getAuthorizableFields() {
        Map<String, AuthorizableFieldInfoModel> authorizableFields = Maps.newHashMap();
        authorizableFields.put(getName(), new AuthorizableFieldInfoModel(AuthorizationResourceType.CREDENTIAL,
                AuthorizationResourceAction.READ, AuthorizationVariableType.NAME));
        return authorizableFields;
    }
}
