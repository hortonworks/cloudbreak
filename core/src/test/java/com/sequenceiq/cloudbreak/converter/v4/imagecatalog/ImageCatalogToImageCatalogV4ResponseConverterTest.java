package com.sequenceiq.cloudbreak.converter.v4.imagecatalog;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import jakarta.inject.Inject;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import com.sequenceiq.cloudbreak.domain.ImageCatalog;
import com.sequenceiq.cloudbreak.service.image.ImageCatalogService;
import com.sequenceiq.cloudbreak.service.user.UserService;
import com.sequenceiq.cloudbreak.structuredevent.LegacyRestRequestThreadLocalService;

@ExtendWith(SpringExtension.class)
public class ImageCatalogToImageCatalogV4ResponseConverterTest {
    public static final String NAME = "testCatalog";

    private static final ImageCatalog IMAGE_CATALOG = new ImageCatalog();

    @MockBean
    private ImageCatalogService imageCatalogService;

    @MockBean
    private UserService userService;

    @MockBean
    private LegacyRestRequestThreadLocalService legacyRestRequestThreadLocalService;

    @Inject
    private ImageCatalogToImageCatalogV4ResponseConverter converterUnderTest;

    @BeforeEach
    public void setup() {
        IMAGE_CATALOG.setName(NAME);
    }

    @Test
    public void testImageCatalogResponseHasCreated() {
        assertNotNull(converterUnderTest.convert(IMAGE_CATALOG).getCreated());
    }

    @Configuration
    @Import(ImageCatalogToImageCatalogV4ResponseConverter.class)
    static class Config {
    }
}
