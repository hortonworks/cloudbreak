package com.sequenceiq.cloudbreak.converter.v4.customimage;

import com.sequenceiq.cloudbreak.api.endpoint.v4.customimage.response.CustomImageCatalogV4GetImageResponse;
import com.sequenceiq.cloudbreak.api.endpoint.v4.customimage.response.CustomImageCatalogV4VmImageResponse;
import com.sequenceiq.cloudbreak.cloud.model.catalog.Image;
import com.sequenceiq.cloudbreak.core.CloudbreakImageCatalogException;
import com.sequenceiq.cloudbreak.core.CloudbreakImageNotFoundException;
import com.sequenceiq.cloudbreak.domain.CustomImage;
import com.sequenceiq.cloudbreak.domain.VmImage;
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
public class CustomImageToCustomImageCatalogV4GetImageResponseConverterTest {

    private static final String IMAGE_ID = "image id";

    private static final String SOURCE_IMAGE_ID = "source image id";

    private static final String BASE_PARCEL_URL = "base parcel url";

    private static final String REGION = "region";

    private static final String IMAGE_REFERENCE = "image reference";

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
    private CustomImageToCustomImageCatalogV4GetImageResponseConverter victim;

    @Test
    public void shouldConvert() throws CloudbreakImageNotFoundException, CloudbreakImageCatalogException {
        CustomImage customImage = new CustomImage();
        customImage.setName(IMAGE_ID);
        customImage.setImageType(ImageType.RUNTIME);
        customImage.setBaseParcelUrl(BASE_PARCEL_URL);
        customImage.setCustomizedImageId(SOURCE_IMAGE_ID);
        customImage.setVmImage(Collections.singleton(getVmImage(REGION, IMAGE_REFERENCE)));

        when(imageCatalogService.getSourceImageByImageType(customImage)).thenReturn(statedImage);
        when(statedImage.getImage()).thenReturn(image);
        when(image.getCreated()).thenReturn(SOURCE_IMAGE_DATE);
        when(image.getImageSetsByProvider()).thenReturn(Map.of(CLOUD_PROVIDER, Map.of()));
        when(imageVersionsConverter.convert(image)).thenReturn(Collections.emptyMap());

        CustomImageCatalogV4GetImageResponse result = victim.convert(customImage);
        assertEquals(IMAGE_ID, result.getImageId());
        assertEquals(SOURCE_IMAGE_ID, result.getSourceImageId());
        assertEquals(BASE_PARCEL_URL, result.getBaseParcelUrl());
        assertEquals(ImageType.RUNTIME.name(), result.getImageType());
        assertEquals(1, result.getVmImages().size());
        assertNotNull(result.getImageDate());
        assertEquals(SOURCE_IMAGE_DATE, result.getSourceImageDate());
        assertEquals(CLOUD_PROVIDER, result.getCloudProvider());
        assertNotNull(result.getVersions());

        CustomImageCatalogV4VmImageResponse vmImage = result.getVmImages().stream().findFirst().get();
        assertEquals(REGION, vmImage.getRegion());
        assertEquals(IMAGE_REFERENCE, vmImage.getImageReference());
    }

    private VmImage getVmImage(String region, String imageReference) {
        VmImage request = new VmImage();
        request.setRegion(region);
        request.setImageReference(imageReference);

        return request;
    }
}