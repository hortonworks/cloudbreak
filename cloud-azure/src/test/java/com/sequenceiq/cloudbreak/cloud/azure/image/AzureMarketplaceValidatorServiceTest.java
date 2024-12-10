package com.sequenceiq.cloudbreak.cloud.azure.image;

import static com.sequenceiq.cloudbreak.cloud.PlatformParametersConsts.ACCEPTANCE_POLICY_PARAMETER;
import static com.sequenceiq.cloudbreak.cloud.azure.AzureDeploymentMarketplaceError.MARKETPLACE_PURCHASE_ELIGIBILITY_FAILED;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.azure.core.http.HttpResponse;
import com.azure.core.management.exception.ManagementError;
import com.azure.core.management.exception.ManagementException;
import com.azure.resourcemanager.resources.models.Subscription;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.cloud.azure.AzureTestUtils;
import com.sequenceiq.cloudbreak.cloud.azure.client.AzureClient;
import com.sequenceiq.cloudbreak.cloud.azure.image.marketplace.AzureImageTermStatus;
import com.sequenceiq.cloudbreak.cloud.azure.image.marketplace.AzureImageTermsSignerService;
import com.sequenceiq.cloudbreak.cloud.azure.image.marketplace.AzureMarketplaceImage;
import com.sequenceiq.cloudbreak.cloud.azure.image.marketplace.AzureMarketplaceImageProviderService;
import com.sequenceiq.cloudbreak.cloud.azure.template.AzureTemplateDeploymentService;
import com.sequenceiq.cloudbreak.cloud.azure.util.AzureExceptionHandler;
import com.sequenceiq.cloudbreak.cloud.azure.validator.AzureImageFormatValidator;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.exception.CloudConnectorException;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.cloud.model.Image;
import com.sequenceiq.cloudbreak.cloud.model.catalog.PrepareImageType;
import com.sequenceiq.cloudbreak.validation.ValidationResult;

@ExtendWith(MockitoExtension.class)
class AzureMarketplaceValidatorServiceTest {

    private static final String USER_CRN = "crn:cdp:iam:us-west-1:1234:user:1";

    private static final String SUBSCRIPTION_ID = "subscription-id";

    @Mock
    private AzureTemplateDeploymentService azureTemplateDeploymentService;

    @Mock
    private EntitlementService entitlementService;

    @Mock
    private AzureImageFormatValidator azureImageFormatValidator;

    @Mock
    private AzureImageTermsSignerService azureImageTermsSignerService;

    @Mock
    private AzureMarketplaceImageProviderService azureMarketplaceImageProviderService;

    @Mock
    private AuthenticatedContext ac;

    @Mock
    private AzureExceptionHandler azureExceptionHandler;

    @Mock
    private Image image;

    @Mock
    private AzureClient client;

    @Mock
    private CloudStack stack;

    @InjectMocks
    private AzureMarketplaceValidatorService azureMarketplaceValidatorService;

    @BeforeEach
    void setup() {
        AzureClient client = mock(AzureClient.class);
        lenient().when(ac.getParameter(AzureClient.class)).thenReturn(client);
        lenient().when(client.getCurrentSubscription()).thenReturn(mock(Subscription.class));
        lenient().when(client.getCurrentSubscription().subscriptionId()).thenReturn(SUBSCRIPTION_ID);
        lenient().when(azureImageTermsSignerService.getImageTermStatus(anyString(), any(), any())).thenReturn(AzureImageTermStatus.ACCEPTED);
    }

    @Test
    void validateMarketplaceImageShouldCopyNonMarketplaceImage() {
        // Given
        Image image = mock(Image.class);
        CloudStack cloudStack = mock(CloudStack.class);

        when(azureImageFormatValidator.isMarketplaceImageFormat(image)).thenReturn(false);

        // When
        MarketplaceValidationResult result = azureMarketplaceValidatorService.validateMarketplaceImage(image, PrepareImageType.EXECUTED_DURING_IMAGE_CHANGE,
                "fallbackTarget", mock(AzureClient.class), cloudStack, ac);

        // Then
        assertFalse(result.isFallbackRequired());
        assertFalse(result.isSkipVhdCopy());
        verify(azureImageFormatValidator).isMarketplaceImageFormat(image);
        verify(azureTemplateDeploymentService, never()).runWhatIfAnalysis(any(), any(), any());
        verify(entitlementService, never()).azureOnlyMarketplaceImagesEnabled(anyString());
        verifyNoMoreInteractions(azureTemplateDeploymentService, entitlementService);
    }

    @Test
    void validateMarketplaceImageShouldSkipExecutedDuringProvision() {
        // Given
        Image image = mock(Image.class);
        CloudStack cloudStack = mock(CloudStack.class);

        when(azureImageFormatValidator.isMarketplaceImageFormat(image)).thenReturn(true);

        // When
        MarketplaceValidationResult result = ThreadBasedUserCrnProvider.doAs(USER_CRN, () ->
                azureMarketplaceValidatorService.validateMarketplaceImage(image, PrepareImageType.EXECUTED_DURING_PROVISIONING,
                "fallbackTarget", mock(AzureClient.class), cloudStack, ac));

        // Then
        assertFalse(result.isFallbackRequired());
        assertTrue(result.isSkipVhdCopy());
        verify(azureImageFormatValidator).isMarketplaceImageFormat(image);
        verify(azureTemplateDeploymentService, never()).runWhatIfAnalysis(any(), any(), any());
        verify(entitlementService, never()).azureOnlyMarketplaceImagesEnabled(anyString());
        verifyNoMoreInteractions(azureTemplateDeploymentService, entitlementService);
    }

    @Test
    void validateMarketplaceImageShouldSkipIfMarketplaceOnlyEntitlementGranted() {
        // Given
        Image image = mock(Image.class);
        CloudStack cloudStack = mock(CloudStack.class);

        when(azureImageFormatValidator.isMarketplaceImageFormat(image)).thenReturn(true);
        when(entitlementService.azureOnlyMarketplaceImagesEnabled(anyString())).thenReturn(true);

        // When
        MarketplaceValidationResult result = ThreadBasedUserCrnProvider.doAs(USER_CRN, () ->
                azureMarketplaceValidatorService.validateMarketplaceImage(image, PrepareImageType.EXECUTED_DURING_IMAGE_CHANGE,
                        "fallbackTarget", mock(AzureClient.class), cloudStack, ac));

        // Then
        assertFalse(result.isFallbackRequired());
        assertTrue(result.isSkipVhdCopy());
        verify(azureImageFormatValidator).isMarketplaceImageFormat(image);
        verify(azureTemplateDeploymentService, never()).runWhatIfAnalysis(any(), any(), any());
        verify(entitlementService).azureOnlyMarketplaceImagesEnabled(anyString());
        verifyNoMoreInteractions(azureTemplateDeploymentService, entitlementService);
    }

    @Test
    void validateMarketplaceImageShouldSkipOnMarketplaceImageWithTermsAlreadyAccepted() {
        // Given
        Image image = mock(Image.class);
        CloudStack cloudStack = mock(CloudStack.class);

        when(azureImageFormatValidator.isMarketplaceImageFormat(image)).thenReturn(true);
        when(azureImageTermsSignerService.getImageTermStatus(any(), any(), any())).thenReturn(AzureImageTermStatus.ACCEPTED);

        // When
        MarketplaceValidationResult result = ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> azureMarketplaceValidatorService.validateMarketplaceImage(
                image, PrepareImageType.EXECUTED_DURING_IMAGE_CHANGE, "fallbackTarget", mock(AzureClient.class), cloudStack, ac));

        // Then
        assertFalse(result.isFallbackRequired());
        assertTrue(result.isSkipVhdCopy());
        verify(azureImageFormatValidator).isMarketplaceImageFormat(image);
        verify(azureImageTermsSignerService).getImageTermStatus(any(), any(), any());
        verify(azureTemplateDeploymentService, never()).runWhatIfAnalysis(any(), any(), any());
        verify(entitlementService).azureOnlyMarketplaceImagesEnabled(anyString());
        verifyNoMoreInteractions(azureTemplateDeploymentService, entitlementService);
    }

    @Test
    void validateMarketplaceImageShouldSignMarketplaceImageWhenNotYetSigned() {
        // Given
        Image image = mock(Image.class);
        CloudStack cloudStack = mock(CloudStack.class);

        when(azureImageFormatValidator.isMarketplaceImageFormat(image)).thenReturn(true);
        when(azureImageTermsSignerService.getImageTermStatus(any(), any(), any())).thenReturn(AzureImageTermStatus.NOT_ACCEPTED);
        when(cloudStack.getParameters()).thenReturn(Map.of(ACCEPTANCE_POLICY_PARAMETER, Boolean.TRUE.toString()));

        // When
        MarketplaceValidationResult result = ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> azureMarketplaceValidatorService.validateMarketplaceImage(
                image, PrepareImageType.EXECUTED_DURING_IMAGE_CHANGE, "fallbackTarget", mock(AzureClient.class), cloudStack, ac));

        // Then
        assertFalse(result.isFallbackRequired());
        assertTrue(result.isSkipVhdCopy());
        verify(azureImageFormatValidator).isMarketplaceImageFormat(image);
        verify(azureImageTermsSignerService).getImageTermStatus(any(), any(), any());
        verify(azureImageTermsSignerService).sign(any(), any(), any());
        verify(azureTemplateDeploymentService).runWhatIfAnalysis(any(), any(), any());
        verify(entitlementService).azureOnlyMarketplaceImagesEnabled(anyString());
        verifyNoMoreInteractions(azureTemplateDeploymentService, entitlementService);
    }

    @Test
    void validateMarketplaceImageShouldSkipOnRandomManagementErrorWithNotAccepted() {
        // Given
        Image image = mock(Image.class);
        CloudStack cloudStack = mock(CloudStack.class);

        when(azureImageFormatValidator.isMarketplaceImageFormat(image)).thenReturn(true);
        when(azureImageTermsSignerService.getImageTermStatus(any(), any(), any())).thenReturn(AzureImageTermStatus.NOT_ACCEPTED);
        when(azureTemplateDeploymentService.runWhatIfAnalysis(any(), any(), any())).thenReturn(Optional.of(getMarketplaceManagementError("random")));

        // When
        MarketplaceValidationResult result = ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> azureMarketplaceValidatorService.validateMarketplaceImage(
                image, PrepareImageType.EXECUTED_DURING_IMAGE_CHANGE,
                "fallbackTarget", mock(AzureClient.class), cloudStack, ac));

        // Then
        assertFalse(result.isFallbackRequired());
        assertTrue(result.isSkipVhdCopy());
        verify(azureImageFormatValidator).isMarketplaceImageFormat(image);
        verify(azureTemplateDeploymentService).runWhatIfAnalysis(any(), any(), any());
        verify(entitlementService).azureOnlyMarketplaceImagesEnabled(anyString());
        verifyNoMoreInteractions(azureTemplateDeploymentService, entitlementService);
    }

    @ParameterizedTest
    @MethodSource("fallbackCombinationProvider")
    void validateMarketplaceImageShouldHandleFallbackOnValidationErrors(boolean hasFallbackImage, String errorCode,
            boolean fallbackRequired, boolean skipVhdCopy) {
        // Given
        Image image = mock(Image.class);
        CloudStack cloudStack = mock(CloudStack.class);
        String fallbackImage = hasFallbackImage ? "fallbackImage" : null;

        when(azureImageFormatValidator.isMarketplaceImageFormat(image)).thenReturn(true);
        when(azureImageTermsSignerService.getImageTermStatus(any(), any(), any())).thenReturn(AzureImageTermStatus.NOT_ACCEPTED);
        when(azureTemplateDeploymentService.runWhatIfAnalysis(any(), any(), any())).thenReturn(Optional.of(
                getMarketplaceManagementError(errorCode)));
        when(entitlementService.azureOnlyMarketplaceImagesEnabled(anyString())).thenReturn(false);

        // When
        MarketplaceValidationResult result = ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> azureMarketplaceValidatorService.validateMarketplaceImage(image,
                PrepareImageType.EXECUTED_DURING_IMAGE_CHANGE, fallbackImage, mock(AzureClient.class), cloudStack, ac));

        assertEquals(fallbackRequired, result.isFallbackRequired());
        assertEquals(skipVhdCopy, result.isSkipVhdCopy());
        verify(azureImageFormatValidator).isMarketplaceImageFormat(image);
        verify(azureTemplateDeploymentService).runWhatIfAnalysis(any(), any(), any());
        verify(entitlementService).azureOnlyMarketplaceImagesEnabled(anyString());
        verifyNoMoreInteractions(azureTemplateDeploymentService, entitlementService);
    }

    @Test
    void validateMarketplaceImageShouldHandleFallbackOnWhatIfPermissionError() {
        // Given
        Image image = mock(Image.class);
        CloudStack cloudStack = mock(CloudStack.class);

        when(azureImageFormatValidator.isMarketplaceImageFormat(image)).thenReturn(true);
        when(azureImageTermsSignerService.getImageTermStatus(any(), any(), any())).thenReturn(AzureImageTermStatus.NOT_ACCEPTED);
        String errorMessage = "Insufficient permission to perform what if analysis, please ensure Microsoft.Resources/deployments/whatIf/action is granted!";
        ManagementException managementException = new ManagementException(errorMessage, mock(HttpResponse.class),
                new ManagementError("unauthorized", errorMessage));
        doThrow(managementException).when(azureTemplateDeploymentService).runWhatIfAnalysis(any(), any(), any());
        when(azureExceptionHandler.isForbidden(eq(managementException))).thenReturn(true);

        // When
        MarketplaceValidationResult result = ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> azureMarketplaceValidatorService.validateMarketplaceImage(image,
                PrepareImageType.EXECUTED_DURING_IMAGE_CHANGE, "fallbackTarget", mock(AzureClient.class), cloudStack, ac));

        assertFalse(result.isFallbackRequired());
        assertTrue(result.isSkipVhdCopy());
        verify(azureImageFormatValidator).isMarketplaceImageFormat(image);
        verify(azureTemplateDeploymentService).runWhatIfAnalysis(any(), any(), any());
        verify(entitlementService).azureOnlyMarketplaceImagesEnabled(anyString());
        verify(azureExceptionHandler).isForbidden(any(ManagementException.class));
        verifyNoMoreInteractions(azureTemplateDeploymentService, entitlementService);
    }

    @Test
    void validateMarketplaceImageShouldHandleFallbackOnWhatIfNonPermissionError() {
        // Given
        Image image = mock(Image.class);
        CloudStack cloudStack = mock(CloudStack.class);

        when(azureImageFormatValidator.isMarketplaceImageFormat(image)).thenReturn(true);
        when(azureImageTermsSignerService.getImageTermStatus(any(), any(), any())).thenReturn(AzureImageTermStatus.NOT_ACCEPTED);
        String errorMessage = "Random management exception happened!";
        ManagementException managementException = new ManagementException(errorMessage, mock(HttpResponse.class),
                new ManagementError("unauthorized", errorMessage));
        doThrow(managementException).when(azureTemplateDeploymentService).runWhatIfAnalysis(any(), any(), any());
        when(azureExceptionHandler.isForbidden(eq(managementException))).thenReturn(false);

        // When
        ManagementException exception = ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> assertThrows(ManagementException.class,
        () -> azureMarketplaceValidatorService.validateMarketplaceImage(image, PrepareImageType.EXECUTED_DURING_IMAGE_CHANGE,
                "fallbackTarget", mock(AzureClient.class), cloudStack, ac)));

        assertEquals("Random management exception happened!", exception.getMessage());
        verify(azureImageFormatValidator).isMarketplaceImageFormat(image);
        verify(azureTemplateDeploymentService).runWhatIfAnalysis(any(), any(), any());
        verify(entitlementService).azureOnlyMarketplaceImagesEnabled(anyString());
        verify(azureExceptionHandler).isForbidden(any(ManagementException.class));
        verifyNoMoreInteractions(azureTemplateDeploymentService, entitlementService);
    }

    @Test
    void validateMarketplaceImageShouldProceedWithImageCopy() {
        // Given
        Image image = mock(Image.class);
        CloudStack cloudStack = mock(CloudStack.class);

        when(azureImageFormatValidator.isMarketplaceImageFormat(image)).thenReturn(true);
        when(azureImageTermsSignerService.getImageTermStatus(any(), any(), any())).thenReturn(AzureImageTermStatus.NOT_ACCEPTED);
        when(azureTemplateDeploymentService.runWhatIfAnalysis(any(), any(), any())).thenReturn(Optional.empty());

        // When
        MarketplaceValidationResult result = ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> azureMarketplaceValidatorService.validateMarketplaceImage(image,
                PrepareImageType.EXECUTED_DURING_IMAGE_CHANGE,
                "fallbackTarget", mock(AzureClient.class), cloudStack, ac));

        // Then
        assertFalse(result.isFallbackRequired());
        assertTrue(result.isSkipVhdCopy());
        verify(azureImageFormatValidator).isMarketplaceImageFormat(image);
        verify(azureTemplateDeploymentService).runWhatIfAnalysis(any(), any(), any());
        verify(entitlementService).azureOnlyMarketplaceImagesEnabled(anyString());
        verifyNoMoreInteractions(azureTemplateDeploymentService, entitlementService);
    }

    private ManagementError getMarketplaceManagementError(String code) {
        ManagementError managementError = AzureTestUtils.managementError(code, "message");
        List<ManagementError> details = new ArrayList<>();
        details.add(AzureTestUtils.managementError("123", "detail1"));
        details.add(AzureTestUtils.managementError("MarketplacePurchaseEligibilityFailed", "message"));
        AzureTestUtils.setDetails(managementError, details);
        return managementError;
    }

    private static Stream<Arguments> fallbackCombinationProvider() {
        return Stream.of(
                // hasFallbackImage, errorCode, fallbackRequired, skipVhdCopy
                Arguments.of(true, MARKETPLACE_PURCHASE_ELIGIBILITY_FAILED.getCode(), true, false),
                Arguments.of(false, MARKETPLACE_PURCHASE_ELIGIBILITY_FAILED.getCode(), false, true),
                Arguments.of(true, "randomCode", false, true),
                Arguments.of(false, "randomCode", false, true));
    }

    @Test
    void testValidateMarketplaceImageTermsImageNotMarketplaceFormat() {
        when(azureImageFormatValidator.isMarketplaceImageFormat(image)).thenReturn(false);

        assertDoesNotThrow(() -> azureMarketplaceValidatorService.validateMarketplaceImageTerms(image, client, stack, ac));

        verify(azureImageFormatValidator).isMarketplaceImageFormat(image);
        verifyNoMoreInteractions(azureImageFormatValidator, azureMarketplaceImageProviderService, azureImageTermsSignerService);
    }

    @Test
    void testValidateMarketplaceImageTermsTermsNonReadable() {
        when(azureImageFormatValidator.isMarketplaceImageFormat(image)).thenReturn(true);
        AzureMarketplaceImage marketplaceImage = mock(AzureMarketplaceImage.class);
        when(azureMarketplaceImageProviderService.get(image)).thenReturn(marketplaceImage);
        AzureClient azureClient = mock(AzureClient.class);
        Subscription currentSubscription = mock(Subscription.class);
        when(azureClient.getCurrentSubscription()).thenReturn(currentSubscription);
        when(currentSubscription.subscriptionId()).thenReturn("sub");
        when(ac.getParameter(AzureClient.class)).thenReturn(azureClient);
        when(azureImageTermsSignerService.getImageTermStatus(
                azureClient.getCurrentSubscription().subscriptionId(), marketplaceImage, azureClient))
                .thenReturn(AzureImageTermStatus.NON_READABLE);

        CloudConnectorException exception = assertThrows(CloudConnectorException.class, () ->
                azureMarketplaceValidatorService.validateMarketplaceImageTerms(image, client, stack, ac));

        assertTrue(exception.getMessage().contains("Cannot read image terms for Azure Marketplace image"));
    }

    @Test
    void testValidateMarketplaceImageTermsTermsNotAcceptedSignFails() {
        when(azureImageFormatValidator.isMarketplaceImageFormat(image)).thenReturn(true);
        AzureMarketplaceImage marketplaceImage = mock(AzureMarketplaceImage.class);
        when(azureMarketplaceImageProviderService.get(image)).thenReturn(marketplaceImage);
        AzureClient azureClient = mock(AzureClient.class);
        Subscription currentSubscription = mock(Subscription.class);
        when(azureClient.getCurrentSubscription()).thenReturn(currentSubscription);
        when(currentSubscription.subscriptionId()).thenReturn("sub");
        when(ac.getParameter(AzureClient.class)).thenReturn(azureClient);
        when(azureImageTermsSignerService.getImageTermStatus(
                azureClient.getCurrentSubscription().subscriptionId(), marketplaceImage, azureClient))
                .thenReturn(AzureImageTermStatus.NOT_ACCEPTED);
        when(stack.getParameters()).thenReturn(Map.of(ACCEPTANCE_POLICY_PARAMETER, "true"));
        when(azureImageTermsSignerService.sign(
                azureClient.getCurrentSubscription().subscriptionId(), marketplaceImage, azureClient))
                .thenReturn(AzureImageTermStatus.NOT_ACCEPTED);

        CloudConnectorException exception = assertThrows(CloudConnectorException.class, () ->
                azureMarketplaceValidatorService.validateMarketplaceImageTerms(image, client, stack, ac));

        assertTrue(exception.getMessage().contains("Cannot accept image terms for Azure Marketplace image"));
    }

    @Test
    void testValidateMarketplaceImageTermsValidationFails() {
        when(azureImageFormatValidator.isMarketplaceImageFormat(image)).thenReturn(true);
        AzureMarketplaceImage marketplaceImage = mock(AzureMarketplaceImage.class);
        when(azureMarketplaceImageProviderService.get(image)).thenReturn(marketplaceImage);
        AzureClient azureClient = mock(AzureClient.class);
        Subscription currentSubscription = mock(Subscription.class);
        when(azureClient.getCurrentSubscription()).thenReturn(currentSubscription);
        when(currentSubscription.subscriptionId()).thenReturn("sub");
        when(ac.getParameter(AzureClient.class)).thenReturn(azureClient);
        when(azureImageTermsSignerService.getImageTermStatus(
                azureClient.getCurrentSubscription().subscriptionId(), marketplaceImage, azureClient))
                .thenReturn(AzureImageTermStatus.ACCEPTED);
        Optional<ManagementError> managementErrors =
                Optional.of(new ManagementError(MARKETPLACE_PURCHASE_ELIGIBILITY_FAILED.getCode(), MARKETPLACE_PURCHASE_ELIGIBILITY_FAILED.value()));
        when(azureTemplateDeploymentService.runWhatIfAnalysis(any(), any(), any())).thenReturn(managementErrors);

        CloudConnectorException exception = assertThrows(CloudConnectorException.class, () ->
                azureMarketplaceValidatorService.validateMarketplaceImageTerms(image, client, stack, ac));

        assertTrue(exception.getMessage().contains("Image validation error: [MarketplacePurchaseEligibilityFailed]"));
    }

    @Test
    void testValidateMarketplaceImageTermsSuccessfulValidation() {
        when(azureImageFormatValidator.isMarketplaceImageFormat(image)).thenReturn(true);
        AzureMarketplaceImage marketplaceImage = mock(AzureMarketplaceImage.class);
        when(azureMarketplaceImageProviderService.get(image)).thenReturn(marketplaceImage);
        AzureClient azureClient = mock(AzureClient.class);
        Subscription currentSubscription = mock(Subscription.class);
        when(azureClient.getCurrentSubscription()).thenReturn(currentSubscription);
        when(currentSubscription.subscriptionId()).thenReturn("sub");
        when(ac.getParameter(AzureClient.class)).thenReturn(azureClient);
        when(azureImageTermsSignerService.getImageTermStatus(
                azureClient.getCurrentSubscription().subscriptionId(), marketplaceImage, azureClient))
                .thenReturn(AzureImageTermStatus.ACCEPTED);
        CloudStack newStack = mock(CloudStack.class);
        ValidationResult validationResult = mock(ValidationResult.class);

        assertDoesNotThrow(() -> azureMarketplaceValidatorService.validateMarketplaceImageTerms(image, client, stack, ac));
    }
}