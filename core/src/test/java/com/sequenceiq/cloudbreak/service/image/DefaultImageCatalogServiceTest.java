package com.sequenceiq.cloudbreak.service.image;

import static com.sequenceiq.common.model.ImageCatalogPlatform.imageCatalogPlatform;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.sequenceiq.cloudbreak.cloud.model.catalog.CloudbreakImageCatalogV3;
import com.sequenceiq.cloudbreak.cloud.model.catalog.Image;
import com.sequenceiq.cloudbreak.cloud.model.catalog.Images;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.cloudbreak.core.CloudbreakImageCatalogException;
import com.sequenceiq.cloudbreak.core.CloudbreakImageNotFoundException;
import com.sequenceiq.common.api.type.ImageType;
import com.sequenceiq.common.model.Architecture;

@ExtendWith(MockitoExtension.class)
public class DefaultImageCatalogServiceTest {

    private static final String IMAGE_ID = "image id";

    private static final Long WORKSPACE_ID = 1L;

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

        when(imageCatalogService.getImage(WORKSPACE_ID, DEFAULT_FREEIPA_CATALOG_URL, FREEIPA_DEFAULT_CATALOG_NAME, IMAGE_ID)).thenReturn(expected);

        StatedImage actual = victim.getImageFromDefaultCatalog(WORKSPACE_ID, IMAGE_ID);

        assertEquals(expected, actual);
    }

    @Test
    public void testGetImageFromDefaultCatalogWhenImageNotFoundInFreeIpaImageCatalog()
            throws CloudbreakImageNotFoundException, CloudbreakImageCatalogException {
        StatedImage expected = mock(StatedImage.class);

        when(imageCatalogService.getImage(WORKSPACE_ID, DEFAULT_FREEIPA_CATALOG_URL, FREEIPA_DEFAULT_CATALOG_NAME, IMAGE_ID))
                .thenThrow(new CloudbreakImageNotFoundException(""));
        when(imageCatalogService.getImage(WORKSPACE_ID, IMAGE_ID)).thenReturn(expected);

        StatedImage actual = victim.getImageFromDefaultCatalog(WORKSPACE_ID, IMAGE_ID);

        assertEquals(expected, actual);
    }

    @Test
    public void testGetImageFromDefaultCatalogWithoutRuntimeThrowsBadRequestInCaseOfNonFreeIpaImageType() {
        BadRequestException badRequestException = assertThrows(BadRequestException.class,
                () -> victim.getImageFromDefaultCatalog(ImageType.RUNTIME.name(), imageCatalogPlatform("aws")));
        assertEquals("Runtime is required in case of 'RUNTIME' image type", badRequestException.getMessage());
    }

    @Test
    public void testGetImageFromDefaultCatalogWithoutRuntimeThrowsBadRequestInCaseOfNotSupportedImageType() {
        BadRequestException badRequestException = assertThrows(BadRequestException.class,
                () -> victim.getImageFromDefaultCatalog(ImageType.UNKNOWN.name(), imageCatalogPlatform("aws")));
        assertEquals("Type 'UNKNOWN' is not supported.", badRequestException.getMessage());
    }

    @Test
    public void testGetImageFromDefaultCatalogWithRuntimeThrowsBadRequestInCaseOfFreeIpaImageType() {
        BadRequestException badRequestException = assertThrows(BadRequestException.class,
                () -> victim.getImageFromDefaultCatalog(ImageType.FREEIPA.name(), imageCatalogPlatform("aws"), "7.2.10",
                Architecture.X86_64));
        assertEquals("Runtime is not supported in case of 'FREEIPA' image type", badRequestException.getMessage());
    }

    @Test
    public void testGetImageFromDefaultCatalogWithRuntimeThrowsBadRequestInCaseOfNotSupportedImageType()
            throws CloudbreakImageNotFoundException, CloudbreakImageCatalogException {
        BadRequestException badRequestException = assertThrows(BadRequestException.class,
                () -> victim.getImageFromDefaultCatalog(ImageType.UNKNOWN.name(), imageCatalogPlatform("aws"), "7.2.10",
                Architecture.X86_64));
        assertEquals("Image type 'UNKNOWN' is not supported.", badRequestException.getMessage());
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

        StatedImage actual = victim.getImageFromDefaultCatalog(ImageType.FREEIPA.name(), imageCatalogPlatform("aws"));

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

        assertThrows(CloudbreakImageNotFoundException.class, () -> victim.getImageFromDefaultCatalog(ImageType.FREEIPA.name(), imageCatalogPlatform("aws")));
    }

    @Test
    public void testGetImageFromDefaultCatalogWithRuntime() throws CloudbreakImageCatalogException, CloudbreakImageNotFoundException {
        ArgumentCaptor<ImageFilter> imageFilterArgumentCaptor = ArgumentCaptor.forClass(ImageFilter.class);
        when(imageCatalogService.getImagePrewarmedDefaultPreferred(imageFilterArgumentCaptor.capture())).thenReturn(mock(StatedImage.class));

        StatedImage actual = victim.getImageFromDefaultCatalog(ImageType.RUNTIME.name(), imageCatalogPlatform("aws"), "7.2.10", Architecture.ARM64);
        assertNotNull(actual);
        assertEquals("7.2.10", imageFilterArgumentCaptor.getValue().getClusterVersion());
        assertEquals(Architecture.ARM64, imageFilterArgumentCaptor.getValue().getArchitecture());
        assertTrue(imageFilterArgumentCaptor.getValue().getPlatforms().stream().map(e -> e.nameToLowerCase()).collect(Collectors.toList())
                .contains("aws"));
    }
}