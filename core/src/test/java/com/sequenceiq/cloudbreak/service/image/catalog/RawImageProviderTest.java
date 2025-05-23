package com.sequenceiq.cloudbreak.service.image.catalog;

import static com.sequenceiq.cloudbreak.common.mappable.CloudPlatform.AWS;
import static com.sequenceiq.cloudbreak.common.mappable.CloudPlatform.AZURE;
import static com.sequenceiq.common.model.ImageCatalogPlatform.imageCatalogPlatform;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.AdditionalAnswers.returnsSecondArg;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.cloud.model.catalog.CloudbreakImageCatalogV3;
import com.sequenceiq.cloudbreak.cloud.model.catalog.Image;
import com.sequenceiq.cloudbreak.cloud.model.catalog.Images;
import com.sequenceiq.cloudbreak.domain.ImageCatalog;
import com.sequenceiq.cloudbreak.service.image.ImageFilter;
import com.sequenceiq.cloudbreak.service.image.ProviderSpecificImageFilter;
import com.sequenceiq.cloudbreak.service.image.StatedImages;

@ExtendWith(MockitoExtension.class)
public class RawImageProviderTest {

    private static final String BASE_IMAGE_AWS = "base-aws-1";

    private static final String CDH_IMAGE_AWS = "cdh-aws-1";

    private static final String FREEIPA_IMAGE_AWS = "freeipa-aws-1";

    private static final String IMAGE_CATALOG_URL = "image-catalog-url";

    private static final String IMAGE_CATALOG_NAME = "image-catalog-name";

    private static final String BASE_IMAGE_AZURE = "base-az-2";

    private static final String CDH_IMAGE_AZURE = "cdh-az-2";

    private static final String FREEIPA_IMAGE_AZURE = "freeipa-az-2";

    @Mock
    private ProviderSpecificImageFilter providerSpecificImageFilter;

    @InjectMocks
    private RawImageProvider underTest;

    @Test
    void testGetImagesShouldReturnOnlyTheAwsImagesFromTheImageCatalog() {
        ImageFilter imageFilter = createImageFilter();
        CloudbreakImageCatalogV3 imageCatalogV3 = createImageCatalog();

        when(providerSpecificImageFilter.filterImages(any(), anyList())).then(returnsSecondArg());

        StatedImages actual = underTest.getImages(imageCatalogV3, imageFilter);

        assertEquals(IMAGE_CATALOG_NAME, actual.getImageCatalogName());
        assertEquals(IMAGE_CATALOG_URL, actual.getImageCatalogUrl());
        Images images = actual.getImages();
        assertTrue(images.getBaseImages().stream().anyMatch(image -> BASE_IMAGE_AWS.equals(image.getUuid())));
        assertTrue(images.getBaseImages().stream().noneMatch(image -> BASE_IMAGE_AZURE.equals(image.getUuid())));
        assertTrue(images.getCdhImages().stream().anyMatch(image -> CDH_IMAGE_AWS.equals(image.getUuid())));
        assertTrue(images.getCdhImages().stream().noneMatch(image -> CDH_IMAGE_AZURE.equals(image.getUuid())));
        assertTrue(images.getFreeIpaImages().stream().anyMatch(image -> FREEIPA_IMAGE_AWS.equals(image.getUuid())));
        assertTrue(images.getFreeIpaImages().stream().noneMatch(image -> FREEIPA_IMAGE_AZURE.equals(image.getUuid())));
    }

    private CloudbreakImageCatalogV3 createImageCatalog() {
        return new CloudbreakImageCatalogV3(createImages(), null);
    }

    private Images createImages() {
        return new Images(
                List.of(createImage(BASE_IMAGE_AWS, AWS.name()), createImage(BASE_IMAGE_AZURE, AZURE.name())),
                List.of(createImage(CDH_IMAGE_AWS, AWS.name()), createImage(CDH_IMAGE_AZURE, AZURE.name())),
                List.of(createImage(FREEIPA_IMAGE_AWS, AWS.name()), createImage(FREEIPA_IMAGE_AZURE, AZURE.name())),
                null);
    }

    private ImageFilter createImageFilter() {
        ImageCatalog imageCatalog = new ImageCatalog();
        imageCatalog.setImageCatalogUrl(IMAGE_CATALOG_URL);
        imageCatalog.setName(IMAGE_CATALOG_NAME);
        return ImageFilter.builder()
                .withImageCatalog(imageCatalog)
                .withPlatforms(Collections.singleton(imageCatalogPlatform(AWS.name())))
                .build();
    }

    private Image createImage(String imageId, String cloudPlatform) {
        return Image.builder()
                .withUuid(imageId)
                .withImageSetsByProvider(Map.of(cloudPlatform, Collections.emptyMap()))
                .withAdvertised(false)
                .build();
    }

}