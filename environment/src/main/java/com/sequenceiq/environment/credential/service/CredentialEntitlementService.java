package com.sequenceiq.environment.credential.service;

import static com.sequenceiq.cloudbreak.auth.altus.model.Entitlement.CDP_AZURE_CERTIFICATE_AUTH;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.common.api.credential.AppAuthenticationType;
import com.sequenceiq.environment.api.v1.credential.model.parameters.azure.AzureCredentialRequestParameters;

@Service
public class CredentialEntitlementService {

    private static final Logger LOGGER = LoggerFactory.getLogger(CredentialEntitlementService.class);

    @Inject
    private EntitlementService entitlementService;

    public void checkAzureEntitlement(String accountId, AzureCredentialRequestParameters azureCredentialRequestParameters) {
        if (azureCredentialRequestParameters != null && azureCredentialRequestParameters.getAppBased() != null) {
            if (AppAuthenticationType.CERTIFICATE == azureCredentialRequestParameters.getAppBased().getAuthenticationType()) {
                if (!entitlementService.isAzureCertificateAuthEnabled(accountId)) {
                    throw new IllegalStateException("You are not entitled to use certificate based authentication for your Azure credential. " +
                            "Please contact Cloudera to enable " + CDP_AZURE_CERTIFICATE_AUTH + " for your account");
                }
            }
        }
    }
}
