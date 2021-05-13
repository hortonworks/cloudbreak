package com.sequenceiq.cloudbreak.converter.v4.customimage;

import com.sequenceiq.cloudbreak.api.endpoint.v4.customimage.response.CustomImageCatalogV4GetResponse;
import com.sequenceiq.cloudbreak.domain.CustomImage;
import com.sequenceiq.cloudbreak.domain.ImageCatalog;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ImageCatalogToCustomImageCatalogV4GetResponseConverterTest {

    private static final String NAME = "image catalog name";

    private static final String DESCRIPTION = "image catalog description";

    private static final String IMAGE_ID = "image id";

    private ImageCatalogToCustomImageCatalogV4GetResponseConverter victim;

    @BeforeEach
    public void initTest() {
        victim = new ImageCatalogToCustomImageCatalogV4GetResponseConverter();
    }

    @Test
    public void shouldConvert() {
        ImageCatalog imageCatalog = new ImageCatalog();
        imageCatalog.setName(NAME);
        imageCatalog.setDescription(DESCRIPTION);
        imageCatalog.setCustomImages(Collections.singleton(getCustomImage(IMAGE_ID)));

        CustomImageCatalogV4GetResponse result = victim.convert(imageCatalog);

        assertEquals(NAME, result.getName());
        assertEquals(DESCRIPTION, result.getDescription());
        assertEquals(1, result.getImageIds().size());
        assertEquals(IMAGE_ID, result.getImageIds().stream().findFirst().get());
    }

    private CustomImage getCustomImage(String name) {
        CustomImage customImage = new CustomImage();
        customImage.setName(name);

        return customImage;
    }
}