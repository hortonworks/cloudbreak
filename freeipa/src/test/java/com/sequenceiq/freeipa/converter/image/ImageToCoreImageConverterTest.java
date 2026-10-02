package com.sequenceiq.freeipa.converter.image;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Map;

import org.junit.jupiter.api.Test;

import com.sequenceiq.cloudbreak.constant.AzureConstants;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.image.Image;

class ImageToCoreImageConverterTest {

    private static final Map<String, String> TAGS = Map.of(AzureConstants.HYPER_V_GENERATION_TAG, "2", "fips-mode", "disabled");

    private final ImageToCoreImageConverter underTest = new ImageToCoreImageConverter();

    private final CoreImageToImageConverter coreImageToImageConverter = new CoreImageToImageConverter();

    @Test
    void testConvertKeepsTags() {
        com.sequenceiq.cloudbreak.cloud.model.catalog.Image result = underTest.convert(image(TAGS));

        assertEquals(TAGS, result.getTags());
    }

    @Test
    void testRoundTripThroughProviderSpecificFilterKeepsTags() {
        Image result = coreImageToImageConverter.convert(underTest.convert(image(TAGS)));

        assertEquals(TAGS, result.getTags());
    }

    @Test
    void testConvertWhenNoTags() {
        com.sequenceiq.cloudbreak.cloud.model.catalog.Image result = underTest.convert(image(Map.of()));

        assertEquals(Map.of(), result.getTags());
    }

    private Image image(Map<String, String> tags) {
        return new Image(1L, "date", "description", "redhat9", "uuid", Map.of("azure", Map.of("West US 2", "vhd")), "redhat9",
                Map.of("salt", "3006.10"), true, "x86_64", tags);
    }
}
