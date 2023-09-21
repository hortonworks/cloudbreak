package com.sequenceiq.cloudbreak.controller.validation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyLong;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.image.ImageSettingsV4Request;
import com.sequenceiq.cloudbreak.cloud.Authenticator;
import com.sequenceiq.cloudbreak.cloud.CloudConnector;
import com.sequenceiq.cloudbreak.cloud.azure.validator.AzureImageFormatValidator;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.exception.CloudConnectorException;
import com.sequenceiq.cloudbreak.cloud.exception.CloudPlatformValidationWarningException;
import com.sequenceiq.cloudbreak.cloud.init.CloudPlatformConnectors;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.Image;
import com.sequenceiq.cloudbreak.cloud.model.Platform;
import com.sequenceiq.cloudbreak.common.user.CloudbreakUser;
import com.sequenceiq.cloudbreak.service.environment.marketplace.AzureMarketplaceTermsClientService;
import com.sequenceiq.cloudbreak.util.StackUtil;

@ExtendWith(MockitoExtension.class)
public class RecommendedImageValidatorTest {

    private static final String ENVIRONMENT_CRN = "environment_crn";

    private static final String REGION = "region";

    private static final String PLATFORM = "AZURE";

    private static final String BLUEPRINT_NAME = "blueprint_name";

    @InjectMocks
    private RecommendedImageValidator validator;

    @Mock
    private CloudPlatformConnectors cloudPlatformConnectors;

    @Mock
    private StackUtil stackUtil;

    @Mock
    private AzureMarketplaceTermsClientService azureMarketplaceTermsClientService;

    @Mock
    private RecommendImageService recommendImageService;

    @Mock
    private CloudConnector cloudConnector;

    @Mock
    private AzureImageFormatValidator imageValidator;

    @Mock
    private Authenticator authenticator;

    @Test
    public void testValidateWithNonAzurePlatform() {
        String platform = "AWS";

        CloudbreakUser cloudbreakUser = new CloudbreakUser("testId", "testCrn", "testName", "testEmail", "tenant");
        RecommendedImageValidator.ValidationResult result = validator.validateRecommendedImage(
                1L, cloudbreakUser, new ImageSettingsV4Request(), "", "", platform, "");

        assertNull(result.getErrorMsg());
        assertNull(result.getWarningMsg());
    }

    @Test
    public void testValidateWithSuccessfulValidation() {
        setupAzureMocks();

        CloudbreakUser cloudbreakUser = new CloudbreakUser("testId", "testCrn", "testName", "testEmail", "tenant");
        RecommendedImageValidator.ValidationResult result = validator.validateRecommendedImage(
                1L, cloudbreakUser, new ImageSettingsV4Request(), "", "", "AZURE", "");

        assertNull(result.getErrorMsg());
        assertNull(result.getWarningMsg());
    }

    @Test
    public void testValidateWithCloudConnectorException() {
        setupAzureMocks();
        doThrow(new CloudConnectorException("Error during validation")).when(imageValidator).validate(any(), any());

        CloudbreakUser cloudbreakUser = new CloudbreakUser("testId", "testCrn", "testName", "testEmail", "tenant");
        RecommendedImageValidator.ValidationResult result = validator.validateRecommendedImage(
                1L, cloudbreakUser, new ImageSettingsV4Request(), "", "", "AZURE", "");

        assertEquals("Error during validation", result.getErrorMsg());
        assertNull(result.getWarningMsg());
    }

    @Test
    public void testValidateWithCloudPlatformValidationWarningException() {
        setupAzureMocks();
        doThrow(new CloudPlatformValidationWarningException("Warning during validation")).when(imageValidator).validate(any(), any());

        CloudbreakUser cloudbreakUser = new CloudbreakUser("testId", "testCrn", "testName", "testEmail", "tenant");
        RecommendedImageValidator.ValidationResult result = validator.validateRecommendedImage(
                1L, cloudbreakUser, new ImageSettingsV4Request(), "", "", "AZURE", "");

        assertNull(result.getErrorMsg());
        assertEquals("Warning during validation", result.getWarningMsg());
    }

    private void setupAzureMocks() {
        when(cloudPlatformConnectors.get(any())).thenReturn(cloudConnector);
        when(cloudConnector.validators(any())).thenReturn(List.of(imageValidator));
        when(cloudConnector.authentication()).thenReturn(authenticator);
        CloudContext cloudContext = CloudContext.Builder.builder().withPlatform(Platform.platform(PLATFORM)).build();
        CloudCredential cloudCredential = new CloudCredential();
        when(authenticator.authenticate(any(), any())).thenReturn(new AuthenticatedContext(cloudContext, cloudCredential));
        when(azureMarketplaceTermsClientService.getAccepted()).thenReturn(true);
        Image image = new Image(
                "imageName",
                Map.of(),
                "os",
                "osType",
                "url",
                "catalog",
                "imageId",
                Map.of(),
                "date",
                0L);
        when(recommendImageService.recommendImage(anyLong(), any(), any(), any(), any(), any(), any()))
                .thenReturn(image);
    }
}

