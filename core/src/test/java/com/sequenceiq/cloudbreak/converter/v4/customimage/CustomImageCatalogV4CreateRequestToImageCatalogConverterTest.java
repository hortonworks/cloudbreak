package com.sequenceiq.cloudbreak.converter.v4.customimage;

import com.sequenceiq.cloudbreak.api.endpoint.v4.customimage.request.CustomImageCatalogV4CreateRequest;
import com.sequenceiq.cloudbreak.domain.ImageCatalog;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class CustomImageCatalogV4CreateRequestToImageCatalogConverterTest {

    private static final String NAME = "name";

    private static final String DESCRIPTION = "description";

    private CustomImageCatalogV4CreateRequestToImageCatalogConverter victim;

    @BeforeEach
    public void initTest() {
        victim = new CustomImageCatalogV4CreateRequestToImageCatalogConverter();
    }

    @Test
    public void shouldConvert() {
        CustomImageCatalogV4CreateRequest source = new CustomImageCatalogV4CreateRequest();
        source.setName(NAME);
        source.setDescription(DESCRIPTION);

        ImageCatalog result = victim.convert(source);
        assertEquals(NAME, result.getName());
        assertEquals(DESCRIPTION, result.getDescription());
    }
}