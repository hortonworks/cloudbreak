package com.sequenceiq.cloudbreak.converter.v4.customimage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.api.endpoint.v4.customimage.response.CustomImageCatalogV4GetResponse;
import com.sequenceiq.cloudbreak.api.endpoint.v4.customimage.response.CustomImageCatalogV4ImageListItemResponse;
import com.sequenceiq.cloudbreak.api.endpoint.v4.customimage.response.CustomImageCatalogV4VmImageResponse;
import com.sequenceiq.cloudbreak.cloud.model.catalog.Image;
import com.sequenceiq.cloudbreak.core.CloudbreakImageCatalogException;
import com.sequenceiq.cloudbreak.core.CloudbreakImageNotFoundException;
import com.sequenceiq.cloudbreak.domain.CustomImage;
import com.sequenceiq.cloudbreak.domain.ImageCatalog;
import com.sequenceiq.cloudbreak.domain.VmImage;
import com.sequenceiq.cloudbreak.service.image.ImageCatalogService;
import com.sequenceiq.cloudbreak.service.image.StatedImage;
import com.sequenceiq.common.api.type.ImageType;

@ExtendWith(MockitoExtension.class)
public class ImageCatalogToCustomImageCatalogV4GetResponseConverterTest {

    private static final String NAME = "image catalog name";

    private static final String DESCRIPTION = "image catalog description";

    private static final String IMAGE_ID = "image id";

    private static final String SOURCE_IMAGE_ID = "source image id";

    private static final Long SOURCE_IMAGE_DATE = 2L;

    private static final String CLOUD_PROVIDER = "aws";

    private static final String REGION = "us-west-1";

    private static final String IMAGE_REFERENCE = "ami-0123456789abcdef0";

    private static final String OTHER_IMAGE_ID = "other image id";

    private static final String OTHER_REGION = "eu-central-1";

    private static final String OTHER_IMAGE_REFERENCE = "ami-0fedcba9876543210";

    @Mock
    private ImageCatalogService imageCatalogService;

    @Mock
    private ImageVersionsConverter imageVersionsConverter;

    @Spy
    private VmImagesConverter vmImagesConverter;

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
        customImage.setVmImage(Set.of(getVmImage(REGION, IMAGE_REFERENCE)));
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

        assertEquals(1, imageResult.getVmImages().size());
        CustomImageCatalogV4VmImageResponse vmImageResult = imageResult.getVmImages().stream().findFirst().get();
        assertEquals(REGION, vmImageResult.getRegion());
        assertEquals(IMAGE_REFERENCE, vmImageResult.getImageReference());
    }

    @Test
    public void shouldConvertVmImagesOfEachImageSeparately() throws CloudbreakImageNotFoundException, CloudbreakImageCatalogException {
        CustomImage firstImage = getCustomImage(IMAGE_ID);
        firstImage.setVmImage(Set.of(getVmImage(REGION, IMAGE_REFERENCE)));
        CustomImage secondImage = getCustomImage(OTHER_IMAGE_ID);
        secondImage.setVmImage(Set.of(getVmImage(OTHER_REGION, OTHER_IMAGE_REFERENCE)));
        ImageCatalog imageCatalog = new ImageCatalog();
        imageCatalog.setCustomImages(Set.of(firstImage, secondImage));

        when(imageCatalogService.getSourceImageByImageType(any(CustomImage.class))).thenReturn(statedImage);
        when(statedImage.getImage()).thenReturn(image);

        Map<String, Map<String, String>> imageReferencesByRegionByImageId = victim.convert(imageCatalog).getImages().stream()
                .collect(Collectors.toMap(CustomImageCatalogV4ImageListItemResponse::getImageId, this::imageReferencesByRegion));

        assertEquals(Map.of(
                IMAGE_ID, Map.of(REGION, IMAGE_REFERENCE),
                OTHER_IMAGE_ID, Map.of(OTHER_REGION, OTHER_IMAGE_REFERENCE)), imageReferencesByRegionByImageId);
    }

    private Map<String, String> imageReferencesByRegion(CustomImageCatalogV4ImageListItemResponse image) {
        return image.getVmImages().stream()
                .collect(Collectors.toMap(CustomImageCatalogV4VmImageResponse::getRegion, CustomImageCatalogV4VmImageResponse::getImageReference));
    }

    private CustomImage getCustomImage(String name) {
        CustomImage customImage = new CustomImage();
        customImage.setName(name);

        return customImage;
    }

    private VmImage getVmImage(String region, String imageReference) {
        VmImage vmImage = new VmImage();
        vmImage.setRegion(region);
        vmImage.setImageReference(imageReference);

        return vmImage;
    }
}