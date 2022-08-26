package com.sequenceiq.environment.credential.v1.converter.azure;

import static com.sequenceiq.cloudbreak.util.NullUtil.doIfNotNull;

import java.util.Base64;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.common.api.credential.AppAuthenticationType;
import com.sequenceiq.environment.api.v1.credential.model.parameters.azure.AppBasedRequest;
import com.sequenceiq.environment.api.v1.credential.model.parameters.azure.AzureCredentialCertificateResponse;
import com.sequenceiq.environment.api.v1.credential.model.parameters.azure.AzureCredentialRequestParameters;
import com.sequenceiq.environment.api.v1.credential.model.parameters.azure.AzureCredentialResponseParameters;
import com.sequenceiq.environment.api.v1.credential.model.parameters.azure.RoleBasedRequest;
import com.sequenceiq.environment.api.v1.credential.model.parameters.azure.RoleBasedResponse;
import com.sequenceiq.environment.credential.attributes.azure.AppBasedAttributes;
import com.sequenceiq.environment.credential.attributes.azure.AzureCredentialAttributes;
import com.sequenceiq.environment.credential.attributes.azure.AzureCredentialCertificate;
import com.sequenceiq.environment.credential.attributes.azure.CodeGrantFlowAttributes;
import com.sequenceiq.environment.credential.service.AzureCredentialCertificateService;

@Component
public class AzureCredentialV1ParametersToAzureCredentialAttributesConverter {

    @Inject
    private AzureCredentialCertificateService azureCredentialCertificateService;

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
            certificateResponse.setId(certificate.getId());
            certificateResponse.setBase64(Base64.getEncoder().encodeToString(certificate.getCertificate().getBytes()));
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

    private CodeGrantFlowAttributes getRoleBased(RoleBasedRequest roleBased) {
        CodeGrantFlowAttributes response = new CodeGrantFlowAttributes();
        response.setDeploymentAddress(roleBased.getDeploymentAddress());
        return response;
    }

    private AppBasedAttributes getAppBased(AppBasedRequest appBased) {
        AppBasedAttributes response = new AppBasedAttributes();
        response.setAccessKey(appBased.getAccessKey());
        AppAuthenticationType authenticationType = appBased.getAuthenticationType() == null ? AppAuthenticationType.SECRET : appBased.getAuthenticationType();
        response.setAuthenticationType(authenticationType);
        if (authenticationType == AppAuthenticationType.SECRET) {
            response.setSecretKey(appBased.getSecretKey());
        } else {
            AzureCredentialCertificate cert = azureCredentialCertificateService.generate();
            response.setCertificate(cert);
        }
        return response;
    }
}
