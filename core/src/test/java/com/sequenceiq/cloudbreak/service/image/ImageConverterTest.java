package com.sequenceiq.cloudbreak.service.image;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;

import org.junit.jupiter.api.Test;

import com.sequenceiq.cloudbreak.cloud.model.Image;
import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.cloudbreak.service.CloudbreakRuntimeException;

class ImageConverterTest {

    private final ImageConverter underTest = new ImageConverter();

    @Test
    public void testConvertJsonToImage() throws IOException {
        Json imageJson = mock(Json.class);
        Image expected = mock(Image.class);
        when(imageJson.get(Image.class)).thenReturn(expected);

        Image result = underTest.convertJsonToImage(imageJson);

        assertEquals(expected, result);
    }

    @Test
    public void testConvertJsonToImageFails() throws IOException {
        Json imageJson = mock(Json.class);
        when(imageJson.get(Image.class)).thenThrow(new IOException());

        assertThrows(CloudbreakRuntimeException.class, () -> underTest.convertJsonToImage(imageJson));
    }
}