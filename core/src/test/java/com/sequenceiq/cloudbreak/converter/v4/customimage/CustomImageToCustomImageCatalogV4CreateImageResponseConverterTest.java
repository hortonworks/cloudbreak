package com.sequenceiq.cloudbreak.converter.v4.customimage;

import com.sequenceiq.cloudbreak.api.endpoint.v4.customimage.response.CustomImageCatalogV4CreateImageResponse;
import com.sequenceiq.cloudbreak.api.endpoint.v4.customimage.response.CustomImageCatalogV4VmImageResponse;
import com.sequenceiq.cloudbreak.domain.CustomImage;
import com.sequenceiq.cloudbreak.domain.VmImage;
import com.sequenceiq.common.api.type.ImageType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class CustomImageToCustomImageCatalogV4CreateImageResponseConverterTest {

    private static final String IMAGE_ID = "image id";

    private static final String SOURCE_IMAGE_ID = "source image id";

    private static final String BASE_PARCEL_URL = "base parcel url";

    private static final String REGION = "region";

    private static final String IMAGE_REFERENCE = "image reference";

    private CustomImageToCustomImageCatalogV4CreateImageResponseConverter victim;

    @BeforeEach
    public void initTest() {
        victim = new CustomImageToCustomImageCatalogV4CreateImageResponseConverter();
    }

    @Test
    public void shouldConvert() {
        CustomImage customImage = new CustomImage();
        customImage.setName(IMAGE_ID);
        customImage.setImageType(ImageType.RUNTIME);
        customImage.setBaseParcelUrl(BASE_PARCEL_URL);
        customImage.setCustomizedImageId(SOURCE_IMAGE_ID);
        customImage.setVmImage(Collections.singleton(getVmImage(REGION, IMAGE_REFERENCE)));

        CustomImageCatalogV4CreateImageResponse result = victim.convert(customImage);
        assertEquals(IMAGE_ID, result.getImageId());
        assertEquals(SOURCE_IMAGE_ID, result.getSourceImageId());
        assertEquals(BASE_PARCEL_URL, result.getBaseParcelUrl());
        assertEquals(ImageType.RUNTIME.name(), result.getImageType());
        assertEquals(1, result.getVmImages().size());

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