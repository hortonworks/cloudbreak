package com.sequenceiq.cloudbreak.cloud.azure.image.marketplace;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.cloud.azure.validator.AzureImageFormatValidator;
import com.sequenceiq.cloudbreak.cloud.exception.CloudConnectorException;
import com.sequenceiq.cloudbreak.cloud.model.Image;
import com.sequenceiq.cloudbreak.cloud.model.catalog.ImagePackageVersion;

@ExtendWith(MockitoExtension.class)
public class AzureMarketplaceImageProviderServiceTest {

    private static final String VALID_IMAGE_NAME = "cloudera:cdp-7_2:freeipa:1.0.2103081333";

    private static final String INVALID_IMAGE_NAME = "cloudera:cdp-7_2:freeipa:1.0.2103081333:latest";

    @InjectMocks
    private AzureMarketplaceImageProviderService underTest;

    @Mock
    private AzureImageFormatValidator azureImageFormatValidator;

    @Test
    void testImageHasValidFormat() {
        Image image = Image.builder()
                .withImageName(VALID_IMAGE_NAME)
                .build();
        when(azureImageFormatValidator.isMarketplaceImageFormat(VALID_IMAGE_NAME)).thenReturn(true);
        AzureMarketplaceImage azureMarketplaceImage = underTest.get(image);

        assertEquals("cloudera", azureMarketplaceImage.getPublisherId());
        assertEquals("cdp-7_2", azureMarketplaceImage.getOfferId());
        assertEquals("freeipa", azureMarketplaceImage.getPlanId());
        assertEquals("1.0.2103081333", azureMarketplaceImage.getVersion());
    }

    @Test
    void testImageHasInvalidFormat() {
        Image image = Image.builder()
                .withImageName(INVALID_IMAGE_NAME)
                .build();
        when(azureImageFormatValidator.isMarketplaceImageFormat(INVALID_IMAGE_NAME)).thenReturn(false);
        assertThrows(CloudConnectorException.class, () -> underTest.get(image));
    }

    @Test
    void testSourceImageHasValidFormat() {

        Map<String, String> packageVersions = new HashMap<>();
        packageVersions.put(ImagePackageVersion.SOURCE_IMAGE.getKey(), VALID_IMAGE_NAME);
        Image image = Image.builder()
                .withImageName("imageName")
                .withPackageVersions(packageVersions)
                .build();
        when(azureImageFormatValidator.isMarketplaceImageFormat(VALID_IMAGE_NAME)).thenReturn(true);
        AzureMarketplaceImage azureMarketplaceImage = underTest.getSourceImage(image);

        assertEquals("cloudera", azureMarketplaceImage.getPublisherId());
        assertEquals("cdp-7_2", azureMarketplaceImage.getOfferId());
        assertEquals("freeipa", azureMarketplaceImage.getPlanId());
        assertEquals("1.0.2103081333", azureMarketplaceImage.getVersion());
    }

    @Test
    void testSourceImageHasInvalidFormat() {

        Map<String, String> packageVersions = new HashMap<>();
        packageVersions.put(ImagePackageVersion.SOURCE_IMAGE.getKey(), INVALID_IMAGE_NAME);
        Image image = Image.builder()
                .withImageName("imageName")
                .withPackageVersions(packageVersions)
                .build();

        CloudConnectorException exception = assertThrows(CloudConnectorException.class, () -> underTest.getSourceImage(image));
        assertEquals("Invalid Marketplace image URN in the image catalog! Please specify the image in an URN format, 4 segments separated by a colon "
                + "(actual value is: cloudera:cdp-7_2:freeipa:1.0.2103081333:latest)!", exception.getMessage());

    }

    @Test
    void testSourceImageIsBlank() {

        Map<String, String> packageVersions = new HashMap<>();
        packageVersions.put(ImagePackageVersion.SOURCE_IMAGE.getKey(), "");
        Image image = Image.builder()
                .withImageName("imageName")
                .withPackageVersions(packageVersions)
                .build();

        CloudConnectorException exception = assertThrows(CloudConnectorException.class, () -> underTest.getSourceImage(image));
        assertEquals("Missing Marketplace image URN! Please specify the image in an URN format, "
                + "4 segments separated by a colon in the image catalog", exception.getMessage());

    }

    @Test
    void testSourceImageIsMissing() {
        Image image = Image.builder()
                .withImageName("imageName")
                .build();

        CloudConnectorException exception = assertThrows(CloudConnectorException.class, () -> underTest.getSourceImage(image));
        assertEquals("Missing Marketplace image URN! Please specify the image in an URN format, "
                + "4 segments separated by a colon in the image catalog", exception.getMessage());

    }
}