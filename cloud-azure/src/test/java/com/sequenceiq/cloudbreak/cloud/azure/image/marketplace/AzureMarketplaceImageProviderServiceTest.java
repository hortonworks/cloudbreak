package com.sequenceiq.cloudbreak.cloud.azure.image.marketplace;

import java.util.HashMap;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.cloud.exception.CloudConnectorException;
import com.sequenceiq.cloudbreak.cloud.model.Image;

@ExtendWith(MockitoExtension.class)
public class AzureMarketplaceImageProviderServiceTest {

    private static final String VALID_IMAGE_NAME = "cloudera:cdp-7_2:freeipa:1.0.2103081333";

    private static final String INVALID_IMAGE_NAME = "cloudera:cdp-7_2:freeipa:1.0.2103081333:latest";

    @InjectMocks
    private AzureMarketplaceImageProviderService underTest;

    @Test
    void testImageHasValidFormat() {

        Image image = new Image(VALID_IMAGE_NAME, new HashMap<>(), "centos7", "redhat7", "", "default", "default-id", new HashMap<>());
        AzureMarketplaceImage azureMarketplaceImage = underTest.get(image);

        Assertions.assertEquals("cloudera", azureMarketplaceImage.getPublisherId());
        Assertions.assertEquals("cdp-7_2", azureMarketplaceImage.getOfferId());
        Assertions.assertEquals("freeipa", azureMarketplaceImage.getPlanId());
        Assertions.assertEquals("1.0.2103081333", azureMarketplaceImage.getVersion());
    }

    @Test
    void testImageHasInvalidFormat() {

        Image image = new Image(INVALID_IMAGE_NAME, new HashMap<>(), "centos7", "redhat7", "", "default", "default-id", new HashMap<>());
        Assertions.assertThrows(CloudConnectorException.class, () -> underTest.get(image));

    }
}