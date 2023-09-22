package com.sequenceiq.environment.credential.v1.converter.azure;

import static com.sequenceiq.cloudbreak.util.NullUtil.doIfNotNull;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.base64.Base64Util;
import com.sequenceiq.environment.api.v1.credential.model.parameters.azure.AzureCredentialCertificateResponse;
import com.sequenceiq.environment.api.v1.credential.model.parameters.azure.AzureCredentialResponseParameters;
import com.sequenceiq.environment.api.v1.credential.model.parameters.azure.RoleBasedResponse;
import com.sequenceiq.environment.credential.attributes.azure.AzureCredentialAttributes;
import com.sequenceiq.environment.credential.attributes.azure.AzureCredentialCertificate;
import com.sequenceiq.environment.credential.attributes.azure.CodeGrantFlowAttributes;

@Component
public class AzureCredentialAttributesToAzureCredentialResponseParametersConverter {

    public AzureCredentialResponseParameters convert(AzureCredentialAttributes source) {
        AzureCredentialResponseParameters response = new AzureCredentialResponseParameters();
        doIfNotNull(source.getCodeGrantFlowBased(), param -> response.setRoleBased(getRoleBased(param)));

        doIfNotNull(source.getAppBased(), param -> response.setAccessKey(param.getAccessKey()));
        doIfNotNull(source.getAppBased(), param -> response.setAuthenticationType(param.getAuthenticationType()));
        doIfNotNull(source.getAppBased(), param -> response.setCertificate(convert(param.getCertificate())));
        doIfNotNull(source.getCodeGrantFlowBased(), param -> response.setAccessKey(param.getAccessKey()));

        response.setSubscriptionId(source.getSubscriptionId());
        response.setTenantId(source.getTenantId());
        return response;
    }

    private AzureCredentialCertificateResponse convert(AzureCredentialCertificate certificate) {
        AzureCredentialCertificateResponse certificateResponse = null;
        if (certificate != null) {
            certificateResponse = new AzureCredentialCertificateResponse();
            certificateResponse.setBase64(Base64Util.encode(certificate.getCertificate()));
            certificateResponse.setSha512(certificate.getSha512());
            certificateResponse.setExpiration(certificate.getExpiration());
            certificateResponse.setStatus(certificate.getStatus());
        }
        return certificateResponse;
    }

    private RoleBasedResponse getRoleBased(CodeGrantFlowAttributes roleBased) {
        RoleBasedResponse response = new RoleBasedResponse();
        response.setAppObjectId(roleBased.getAppObjectId());
        response.setCodeGrantFlow(true);
        response.setDeploymentAddress(roleBased.getDeploymentAddress());
        response.setSpDisplayName(roleBased.getSpDisplayName());
        return response;
    }
}
