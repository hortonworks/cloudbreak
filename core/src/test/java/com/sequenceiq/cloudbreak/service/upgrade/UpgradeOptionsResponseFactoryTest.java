package com.sequenceiq.cloudbreak.service.upgrade;

import static com.sequenceiq.common.model.ImageCatalogPlatform.imageCatalogPlatform;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.image.ImageComponentVersions;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.upgrade.UpgradeV4Response;
import com.sequenceiq.cloudbreak.cloud.model.Image;
import com.sequenceiq.cloudbreak.common.service.PlatformStringTransformer;
import com.sequenceiq.cloudbreak.core.CloudbreakImageNotFoundException;
import com.sequenceiq.cloudbreak.service.image.ImageService;
import com.sequenceiq.cloudbreak.service.upgrade.image.ImageFilterResult;
import com.sequenceiq.common.model.ImageCatalogPlatform;

@ExtendWith(MockitoExtension.class)
public class UpgradeOptionsResponseFactoryTest {

    private static final String CLOUD_PLATFORM = "aws";

    private static final String V_7_0_3 = "7.0.3";

    private static final String V_7_0_2 = "7.0.2";

    private static final long CREATED = 1578676431L;

    private static final String IMAGE_ID = "f58c5f97-4609-4b47-6498-1c1bc6a4501c";

    private static final String IMAGE_NAME = "ami-0fd22f4513051ac2b";

    private static final String IMAGE_CATALOG_NAME = "/images";

    private static final String REGION = "eu-west-1";

    private static final String DATE = "2022-03-21";

    @InjectMocks
    private UpgradeOptionsResponseFactory underTest;

    @Mock
    private ImageService imageService;

    @Mock
    private PlatformStringTransformer platformStringTransformer;

    @Mock
    private ComponentVersionProvider componentVersionProvider;

    @Test
    public void testCreateV4ResponseShouldReturnTheUpgradeOptionsFromTheGivenParameters() throws CloudbreakImageNotFoundException {
        ImageCatalogPlatform imageCatalogPlatform = imageCatalogPlatform(CLOUD_PLATFORM);
        Map<String, String> packageVersions = createPackageVersions();
        ImageComponentVersions expectedPackageVersions = createExpectedPackageVersions();
        Image currentImage = createModelImage(packageVersions);
        ImageFilterResult availableImages = createAvailableImages(packageVersions);

        when(platformStringTransformer.getPlatformStringForImageCatalogByRegion(anyString(), anyString())).thenReturn(imageCatalogPlatform);
        when(imageService.determineImageName(CLOUD_PLATFORM, imageCatalogPlatform, REGION, availableImages.getImages().get(0)))
                .thenReturn(IMAGE_NAME);
        when(componentVersionProvider.getComponentVersions(eq(packageVersions), any(), any())).thenReturn(expectedPackageVersions);

        UpgradeV4Response actual = underTest.createV4Response(currentImage, availableImages, CLOUD_PLATFORM, REGION, IMAGE_CATALOG_NAME);

        assertEquals(IMAGE_ID, actual.getCurrent().getImageId());
        assertEquals(IMAGE_CATALOG_NAME, actual.getCurrent().getImageCatalogName());
        assertEquals(IMAGE_NAME, actual.getCurrent().getImageName());
        assertEquals(expectedPackageVersions, actual.getCurrent().getComponentVersions());
        assertEquals(CREATED, actual.getCurrent().getCreated().longValue());
        assertEquals(DATE, actual.getCurrent().getDate());
        assertEquals(IMAGE_ID, actual.getUpgradeCandidates().get(0).getImageId());
        assertEquals(IMAGE_CATALOG_NAME, actual.getUpgradeCandidates().get(0).getImageCatalogName());
        assertEquals(IMAGE_NAME, actual.getUpgradeCandidates().get(0).getImageName());
        assertEquals(expectedPackageVersions, actual.getUpgradeCandidates().get(0).getComponentVersions());
        assertEquals(CREATED, actual.getUpgradeCandidates().get(0).getCreated().longValue());
        assertEquals(DATE, actual.getUpgradeCandidates().get(0).getDate());
    }

    private ImageFilterResult createAvailableImages(Map<String, String> packageVersions) {
        return new ImageFilterResult(List.of(createImage(packageVersions)), null);
    }

    private com.sequenceiq.cloudbreak.cloud.model.catalog.Image createImage(Map<String, String> packageVersions) {
        return new com.sequenceiq.cloudbreak.cloud.model.catalog.Image(DATE, CREATED, null, null, null, IMAGE_ID, null, null,
                Map.of(CLOUD_PLATFORM, Map.of(REGION, IMAGE_NAME)), null, null,
                packageVersions, null, null, null, true, null, null, null);
    }

    private Image createModelImage(Map<String, String> packageVersions) {
        return new Image(IMAGE_NAME, null, null, null, null, IMAGE_CATALOG_NAME, IMAGE_ID, packageVersions, DATE, CREATED);
    }

    private Map<String, String> createPackageVersions() {
        return Map.of(
                "cm", V_7_0_3,
                "stack", V_7_0_2);
    }

    private ImageComponentVersions createExpectedPackageVersions() {
        ImageComponentVersions imageComponentVersions = new ImageComponentVersions();
        imageComponentVersions.setCm(V_7_0_3);
        imageComponentVersions.setCdp(V_7_0_2);
        return imageComponentVersions;
    }

}