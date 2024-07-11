package com.sequenceiq.cloudbreak.core.flow2.stack.upscale;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.stack.ImageFallbackSuccess;
import com.sequenceiq.cloudbreak.reactor.api.event.stack.userdata.UpscaleCreateUserdataSecretsSuccess;

public class ImageFallbackSuccessToUpscaleCreateUserdataSecretsSuccessConverterTest {

    private final ImageFallbackSuccessToUpscaleCreateUserdataSecretsSuccessConverter underTest =
            new ImageFallbackSuccessToUpscaleCreateUserdataSecretsSuccessConverter();

    @Test
    void testCanConvert() {
        assertTrue(underTest.canConvert(ImageFallbackSuccess.class));
        assertFalse(underTest.canConvert(StackEvent.class));
    }

    @Test
    void testConvert() {
        UpscaleCreateUserdataSecretsSuccess result = underTest.convert(new ImageFallbackSuccess(1L));
        assertEquals(1L, result.getResourceId());
        assertEquals(List.of(), result.getCreatedSecretResourceIds());
    }
}
