package com.sequenceiq.cloudbreak.cloud.azure.validator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.azure.resourcemanager.resources.models.Subscription;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.cloud.azure.client.AzureClient;
import com.sequenceiq.cloudbreak.cloud.azure.image.marketplace.AzureImageTermsSignerService;
import com.sequenceiq.cloudbreak.cloud.azure.image.marketplace.AzureMarketplaceImageProviderService;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.exception.CloudConnectorException;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.cloud.model.Image;

@ExtendWith(MockitoExtension.class)
public class AzureImageFormatValidatorTest {

    private static final String TEST_ACCOUNT_ID = "accid";

    private static final String TEST_USER_CRN = String.format("crn:cdp:iam:us-west-1:%s:user:mockuser@cloudera.com", TEST_ACCOUNT_ID);

    private static final String VALID_IMAGE_NAME = "https://cldrwestus2.blob.core.windows.net/images/cb-cdh-726-210326090153.vhd";

    private static final String INVALID_IMAGE_NAME = "cldrwestus2.blob.core.windows.net/images/cb-cdh-726-210326090153.vhd\"";

    private static final String MARKETPLACE_IMAGE_NAME = "cloudera:cdp-7_2:freeipa:1.0.2103081333";

    private static final String AZURE_SUBSCRIPTION_ID = "azure-subscription-id";

    @Mock
    private AzureMarketplaceImageProviderService azureMarketplaceImageProviderService;

    @Mock
    private EntitlementService entitlementService;

    @Mock
    private AuthenticatedContext authenticatedContext;

    @Mock
    private AzureImageTermsSignerService azureImageTermsSignerService;

    @InjectMocks
    private AzureImageFormatValidator underTest;

    private CloudStack cloudStack;

    @Test
    void testImageHasValidVhdFormat() {
        Image image = new Image(VALID_IMAGE_NAME, new HashMap<>(), "centos7", "redhat7", "", "default", "default-id", new HashMap<>());
        cloudStack = new CloudStack(List.of(), null, image, Map.of(), Map.of(), null, null, null, null, null);
        when(entitlementService.azureOnlyMarketplaceImagesEnabled(TEST_ACCOUNT_ID)).thenReturn(false);

        ThreadBasedUserCrnProvider.doAs(TEST_USER_CRN, () -> underTest.validate(authenticatedContext, cloudStack));

        verify(entitlementService, times(0)).azureMarketplaceImagesEnabled(any());
        verify(azureImageTermsSignerService, never()).isSigned(eq(AZURE_SUBSCRIPTION_ID), any(), any());
    }

    @Test
    void testVhdImageOnlyAzureMarketplaceImageAllowed() {
        Image image = new Image(VALID_IMAGE_NAME, new HashMap<>(), "centos7", "redhat7", "", "default", "default-id", new HashMap<>());
        cloudStack = new CloudStack(List.of(), null, image, Map.of(), Map.of(), null, null, null, null, null);
        when(entitlementService.azureOnlyMarketplaceImagesEnabled(TEST_ACCOUNT_ID)).thenReturn(true);

        CloudConnectorException exception = Assertions.assertThrows(CloudConnectorException.class,
                () -> ThreadBasedUserCrnProvider.doAs(TEST_USER_CRN, () -> underTest.validate(authenticatedContext, cloudStack)));

        String expected = "Your image https://cldrwestus2.blob.core.windows.net/images/cb-cdh-726-210326090153.vhd seems to be a VHD image, " +
                "but only Azure Marketplace images allowed in your account! " +
                "If you would like to use it please open Cloudera support ticket to enable this capability!";
        assertEquals(expected, exception.getMessage());
        verify(entitlementService, times(0)).azureMarketplaceImagesEnabled(any());
        verify(azureImageTermsSignerService, never()).isSigned(eq(AZURE_SUBSCRIPTION_ID), any(), any());
    }

    @Test
    void testImageHasValidMarketplaceFormatNoEntitlement() {
        Image image = new Image(MARKETPLACE_IMAGE_NAME, new HashMap<>(), "centos7", "redhat7", "", "default",
                "default-id", new HashMap<>());
        cloudStack = new CloudStack(List.of(), null, image, Map.of(), Map.of(), null, null, null, null, null);

        when(entitlementService.azureMarketplaceImagesEnabled(TEST_ACCOUNT_ID)).thenReturn(false);

        Assertions.assertThrows(CloudConnectorException.class,
                () -> ThreadBasedUserCrnProvider.doAs(TEST_USER_CRN,
                        () -> ThreadBasedUserCrnProvider.doAs(TEST_USER_CRN, () -> underTest.validate(authenticatedContext, cloudStack))));
        verify(azureImageTermsSignerService, never()).isSigned(eq(AZURE_SUBSCRIPTION_ID), any(), any());
    }

    @Test
    void testImageHasValidMarketplaceFormatEntitlement() {
        Image image = new Image(MARKETPLACE_IMAGE_NAME, new HashMap<>(), "centos7", "redhat7", "", "default",
                "default-id", new HashMap<>());
        cloudStack = new CloudStack(List.of(), null, image, Map.of(), Map.of(), null, null, null, null, null);

        setupAuthenticatedContext();
        when(entitlementService.azureMarketplaceImagesEnabled(TEST_ACCOUNT_ID)).thenReturn(true);
        when(azureImageTermsSignerService.isSigned(anyString(), any(), any())).thenReturn(true);

        ThreadBasedUserCrnProvider.doAs(TEST_USER_CRN, () -> underTest.validate(authenticatedContext, cloudStack));

        verify(entitlementService, times(1)).azureMarketplaceImagesEnabled(any());
        verify(azureImageTermsSignerService).isSigned(eq(AZURE_SUBSCRIPTION_ID), any(), any());
    }

    @Test
    void testImageHasValidMarketplaceFormatEntitlementNoTermsAccepted() {
        Image image = new Image(MARKETPLACE_IMAGE_NAME, new HashMap<>(), "centos7", "redhat7", "", "default",
                "default-id", new HashMap<>());
        cloudStack = new CloudStack(List.of(), null, image, Map.of(), Map.of(), null, null, null, null, null);
        setupAuthenticatedContext();
        when(entitlementService.azureMarketplaceImagesEnabled(TEST_ACCOUNT_ID)).thenReturn(true);
        when(azureImageTermsSignerService.isSigned(anyString(), any(), any())).thenReturn(false);

        CloudConnectorException exception = Assertions.assertThrows(CloudConnectorException.class,
                () -> ThreadBasedUserCrnProvider.doAs(TEST_USER_CRN, () -> underTest.validate(authenticatedContext, cloudStack)));

        assertEquals("Your image cloudera:cdp-7_2:freeipa:1.0.2103081333 seems to be an Azure Marketplace image, however " +
                        "its Terms and Conditions are not accepted! Please accept them and retry the provisioning or upgrade. " +
                        "On how to accept the Terms and Conditions of the image " +
                        "please refer to azure documentation at https://docs.microsoft.com/en-us/cli/azure/vm/image/terms?view=azure-cli-latest.",
                exception.getMessage());
        verify(entitlementService, times(1)).azureMarketplaceImagesEnabled(any());
        verify(azureImageTermsSignerService).isSigned(eq(AZURE_SUBSCRIPTION_ID), any(), any());
    }

    @Test
    void testImageHasInvalidFormat() {
        Image image = new Image(INVALID_IMAGE_NAME, new HashMap<>(), "centos7", "redhat7", "", "default",
                "default-id", new HashMap<>());
        cloudStack = new CloudStack(List.of(), null, image, Map.of(), Map.of(), null, null, null, null, null);

        CloudConnectorException exception = Assertions.assertThrows(CloudConnectorException.class,
                () -> ThreadBasedUserCrnProvider.doAs(TEST_USER_CRN, () -> underTest.validate(authenticatedContext, cloudStack)));
        String expected = "Your image name cldrwestus2.blob.core.windows.net/images/cb-cdh-726-210326090153.vhd\" is invalid. " +
                "Please check the desired format in the documentation!";
        assertEquals(expected, exception.getMessage());
    }

    private void setupAuthenticatedContext() {
        AzureClient azureClient = mock(AzureClient.class);
        Subscription subscription = mock(Subscription.class);
        when(subscription.subscriptionId()).thenReturn(AZURE_SUBSCRIPTION_ID);
        when(azureClient.getCurrentSubscription()).thenReturn(subscription);
        when(authenticatedContext.getParameter(AzureClient.class)).thenReturn(azureClient);
    }

}