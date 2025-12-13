package com.sequenceiq.cloudbreak.cloud.azure.validator;

import static com.sequenceiq.cloudbreak.cloud.PlatformParametersConsts.ACCEPTANCE_POLICY_PARAMETER;
import static com.sequenceiq.cloudbreak.cloud.azure.image.marketplace.AzureImageTermStatus.ACCEPTED;
import static com.sequenceiq.cloudbreak.cloud.azure.image.marketplace.AzureImageTermStatus.NON_READABLE;
import static com.sequenceiq.cloudbreak.cloud.azure.image.marketplace.AzureImageTermStatus.NOT_ACCEPTED;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.azure.resourcemanager.resources.models.Subscription;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.cloud.azure.client.AzureClient;
import com.sequenceiq.cloudbreak.cloud.azure.image.marketplace.AzureImageTermStatus;
import com.sequenceiq.cloudbreak.cloud.azure.image.marketplace.AzureImageTermsSignerService;
import com.sequenceiq.cloudbreak.cloud.azure.image.marketplace.AzureMarketplaceImageProviderService;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.exception.CloudConnectorException;
import com.sequenceiq.cloudbreak.cloud.exception.CloudPlatformValidationWarningException;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.cloud.model.Image;
import com.sequenceiq.cloudbreak.cloud.model.catalog.ImagePackageVersion;

@ExtendWith(MockitoExtension.class)
public class AzureImageFormatValidatorTest {

    private static final String TEST_ACCOUNT_ID = "accid";

    private static final String TEST_USER_CRN = String.format("crn:cdp:iam:us-west-1:%s:user:mockuser@cloudera.com", TEST_ACCOUNT_ID);

    private static final String VALID_IMAGE_NAME = "https://cldrwestus2.blob.core.windows.net/images/cb-cdh-726-210326090153.vhd";

    private static final String INVALID_IMAGE_NAME = "cldrwestus2.blob.core.windows.net/images/cb-cdh-726-210326090153.vhd\"";

    private static final String MARKETPLACE_IMAGE_NAME = "cloudera:cdp-7_2:freeipa:1.0.2103081333";

    private static final String AZURE_SUBSCRIPTION_ID = "azure-subscription-id";

    private static final String FAIL = "Your image " + MARKETPLACE_IMAGE_NAME + " seems to be an Azure Marketplace image, "
            + "however its Terms and Conditions are not accepted! "
            + "Please either enable automatic consent or accept the terms manually and initiate the provisioning or upgrade again. " +
            "On how to accept the Terms and Conditions of the image please refer to azure documentation " +
            "at https://docs.microsoft.com/en-us/cli/azure/vm/image/terms?view=azure-cli-latest.";

    private static final String WARN_NON_READABLE = "Cloudera Management Console does not have sufficient permissions to read if "
            + "Terms and Conditions are accepted for the Azure Marketplace image " + MARKETPLACE_IMAGE_NAME + "."
            + " Please either enable automatic consent or ensure that the terms are already accepted!";

    private static final String WARN_NON_ACCEPTED = "Your image " + MARKETPLACE_IMAGE_NAME + " seems to be an Azure Marketplace image, "
            + "however its Terms and Conditions are not accepted! We will use VHD images for the deployment."
            + "If you would like to use Marketplace images instead, please either enable automatic consent "
            + "or accept the terms manually and initiate the provisioning or upgrade again. " +
            "On how to accept the Terms and Conditions of the image please refer to azure documentation " +
            "at https://docs.microsoft.com/en-us/cli/azure/vm/image/terms?view=azure-cli-latest.";

    private static final String PASS = "pass";

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
        Image image = Image.builder().withImageName(VALID_IMAGE_NAME).build();
        cloudStack = CloudStack.builder()
                .image(image)
                .build();
        when(entitlementService.azureOnlyMarketplaceImagesEnabled(TEST_ACCOUNT_ID)).thenReturn(false);

        ThreadBasedUserCrnProvider.doAs(TEST_USER_CRN, () -> underTest.validate(authenticatedContext, cloudStack));

        verify(entitlementService, times(0)).azureMarketplaceImagesEnabled(any());
        verify(entitlementService, times(1)).azureOnlyMarketplaceImagesEnabled(any());
        verify(azureImageTermsSignerService, never()).getImageTermStatus(eq(AZURE_SUBSCRIPTION_ID), any(), any());
    }

    @Test
    void testVhdImageOnlyAzureMarketplaceImageAllowed() {
        Image image = Image.builder().withImageName(VALID_IMAGE_NAME).build();
        cloudStack = CloudStack.builder()
                .image(image)
                .build();
        when(entitlementService.azureOnlyMarketplaceImagesEnabled(TEST_ACCOUNT_ID)).thenReturn(true);

        CloudConnectorException exception = assertThrows(CloudConnectorException.class,
                () -> ThreadBasedUserCrnProvider.doAs(TEST_USER_CRN, () -> underTest.validate(authenticatedContext, cloudStack)));

        String expected = "Your image https://cldrwestus2.blob.core.windows.net/images/cb-cdh-726-210326090153.vhd seems to be a VHD image, " +
                "but only Azure Marketplace images allowed in your account! " +
                "If you would like to use it please open Cloudera support ticket to enable this capability!";
        assertEquals(expected, exception.getMessage());
        verify(entitlementService, times(0)).azureMarketplaceImagesEnabled(any());
        verify(azureImageTermsSignerService, never()).getImageTermStatus(eq(AZURE_SUBSCRIPTION_ID), any(), any());
    }

    @Test
    void testImageHasValidMarketplaceFormatNoEntitlement() {
        Image image = Image.builder().withImageName(MARKETPLACE_IMAGE_NAME).build();
        cloudStack = CloudStack.builder()
                .image(image)
                .build();

        when(entitlementService.azureMarketplaceImagesEnabled(TEST_ACCOUNT_ID)).thenReturn(false);

        assertThrows(CloudConnectorException.class,
                () -> ThreadBasedUserCrnProvider.doAs(TEST_USER_CRN,
                        () -> ThreadBasedUserCrnProvider.doAs(TEST_USER_CRN, () -> underTest.validate(authenticatedContext, cloudStack))));
        verify(azureImageTermsSignerService, never()).getImageTermStatus(eq(AZURE_SUBSCRIPTION_ID), any(), any());
    }

    @Test
    void testImageHasValidMarketplaceFormatEntitlementTermsAccepted() {
        Image image = Image.builder().withImageName(MARKETPLACE_IMAGE_NAME).build();
        cloudStack = CloudStack.builder()
                .image(image)
                .build();

        setupAuthenticatedContext();
        when(entitlementService.azureMarketplaceImagesEnabled(TEST_ACCOUNT_ID)).thenReturn(true);
        when(azureImageTermsSignerService.getImageTermStatus(anyString(), any(), any())).thenReturn(AzureImageTermStatus.ACCEPTED);

        ThreadBasedUserCrnProvider.doAs(TEST_USER_CRN, () -> underTest.validate(authenticatedContext, cloudStack));

        verify(entitlementService, times(1)).azureMarketplaceImagesEnabled(any());
        verify(azureImageTermsSignerService).getImageTermStatus(eq(AZURE_SUBSCRIPTION_ID), any(), any());
    }

    @Test
    void testImageHasValidMarketplaceFormatEntitlementNoTermsAccepted() {
        Image image = Image.builder().withImageName(MARKETPLACE_IMAGE_NAME).build();
        cloudStack = CloudStack.builder()
                .image(image)
                .build();
        setupAuthenticatedContext();
        when(entitlementService.azureMarketplaceImagesEnabled(TEST_ACCOUNT_ID)).thenReturn(true);
        when(azureImageTermsSignerService.getImageTermStatus(anyString(), any(), any())).thenReturn(NOT_ACCEPTED);

        CloudPlatformValidationWarningException exception = assertThrows(CloudPlatformValidationWarningException.class,
                () -> ThreadBasedUserCrnProvider.doAs(TEST_USER_CRN, () -> underTest.validate(authenticatedContext, cloudStack)));

        assertEquals(WARN_NON_ACCEPTED, exception.getMessage());
        verify(entitlementService, times(1)).azureMarketplaceImagesEnabled(any());
        verify(azureImageTermsSignerService).getImageTermStatus(eq(AZURE_SUBSCRIPTION_ID), any(), any());
    }

    @ParameterizedTest
    @MethodSource("marketplaceSettingsDataProvider")
    void testMarketplaceImageScenarios(boolean marketplaceOnly, boolean autoAccept, AzureImageTermStatus termStatus, String expectedResult) {
        Image image = Image.builder().withImageName(MARKETPLACE_IMAGE_NAME).build();
        cloudStack = CloudStack.builder()
                .image(image)
                .parameters(Map.of(ACCEPTANCE_POLICY_PARAMETER, Boolean.toString(autoAccept)))
                .build();
        setupAuthenticatedContext();

        when(entitlementService.azureMarketplaceImagesEnabled(TEST_ACCOUNT_ID)).thenReturn(true);
        when(entitlementService.azureOnlyMarketplaceImagesEnabled(TEST_ACCOUNT_ID)).thenReturn(marketplaceOnly);
        when(azureImageTermsSignerService.getImageTermStatus(anyString(), any(), any())).thenReturn(termStatus);
        RuntimeException exception;

        switch (expectedResult) {
            case FAIL -> {
                exception = assertThrows(CloudConnectorException.class,
                        () -> ThreadBasedUserCrnProvider.doAs(TEST_USER_CRN, () -> underTest.validate(authenticatedContext, cloudStack)));
                assertEquals(FAIL, exception.getMessage());
            }
            case WARN_NON_ACCEPTED -> {
                exception = assertThrows(CloudPlatformValidationWarningException.class,
                        () -> ThreadBasedUserCrnProvider.doAs(TEST_USER_CRN, () -> underTest.validate(authenticatedContext, cloudStack)));
                assertEquals(WARN_NON_ACCEPTED, exception.getMessage());
            }
            case WARN_NON_READABLE -> {
                exception = assertThrows(CloudPlatformValidationWarningException.class,
                        () -> ThreadBasedUserCrnProvider.doAs(TEST_USER_CRN, () -> underTest.validate(authenticatedContext, cloudStack)));
                assertEquals(WARN_NON_READABLE, exception.getMessage());
            }
            case PASS -> ThreadBasedUserCrnProvider.doAs(TEST_USER_CRN, () -> underTest.validate(authenticatedContext, cloudStack));
            default -> {
            }
            // no-op, needed for checkstyle
        }

        verify(entitlementService, times(1)).azureMarketplaceImagesEnabled(any());
        verify(entitlementService, times(1)).azureOnlyMarketplaceImagesEnabled(any());
        verify(azureImageTermsSignerService).getImageTermStatus(eq(AZURE_SUBSCRIPTION_ID), any(), any());
    }

    static Object[][] marketplaceSettingsDataProvider() {
        return new Object[][]{
                // CDP_AZURE_IMAGE_MARKETPLACE_ONLY, auto-accept, term status, expected result
                {true,                               false,       NOT_ACCEPTED, FAIL},
                {true,                               false,       NON_READABLE, WARN_NON_READABLE},
                {true,                               false,       ACCEPTED,     PASS},
                {true,                               true,        NOT_ACCEPTED, PASS},
                {true,                               true,        NON_READABLE, PASS},
                {true,                               true,        ACCEPTED,     PASS},
                {false,                              false,       NOT_ACCEPTED, WARN_NON_ACCEPTED},
                {false,                              false,       NON_READABLE, PASS},
                {false,                              false,       ACCEPTED,     PASS},
                {false,                              true,        NOT_ACCEPTED, PASS},
                {false,                              true,        NON_READABLE, PASS},
                {false,                              true,        ACCEPTED,     PASS},
        };
    }

    @Test
    void testImageHasInvalidFormat() {
        Image image = Image.builder().withImageName(INVALID_IMAGE_NAME).build();
        cloudStack = CloudStack.builder()
                .image(image)
                .build();

        CloudConnectorException exception = assertThrows(CloudConnectorException.class,
                () -> ThreadBasedUserCrnProvider.doAs(TEST_USER_CRN, () -> underTest.validate(authenticatedContext, cloudStack)));
        String expected = "Your image name cldrwestus2.blob.core.windows.net/images/cb-cdh-726-210326090153.vhd\" is invalid. " +
                "Please check the desired format in the documentation!";
        assertEquals(expected, exception.getMessage());
    }

    @Test
    void testMarketplaceImageUri() {
        boolean actualResult = underTest.isMarketplaceImageFormat(MARKETPLACE_IMAGE_NAME);
        assertTrue(actualResult);
    }

    @Test
    void testWrongMarketplaceImageUri() {
        boolean actualResult = underTest.isMarketplaceImageFormat(INVALID_IMAGE_NAME);
        assertFalse(actualResult);
    }

    @Test
    void testMarketplaceImage() {
        Image image = Image.builder().withImageName(MARKETPLACE_IMAGE_NAME).build();
        boolean actualResult = underTest.isMarketplaceImageFormat(image);
        assertTrue(actualResult);
    }

    @Test
    void testWrongMarketplaceImage() {
        Image image = Image.builder().withImageName(INVALID_IMAGE_NAME).build();
        boolean actualResult = underTest.isMarketplaceImageFormat(image);
        assertFalse(actualResult);
    }

    @Test
    void testVhdImage() {
        Image image = Image.builder().withImageName(VALID_IMAGE_NAME).build();
        boolean actualResult = underTest.isVhdImageFormat(image);
        assertTrue(actualResult);
    }

    @Test
    void testWrongVhdImage() {
        Image image = Image.builder().withImageName(INVALID_IMAGE_NAME).build();
        boolean actualResult = underTest.isVhdImageFormat(image);
        assertFalse(actualResult);
    }

    @ParameterizedTest
    @MethodSource("sourceImagePlanDataProvider")
    void testSourceImagePlan(String urn, Boolean result) {
        Map<String, String> packageVersions = new HashMap<>();
        packageVersions.put(ImagePackageVersion.SOURCE_IMAGE.getKey(), urn);
        Image image = Image.builder().withImageName(INVALID_IMAGE_NAME).withPackageVersions(packageVersions).build();
        try {
            boolean actualResult = underTest.hasSourceImagePlan(image);
            assertEquals(result, actualResult);
        } catch (CloudConnectorException e) {
            assertNull(result);
            assertEquals("Your image name cloudera:cloudera-data-platform-cdp-7_2:runtime-7_2_15 is invalid. "
                            + "Please check the desired format in the documentation!", e.getMessage());
        }
    }

    static Object[][] sourceImagePlanDataProvider() {
        return new Object[][]{
                // source-image,                                                                 expected result
                {"cloudera:cloudera-data-platform-cdp-7_2:runtime-7_2_15:0.26459873.1652167162", Boolean.TRUE},
                {"cloudera:cloudera-data-platform-cdp-7_2:runtime-7_2_15",                       null},
                {"",                                                                             Boolean.FALSE},
                {null,                                                                           Boolean.FALSE},
        };
    }

    private void setupAuthenticatedContext() {
        AzureClient azureClient = mock(AzureClient.class);
        Subscription subscription = mock(Subscription.class);
        when(subscription.subscriptionId()).thenReturn(AZURE_SUBSCRIPTION_ID);
        when(azureClient.getCurrentSubscription()).thenReturn(subscription);
        when(authenticatedContext.getParameter(AzureClient.class)).thenReturn(azureClient);
    }

}