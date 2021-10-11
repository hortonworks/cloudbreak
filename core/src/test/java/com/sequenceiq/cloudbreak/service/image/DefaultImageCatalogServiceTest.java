package com.sequenceiq.cloudbreak.service.image;

import com.sequenceiq.cloudbreak.cloud.model.catalog.CloudbreakImageCatalogV3;
import com.sequenceiq.cloudbreak.cloud.model.catalog.Image;
import com.sequenceiq.cloudbreak.cloud.model.catalog.Images;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.cloudbreak.core.CloudbreakImageCatalogException;
import com.sequenceiq.cloudbreak.core.CloudbreakImageNotFoundException;
import com.sequenceiq.common.api.type.ImageType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class DefaultImageCatalogServiceTest {

    private static final String IMAGE_ID = "image id";

    private static final String DEFAULT_FREEIPA_CATALOG_URL = "http://freeipa.url";

    private static final String DEFAULT_CATALOG_URL = "http://cb.url";

    private static final String FREEIPA_DEFAULT_CATALOG_NAME = "freeipa-default";

    @Mock
    private ImageCatalogService imageCatalogService;

    @Mock
    private ImageCatalogProvider imageCatalogProvider;

    @InjectMocks
    private DefaultImageCatalogService victim;

    @BeforeEach
    public void initTest() {
        ReflectionTestUtils.setField(victim, DefaultImageCatalogService.class, "defaultCatalogUrl", DEFAULT_CATALOG_URL, null);
        ReflectionTestUtils.setField(victim, DefaultImageCatalogService.class, "defaultFreeIpaCatalogUrl", DEFAULT_FREEIPA_CATALOG_URL, null);
    }

    @Test
    public void testGetFreeIpaImageFromDefaultCatalog() throws CloudbreakImageNotFoundException, CloudbreakImageCatalogException {
        StatedImage expected = mock(StatedImage.class);

        when(imageCatalogService.getImage(DEFAULT_FREEIPA_CATALOG_URL, FREEIPA_DEFAULT_CATALOG_NAME, IMAGE_ID)).thenReturn(expected);

        StatedImage actual = victim.getImageFromDefaultCatalog(IMAGE_ID);

        assertEquals(expected, actual);
    }

    @Test
    public void testGetImageFromDefaultCatalogWhenImageNotFoundInFreeIpaImageCatalog()
            throws CloudbreakImageNotFoundException, CloudbreakImageCatalogException {
        StatedImage expected = mock(StatedImage.class);

        when(imageCatalogService.getImage(DEFAULT_FREEIPA_CATALOG_URL, FREEIPA_DEFAULT_CATALOG_NAME, IMAGE_ID))
                .thenThrow(new CloudbreakImageNotFoundException(""));
        when(imageCatalogService.getImage(IMAGE_ID)).thenReturn(expected);

        StatedImage actual = victim.getImageFromDefaultCatalog(IMAGE_ID);

        assertEquals(expected, actual);
    }

    @Test
    public void testGetImageFromDefaultCatalogWithoutRuntimeThrowsBadRequestInCaseOfNonFreeIpaImageType() {
        assertThrows(BadRequestException.class, () -> victim.getImageFromDefaultCatalog(ImageType.RUNTIME.name(), "aws"));
    }

    @Test
    public void testGetImageFromDefaultCatalogWithoutRuntimeThrowsBadRequestInCaseOfNotSupportedImageType() {
        assertThrows(BadRequestException.class, () -> victim.getImageFromDefaultCatalog(ImageType.UNKNOWN.name(), "aws"));
    }

    @Test
    public void testGetImageFromDefaultCatalogWithRuntimeThrowsBadRequestInCaseOfFreeIpaImageType() {
        assertThrows(BadRequestException.class, () -> victim.getImageFromDefaultCatalog(ImageType.FREEIPA.name(), "aws", "7.2.10"));
    }

    @Test
    public void testGetImageFromDefaultCatalogWithRuntimeThrowsBadRequestInCaseOfNotSupportedImageType() {
        assertThrows(BadRequestException.class, () -> victim.getImageFromDefaultCatalog(ImageType.UNKNOWN.name(), "aws", "7.2.10"));
    }

    @Test
    public void testGetImageFromDefaultCatalogWithoutRuntime() throws CloudbreakImageNotFoundException, CloudbreakImageCatalogException {
        CloudbreakImageCatalogV3 freeipaImageCatalogV3 = mock(CloudbreakImageCatalogV3.class);
        Images images = mock(Images.class);
        Image expected = mock(Image.class);
        Image notExpectedByCreated = mock(Image.class);
        Image notExpectedByProvider = mock(Image.class);

        Map<String, Map<String, String>> expectedImageSetByProvider = Map.of("aws", Map.of());
        Map<String, Map<String, String>> notExpectedImageSetByProvider = Map.of("azure", Map.of());

        List<Image> imageList = List.of(expected, notExpectedByCreated, notExpectedByProvider);

        when(imageCatalogProvider.getImageCatalogV3(DEFAULT_FREEIPA_CATALOG_URL)).thenReturn(freeipaImageCatalogV3);
        when(freeipaImageCatalogV3.getImages()).thenReturn(images);
        when(images.getFreeIpaImages()).thenReturn(imageList);
        when(expected.getImageSetsByProvider()).thenReturn(expectedImageSetByProvider);
        when(expected.getCreated()).thenReturn(2L);
        when(notExpectedByCreated.getImageSetsByProvider()).thenReturn(expectedImageSetByProvider);
        when(notExpectedByCreated.getCreated()).thenReturn(1L);
        when(notExpectedByProvider.getImageSetsByProvider()).thenReturn(notExpectedImageSetByProvider);
        when(notExpectedByProvider.getCreated()).thenReturn(0L);

        StatedImage actual = victim.getImageFromDefaultCatalog(ImageType.FREEIPA.name(), "aws");

        assertEquals(expected, actual.getImage());
    }

    @Test
    public void testGetImageFromDefaultCatalogWithoutRuntimeThrowsCloudbreakImageNotFoundException()
            throws CloudbreakImageCatalogException {
        CloudbreakImageCatalogV3 freeipaImageCatalogV3 = mock(CloudbreakImageCatalogV3.class);
        Images images = mock(Images.class);
        Image expected = mock(Image.class);
        Image notExpected = mock(Image.class);

        Map<String, Map<String, String>> imageSetByProvider = Map.of();

        List<Image> imageList = List.of(expected, notExpected);

        when(imageCatalogProvider.getImageCatalogV3(DEFAULT_FREEIPA_CATALOG_URL)).thenReturn(freeipaImageCatalogV3);
        when(freeipaImageCatalogV3.getImages()).thenReturn(images);
        when(images.getFreeIpaImages()).thenReturn(imageList);
        when(expected.getImageSetsByProvider()).thenReturn(imageSetByProvider);
        when(expected.getCreated()).thenReturn(2L);
        when(notExpected.getImageSetsByProvider()).thenReturn(imageSetByProvider);
        when(notExpected.getCreated()).thenReturn(1L);

        assertThrows(CloudbreakImageNotFoundException.class, () -> victim.getImageFromDefaultCatalog(ImageType.FREEIPA.name(), "aws"));
    }

    @Test
    public void testGetImageFromDefaultCatalogWithRuntime() throws CloudbreakImageCatalogException, CloudbreakImageNotFoundException {
        ArgumentCaptor<ImageFilter> imageFilterArgumentCaptor = ArgumentCaptor.forClass(ImageFilter.class);
        when(imageCatalogService.getImagePrewarmedDefaultPreferred(imageFilterArgumentCaptor.capture(), Mockito.any())).thenReturn(mock(StatedImage.class));

        StatedImage actual = victim.getImageFromDefaultCatalog(ImageType.RUNTIME.name(), "aws", "7.2.10");

        assertNotNull(actual);
        assertEquals(imageFilterArgumentCaptor.getValue().getClusterVersion(), "7.2.10");
        assertTrue(imageFilterArgumentCaptor.getValue().getPlatforms().contains("aws"));
    }
}