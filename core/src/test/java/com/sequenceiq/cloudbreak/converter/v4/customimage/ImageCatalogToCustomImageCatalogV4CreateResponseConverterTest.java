package com.sequenceiq.cloudbreak.converter.v4.customimage;

import com.sequenceiq.cloudbreak.api.endpoint.v4.customimage.response.CustomImageCatalogV4CreateResponse;
import com.sequenceiq.cloudbreak.domain.ImageCatalog;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ImageCatalogToCustomImageCatalogV4CreateResponseConverterTest {

    private static final String NAME = "image catalog name";

    private static final String DESCRIPTION = "image catalog description";

    private ImageCatalogToCustomImageCatalogV4CreateResponseConverter victim;

    @BeforeEach
    public void initTest() {
        victim = new ImageCatalogToCustomImageCatalogV4CreateResponseConverter();
    }

    @Test
    public void shouldConvert() {
        ImageCatalog imageCatalog = new ImageCatalog();
        imageCatalog.setName(NAME);
        imageCatalog.setDescription(DESCRIPTION);

        CustomImageCatalogV4CreateResponse result = victim.convert(imageCatalog);

        assertEquals(NAME, result.getName());
        assertEquals(DESCRIPTION, result.getDescription());
    }
}