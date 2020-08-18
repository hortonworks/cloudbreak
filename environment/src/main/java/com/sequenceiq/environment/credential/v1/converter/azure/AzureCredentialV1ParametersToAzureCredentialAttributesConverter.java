package com.sequenceiq.environment.credential.v1.converter.azure;

import static com.sequenceiq.cloudbreak.util.NullUtil.doIfNotNull;

import org.springframework.stereotype.Component;

import com.sequenceiq.environment.api.v1.credential.model.parameters.azure.AppBasedRequest;
import com.sequenceiq.environment.api.v1.credential.model.parameters.azure.AzureCredentialRequestParameters;
import com.sequenceiq.environment.api.v1.credential.model.parameters.azure.AzureCredentialResponseParameters;
import com.sequenceiq.environment.api.v1.credential.model.parameters.azure.RoleBasedRequest;
import com.sequenceiq.environment.api.v1.credential.model.parameters.azure.RoleBasedResponse;
import com.sequenceiq.environment.credential.attributes.azure.AppBasedAttributes;
import com.sequenceiq.environment.credential.attributes.azure.AzureCredentialAttributes;
import com.sequenceiq.environment.credential.attributes.azure.CodeGrantFlowAttributes;

@Component
public class AzureCredentialV1ParametersToAzureCredentialAttributesConverter {

    public AzureCredentialAttributes convert(AzureCredentialRequestParameters source) {
        AzureCredentialAttributes response = new AzureCredentialAttributes();
        doIfNotNull(source.getAppBased(), param -> response.setAppBased(getAppBased(param)));
        doIfNotNull(source.getRoleBased(), param -> response.setCodeGrantFlowBased(getRoleBased(param)));
        response.setSubscriptionId(source.getSubscriptionId());
        response.setTenantId(source.getTenantId());
        return response;
    }

    public AzureCredentialResponseParameters convert(AzureCredentialAttributes source) {
        AzureCredentialResponseParameters response = new AzureCredentialResponseParameters();
        doIfNotNull(source.getCodeGrantFlowBased(), param -> response.setRoleBased(getRoleBased(param)));

        doIfNotNull(source.getAppBased(), param -> response.setAccessKey(param.getAccessKey()));
        doIfNotNull(source.getCodeGrantFlowBased(), param -> response.setAccessKey(param.getAccessKey()));

        response.setSubscriptionId(source.getSubscriptionId());
        response.setTenantId(source.getTenantId());
        return response;
    }

    private RoleBasedResponse getRoleBased(CodeGrantFlowAttributes roleBased) {
        RoleBasedResponse response = new RoleBasedResponse();
        response.setAppObjectId(roleBased.getAppObjectId());
        response.setCodeGrantFlow(true);
        response.setDeploymentAddress(roleBased.getDeploymentAddress());
        response.setSpDisplayName(roleBased.getSpDisplayName());
        return response;
    }

    private CodeGrantFlowAttributes getRoleBased(RoleBasedRequest roleBased) {
        CodeGrantFlowAttributes response = new CodeGrantFlowAttributes();
        response.setDeploymentAddress(roleBased.getDeploymentAddress());
        return response;
    }

    private AppBasedAttributes getAppBased(AppBasedRequest appBased) {
        AppBasedAttributes response = new AppBasedAttributes();
        response.setAccessKey(appBased.getAccessKey());
        response.setSecretKey(appBased.getSecretKey());
        return response;
    }
}
