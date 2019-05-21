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
        if (source == null) {
            return null;
        }
        AzureCredentialAttributes response = new AzureCredentialAttributes();
        response.setAppBased(getAppBased(source.getAppBased()));
        response.setRoleBased(getRoleBased(source.getRoleBased()));
        response.setSubscriptionId(source.getSubscriptionId());
        response.setTenantId(source.getTenantId());
        return response;
    }

    public AzureCredentialResponseParameters convert(AzureCredentialAttributes source) {
        if (source == null) {
            return null;
        }
        AzureCredentialResponseParameters response = new AzureCredentialResponseParameters();
        response.setAccessKey(source.getAccessKey());
        response.setRoleBased(getRoleBased(source.getRoleBased()));
        response.setSubscriptionId(source.getSubscriptionId());
        response.setTenantId(source.getTenantId());
        return response;
    }

    private RoleBasedResponse getRoleBased(RoleBasedAttributes source) {
        if (source == null) {
            return null;
        }
        RoleBasedResponse response = new RoleBasedResponse();
        response.setAppObjectId(source.getAppObjectId());
        response.setCodeGrantFlow(source.getCodeGrantFlow());
        response.setDeploymentAddress(source.getDeploymentAddress());
        response.setRoleName(source.getRoleName());
        response.setSpDisplayName(source.getSpDisplayName());
        return response;
    }

    private RoleBasedAttributes getRoleBased(RoleBasedRequest source) {
        if (source == null) {
            return null;
        }
        RoleBasedAttributes response = new RoleBasedAttributes();
        response.setDeploymentAddress(source.getDeploymentAddress());
        response.setRoleName(source.getRoleName());
        return response;
    }

    private AppBasedAttributes getAppBased(AppBasedRequest source) {
        if (source == null) {
            return null;
        }
        AppBasedAttributes response = new AppBasedAttributes();
        response.setAccessKey(source.getAccessKey());
        response.setSecretKey(source.getAccessKey());
        return response;
    }
}
