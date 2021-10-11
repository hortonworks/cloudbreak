package com.sequenceiq.cloudbreak.converter.v4.customimage;

import com.sequenceiq.cloudbreak.api.endpoint.v4.customimage.response.CustomImageCatalogV4GetResponse;
import com.sequenceiq.cloudbreak.api.endpoint.v4.customimage.response.CustomImageCatalogV4ImageListItemResponse;
import com.sequenceiq.cloudbreak.cloud.model.catalog.Image;
import com.sequenceiq.cloudbreak.core.CloudbreakImageCatalogException;
import com.sequenceiq.cloudbreak.core.CloudbreakImageNotFoundException;
import com.sequenceiq.cloudbreak.domain.CustomImage;
import com.sequenceiq.cloudbreak.domain.ImageCatalog;
import com.sequenceiq.cloudbreak.service.image.ImageCatalogService;
import com.sequenceiq.cloudbreak.service.image.StatedImage;
import com.sequenceiq.common.api.type.ImageType;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class ImageCatalogToCustomImageCatalogV4GetResponseConverterTest {

    private static final String NAME = "image catalog name";

    private static final String DESCRIPTION = "image catalog description";

    private static final String IMAGE_ID = "image id";

    private static final String SOURCE_IMAGE_ID = "source image id";

    private static final Long SOURCE_IMAGE_DATE = 2L;

    private static final String CLOUD_PROVIDER = "aws";

    @Mock
    private ImageCatalogService imageCatalogService;

    @Mock
    private ImageVersionsConverter imageVersionsConverter;

    @Mock
    private StatedImage statedImage;

    @Mock
    private Image image;

    @InjectMocks
    private ImageCatalogToCustomImageCatalogV4GetResponseConverter victim;

    @Test
    public void shouldConvert() throws CloudbreakImageNotFoundException, CloudbreakImageCatalogException {
        CustomImage customImage = getCustomImage(IMAGE_ID);
        ImageCatalog imageCatalog = new ImageCatalog();
        imageCatalog.setName(NAME);
        customImage.setImageType(ImageType.RUNTIME);
        customImage.setCustomizedImageId(SOURCE_IMAGE_ID);
        imageCatalog.setDescription(DESCRIPTION);
        imageCatalog.setCustomImages(Collections.singleton(customImage));

        when(imageCatalogService.getSourceImageByImageType(customImage)).thenReturn(statedImage);
        when(statedImage.getImage()).thenReturn(image);
        when(image.getCreated()).thenReturn(SOURCE_IMAGE_DATE);
        when(image.getImageSetsByProvider()).thenReturn(Map.of(CLOUD_PROVIDER, Map.of()));
        when(imageVersionsConverter.convert(image)).thenReturn(Collections.emptyMap());

        CustomImageCatalogV4GetResponse result = victim.convert(imageCatalog);

        assertEquals(NAME, result.getName());
        assertEquals(DESCRIPTION, result.getDescription());
        assertEquals(1, result.getImages().size());

        CustomImageCatalogV4ImageListItemResponse imageResult = result.getImages().stream().findFirst().get();
        assertEquals(IMAGE_ID, imageResult.getImageId());
        assertEquals(SOURCE_IMAGE_ID, imageResult.getSourceImageId());
        assertNotNull(imageResult.getImageDate());
        assertEquals(SOURCE_IMAGE_DATE, imageResult.getSourceImageDate());
        assertEquals(CLOUD_PROVIDER, imageResult.getCloudProvider());
        assertNotNull(imageResult.getVersions());
        assertEquals(ImageType.RUNTIME.name(), imageResult.getImageType());
    }

    private CustomImage getCustomImage(String name) {
        CustomImage customImage = new CustomImage();
        customImage.setName(name);

        return customImage;
    }
}