package com.sequenceiq.cloudbreak.service.image.catalog;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.testcontainers.shaded.org.apache.commons.lang.NotImplementedException;

import com.sequenceiq.cloudbreak.cloud.model.catalog.CloudbreakImageCatalogV3;
import com.sequenceiq.cloudbreak.cloud.model.catalog.Image;
import com.sequenceiq.cloudbreak.core.CloudbreakImageCatalogException;
import com.sequenceiq.cloudbreak.service.image.ImageFilter;
import com.sequenceiq.cloudbreak.service.image.StatedImages;
import com.sequenceiq.cloudbreak.service.image.catalog.model.ImageCatalogMetaData;
import com.sequenceiq.cloudbreak.service.upgrade.image.ImageFilterResult;

@ExtendWith(MockitoExtension.class)
public class ImageCatalogServiceTest {

    private static final String RUNTIME_721 = "7.2.1";

    private static final String RUNTIME_722 = "7.2.2";

    private static final String RUNTIME_7210 = "7.2.10";

    private ImageCatalogService victim;

    @Mock
    private CloudbreakImageCatalogV3 imageCatalogV3;

    @BeforeEach
    public void initTests() {
        Image runtimeImage721 = mock(Image.class);
        Image runtimeImage722 = mock(Image.class);
        Image runtimeImage7210 = mock(Image.class);
        imageCatalogV3 = mock(CloudbreakImageCatalogV3.class);

        when(runtimeImage721.getVersion()).thenReturn(RUNTIME_721);
        when(runtimeImage722.getVersion()).thenReturn(RUNTIME_722);
        when(runtimeImage7210.getVersion()).thenReturn(RUNTIME_7210);

        ImageFilterResult imageFilterResult = new ImageFilterResult(List.of(runtimeImage722, runtimeImage7210, runtimeImage721, runtimeImage721), null);

        victim = new TestImplementation(imageCatalogV3, imageFilterResult);
    }

    @Test
    public void testGetImageMetaDataContainsDistinctListOfVersionsInReversedOrder() {
        ImageCatalogMetaData imageCatalogMetaData = victim.getImageCatalogMetaData(imageCatalogV3);

        assertEquals(List.of(RUNTIME_7210, RUNTIME_722, RUNTIME_721), imageCatalogMetaData.getRuntimeVersions());
    }

    private static class TestImplementation implements ImageCatalogService {

        private CloudbreakImageCatalogV3 imageCatalogV3;

        private ImageFilterResult imageFilterResult;

        TestImplementation(CloudbreakImageCatalogV3 imageCatalogV3, ImageFilterResult imageFilterResult) {
            this.imageCatalogV3 = imageCatalogV3;
            this.imageFilterResult = imageFilterResult;
        }

        @Override
        public StatedImages getImages(CloudbreakImageCatalogV3 imageCatalogV3, ImageFilter imageFilter) {
            throw new NotImplementedException();
        }

        @Override
        public void validate(CloudbreakImageCatalogV3 imageCatalogV3) throws CloudbreakImageCatalogException {
            throw new NotImplementedException();
        }

        @Override
        public ImageFilterResult getImageFilterResult(CloudbreakImageCatalogV3 imageCatalogV3) {
            assertEquals(this.imageCatalogV3, imageCatalogV3);
            return imageFilterResult;
        }
    }
}