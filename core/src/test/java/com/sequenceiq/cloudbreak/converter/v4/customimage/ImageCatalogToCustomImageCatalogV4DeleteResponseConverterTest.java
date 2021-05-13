package com.sequenceiq.cloudbreak.converter.v4.customimage;

import com.sequenceiq.cloudbreak.api.endpoint.v4.customimage.response.CustomImageCatalogV4DeleteResponse;
import com.sequenceiq.cloudbreak.domain.ImageCatalog;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ImageCatalogToCustomImageCatalogV4DeleteResponseConverterTest {

    private static final String NAME = "image catalog name";

    private ImageCatalogToCustomImageCatalogV4DeleteResponseConverter victim;

    @BeforeEach
    public void initTest() {
        victim = new ImageCatalogToCustomImageCatalogV4DeleteResponseConverter();
    }

    @Test
    public void shouldConvert() {
        ImageCatalog imageCatalog = new ImageCatalog();
        imageCatalog.setName(NAME);

        CustomImageCatalogV4DeleteResponse result = victim.convert(imageCatalog);

        assertEquals(NAME, result.getName());
    }
}