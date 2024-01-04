package com.sequenceiq.environment.credential.v1.converter.azure;

import static com.sequenceiq.cloudbreak.util.NullUtil.doIfNotNull;

import java.util.Optional;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.common.api.credential.AppAuthenticationType;
import com.sequenceiq.common.api.credential.AppCertificateStatus;
import com.sequenceiq.environment.api.v1.credential.model.parameters.azure.AppBasedRequest;
import com.sequenceiq.environment.api.v1.credential.model.parameters.azure.AzureCredentialRequestParameters;
import com.sequenceiq.environment.api.v1.credential.model.parameters.azure.RoleBasedRequest;
import com.sequenceiq.environment.credential.attributes.CredentialAttributes;
import com.sequenceiq.environment.credential.attributes.azure.AppBasedAttributes;
import com.sequenceiq.environment.credential.attributes.azure.AzureCredentialAttributes;
import com.sequenceiq.environment.credential.attributes.azure.AzureCredentialCertificate;
import com.sequenceiq.environment.credential.attributes.azure.CodeGrantFlowAttributes;
import com.sequenceiq.environment.credential.service.AzureCredentialCertificateService;

@Component
public class AzureCredentialRequestParametersToAzureCredentialAttributesConverter {

    private static final Logger LOGGER = LoggerFactory.getLogger(AzureCredentialRequestParametersToAzureCredentialAttributesConverter.class);

    @Inject
    private AzureCredentialCertificateService azureCredentialCertificateService;

    public AzureCredentialAttributes convertCreate(AzureCredentialRequestParameters source) {
        AzureCredentialAttributes response = new AzureCredentialAttributes();
        doIfNotNull(source.getAppBased(), param -> response.setAppBased(getAppBased(param, null, true)));
        doIfNotNull(source.getRoleBased(), param -> response.setCodeGrantFlowBased(getRoleBased(param)));
        response.setSubscriptionId(source.getSubscriptionId());
        response.setTenantId(source.getTenantId());
        return response;
    }

    public AzureCredentialAttributes convertModify(AzureCredentialRequestParameters source, CredentialAttributes originalAttributes) {
        AzureCredentialAttributes response = new AzureCredentialAttributes();
        doIfNotNull(source.getAppBased(), param -> response.setAppBased(getAppBased(param, originalAttributes,
                Optional.ofNullable(param.getGenerateCertificate()).orElse(false))));
        doIfNotNull(source.getRoleBased(), param -> response.setCodeGrantFlowBased(getRoleBased(param)));
        response.setSubscriptionId(source.getSubscriptionId());
        response.setTenantId(source.getTenantId());
        return response;
    }

    private CodeGrantFlowAttributes getRoleBased(RoleBasedRequest roleBased) {
        CodeGrantFlowAttributes response = new CodeGrantFlowAttributes();
        response.setDeploymentAddress(roleBased.getDeploymentAddress());
        return response;
    }

    private AppBasedAttributes getAppBased(AppBasedRequest appBasedRequest, CredentialAttributes originalAttributes, boolean generateCertificate) {
        AppBasedAttributes appBasedAttributes = new AppBasedAttributes();
        appBasedAttributes.setAccessKey(appBasedRequest.getAccessKey());

        AppAuthenticationType authenticationType = appBasedRequest.getAuthenticationType() == null ?
                AppAuthenticationType.SECRET : appBasedRequest.getAuthenticationType();
        appBasedAttributes.setAuthenticationType(authenticationType);

        if (authenticationType == AppAuthenticationType.SECRET) {
            appBasedAttributes.setSecretKey(appBasedRequest.getSecretKey());
        } else if (authenticationType == AppAuthenticationType.CERTIFICATE && generateCertificate) {
            appBasedAttributes.setCertificate(azureCredentialCertificateService.generate());
        } else if (authenticationType == AppAuthenticationType.CERTIFICATE && originalAttributes != null) {
            AzureCredentialCertificate certificate = originalAttributes.getAzure().getAppBased().getCertificate();
            certificate.setStatus(AppCertificateStatus.ACTIVE);
            appBasedAttributes.setCertificate(originalAttributes.getAzure().getAppBased().getCertificate());
        }

        return appBasedAttributes;
    }
}
