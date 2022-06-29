package com.sequenceiq.cloudbreak.service.image;

import java.io.IOException;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.sequenceiq.cloudbreak.cloud.model.Image;
import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.cloudbreak.service.CloudbreakRuntimeException;

class ImageConverterTest {

    private final ImageConverter underTest = new ImageConverter();

    @Test
    public void testConvertJsonToImage() throws IOException {
        Json imageJson = Mockito.mock(Json.class);
        Image expected = Mockito.mock(Image.class);
        Mockito.when(imageJson.get(Image.class)).thenReturn(expected);

        Image result = underTest.convertJsonToImage(imageJson);

        Assertions.assertEquals(expected, result);
    }

    @Test
    public void testConvertJsonToImageFails() throws IOException {
        Json imageJson = Mockito.mock(Json.class);
        Mockito.when(imageJson.get(Image.class)).thenThrow(new IOException());

        Assertions.assertThrows(CloudbreakRuntimeException.class, () -> underTest.convertJsonToImage(imageJson));
    }
}