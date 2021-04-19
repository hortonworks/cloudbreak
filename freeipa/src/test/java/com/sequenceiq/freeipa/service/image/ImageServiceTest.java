package com.sequenceiq.freeipa.service.image;

import static org.junit.Assert.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.Optional;

import org.hamcrest.CoreMatchers;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import com.sequenceiq.freeipa.api.model.image.Image;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.image.ImageSettingsRequest;

@RunWith(MockitoJUnitRunner.class)
public class ImageServiceTest {

    private static final String DEFAULT_PLATFORM = "aws";

    private static final String DEFAULT_REGION = "eu-west-1";

    private static final String EXISTING_ID = "ami-09fea90f257c85513";

    private static final String FAKE_ID = "fake-ami-0a6931aea1415eb0e";

    private static final String IMAGE_CATALOG = "image catalog";

    private static final String DEFAULT_OS = "redhat7";

    @Mock
    private ImageProviderFactory imageProviderFactory;

    @Mock
    private ImageProvider imageProvider;

    @InjectMocks
    private ImageService underTest;

    @Mock
    private Image image;

    @Test
    public void tesDetermineImageNameFound() {
        when(image.getImageSetsByProvider()).thenReturn(Collections.singletonMap(DEFAULT_PLATFORM, Collections.singletonMap(DEFAULT_REGION, EXISTING_ID)));

        String imageName = underTest.determineImageName(DEFAULT_PLATFORM, DEFAULT_REGION, image);
        assertEquals("ami-09fea90f257c85513", imageName);
    }

    @Test
    public void tesDetermineImageNameNotFound() {
        when(image.getImageSetsByProvider()).thenReturn(Collections.singletonMap(DEFAULT_PLATFORM, Collections.singletonMap(DEFAULT_REGION, EXISTING_ID)));

        Exception exception = assertThrows(RuntimeException.class, () ->
                underTest.determineImageName(DEFAULT_PLATFORM, "fake-region", image));
        String exceptionMessage = "Virtual machine image couldn't be found in image";
        Assert.assertThat(exception.getMessage(), CoreMatchers.containsString(exceptionMessage));
    }

    @Test
    public void testGetImageGivenIdInputNotFound() {
        ImageSettingsRequest imageSettings = new ImageSettingsRequest();
        imageSettings.setCatalog(IMAGE_CATALOG);
        imageSettings.setId(FAKE_ID);
        imageSettings.setOs(DEFAULT_OS);

        when(imageProviderFactory.getImageProvider(IMAGE_CATALOG)).thenReturn(imageProvider);
        when(imageProvider.getImage(imageSettings, DEFAULT_REGION, DEFAULT_PLATFORM)).thenReturn(Optional.empty());

        Exception exception = assertThrows(RuntimeException.class, () ->
                underTest.getImage(imageSettings, DEFAULT_REGION, DEFAULT_PLATFORM));
        String exceptionMessage = "Could not find any image with id: 'fake-ami-0a6931aea1415eb0e' in region 'eu-west-1' with OS 'redhat7'.";
        assertEquals(exceptionMessage, exception.getMessage());
    }
}
