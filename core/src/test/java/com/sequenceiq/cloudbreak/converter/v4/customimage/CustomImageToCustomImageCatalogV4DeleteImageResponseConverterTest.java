package com.sequenceiq.cloudbreak.converter.v4.customimage;

import com.sequenceiq.cloudbreak.api.endpoint.v4.customimage.response.CustomImageCatalogV4DeleteImageResponse;
import com.sequenceiq.cloudbreak.domain.CustomImage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class CustomImageToCustomImageCatalogV4DeleteImageResponseConverterTest {

    private static final String IMAGE_ID = "image id";

    private CustomImageToCustomImageCatalogV4DeleteImageResponseConverter victim;

    @BeforeEach
    public void initTest() {
        victim = new CustomImageToCustomImageCatalogV4DeleteImageResponseConverter();
    }

    @Test
    public void shouldConvert() {
        CustomImage customImage = new CustomImage();
        customImage.setName(IMAGE_ID);

        CustomImageCatalogV4DeleteImageResponse result = victim.convert(customImage);

        assertEquals(IMAGE_ID, result.getImageId());
    }
}