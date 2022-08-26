package com.sequenceiq.environment.credential.service;


import static com.sequenceiq.common.api.credential.AppAuthenticationType.CERTIFICATE;
import static com.sequenceiq.common.api.credential.AppAuthenticationType.SECRET;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.environment.api.v1.credential.model.parameters.azure.AppBasedRequest;
import com.sequenceiq.environment.api.v1.credential.model.parameters.azure.AzureCredentialRequestParameters;

class CredentialEntitlementServiceTest {

    private static final String ACCOUNT_ID = "anAccountID";

    @Mock
    private EntitlementService entitlementService;

    @InjectMocks
    private CredentialEntitlementService underTest;

    @BeforeEach
    void setUp() throws InterruptedException {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void checkAzureEntitlementEmpty() {
        AzureCredentialRequestParameters azureCredentialRequestParameters = new AzureCredentialRequestParameters();

        when(entitlementService.isAzureCertificateAuthEnabled(anyString())).thenReturn(Boolean.FALSE);

        underTest.checkAzureEntitlement(ACCOUNT_ID, azureCredentialRequestParameters);

        verifyNoInteractions(entitlementService);
    }

    @Test
    void checkAzureEntitlementSecret() {
        AzureCredentialRequestParameters azureCredentialRequestParameters = new AzureCredentialRequestParameters();
        AppBasedRequest appBasedRequest = new AppBasedRequest();
        appBasedRequest.setAuthenticationType(SECRET);
        azureCredentialRequestParameters.setAppBased(appBasedRequest);

        when(entitlementService.isAzureCertificateAuthEnabled(anyString())).thenReturn(Boolean.FALSE);

        underTest.checkAzureEntitlement(ACCOUNT_ID, azureCredentialRequestParameters);

        verifyNoInteractions(entitlementService);
    }

    @Test
    void checkAzureEntitlementCertificate() {
        AzureCredentialRequestParameters azureCredentialRequestParameters = new AzureCredentialRequestParameters();
        AppBasedRequest appBasedRequest = new AppBasedRequest();
        appBasedRequest.setAuthenticationType(CERTIFICATE);
        azureCredentialRequestParameters.setAppBased(appBasedRequest);

        when(entitlementService.isAzureCertificateAuthEnabled(anyString())).thenReturn(Boolean.FALSE);

        Exception exception = assertThrows(IllegalStateException.class, () -> {
            underTest.checkAzureEntitlement(ACCOUNT_ID, azureCredentialRequestParameters);
        });

        assertEquals("You are not entitled to use certificate based authentication for your Azure credential. " +
                "Please contact Cloudera to enable CDP_AZURE_CERTIFICATE_AUTH for your account", exception.getMessage());

        verify(entitlementService, times(1)).isAzureCertificateAuthEnabled(anyString());
    }
}