package com.sequenceiq.environment.credential.converter;

import org.springframework.stereotype.Component;

import com.sequenceiq.environment.api.v1.credential.model.parameters.azure.AppBasedRequest;
import com.sequenceiq.environment.api.v1.credential.model.parameters.azure.AzureCredentialRequestParameters;
import com.sequenceiq.environment.api.v1.credential.model.parameters.azure.AzureCredentialResponseParameters;
import com.sequenceiq.environment.api.v1.credential.model.parameters.azure.RoleBasedRequest;
import com.sequenceiq.environment.api.v1.credential.model.parameters.azure.RoleBasedResponse;
import com.sequenceiq.environment.credential.attributes.azure.AppBasedAttributes;
import com.sequenceiq.environment.credential.attributes.azure.AzureCredentialAttributes;
import com.sequenceiq.environment.credential.attributes.azure.RoleBasedAttributes;

@Component
public class AzureCredentialV1ParametersToAzureCredentialAttributesConverter {

    public AzureCredentialAttributes convert(AzureCredentialRequestParameters source) {
        AzureCredentialAttributes response = new AzureCredentialAttributes();
        response.setAppBased(getAppBased(source.getAppBased()));
        response.setRoleBased(getRoleBased(source.getRoleBased()));
        response.setSubscriptionId(source.getSubscriptionId());
        response.setTenantId(source.getTenantId());
        return response;
    }

    public AzureCredentialResponseParameters convert(AzureCredentialAttributes source) {
        AzureCredentialResponseParameters response = new AzureCredentialResponseParameters();
        response.setAccessKey(source.getAccessKey());
        response.setRoleBased(getRoleBased(source.getRoleBased()));
        response.setSubscriptionId(source.getSubscriptionId());
        response.setTenantId(source.getTenantId());
        return response;
    }

    private RoleBasedResponse getRoleBased(RoleBasedAttributes roleBased) {
        RoleBasedResponse response = new RoleBasedResponse();
        response.setAppObjectId(roleBased.getAppObjectId());
        response.setCodeGrantFlow(roleBased.getCodeGrantFlow());
        response.setDeploymentAddress(roleBased.getDeploymentAddress());
        response.setRoleName(roleBased.getRoleName());
        response.setSpDisplayName(roleBased.getSpDisplayName());
        return response;
    }

    private RoleBasedAttributes getRoleBased(RoleBasedRequest roleBased) {
        RoleBasedAttributes response = new RoleBasedAttributes();
        response.setDeploymentAddress(roleBased.getDeploymentAddress());
        response.setRoleName(roleBased.getRoleName());
        return response;
    }

    private AppBasedAttributes getAppBased(AppBasedRequest appBased) {
        AppBasedAttributes response = new AppBasedAttributes();
        response.setAccessKey(appBased.getAccessKey());
        response.setSecretKey(appBased.getAccessKey());
        return response;
    }
}
