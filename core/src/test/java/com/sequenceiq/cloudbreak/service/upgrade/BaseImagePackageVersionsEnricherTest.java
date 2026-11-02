package com.sequenceiq.cloudbreak.service.upgrade;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.cloud.model.ClouderaManagerProduct;
import com.sequenceiq.cloudbreak.cloud.model.ClouderaManagerRepo;
import com.sequenceiq.cloudbreak.cloud.model.Image;
import com.sequenceiq.cloudbreak.cloud.model.catalog.ImagePackageVersion;
import com.sequenceiq.cloudbreak.cluster.service.ClusterComponentConfigProvider;

@ExtendWith(MockitoExtension.class)
class BaseImagePackageVersionsEnricherTest {

    private static final Long CLUSTER_ID = 2L;

    private static final String CM_VERSION = "7.4.2";

    private static final String CM_BUILD_NUMBER = "15633910";

    private static final String STACK_VERSION = "7.2.17";

    private static final String CDH_BUILD_NUMBER = "64507825";

    @InjectMocks
    private BaseImagePackageVersionsEnricher underTest;

    @Mock
    private ClusterComponentConfigProvider clusterComponentConfigProvider;

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void enrichShouldUseClusterComponentsForBaseImage(boolean force) {
        Image image = createImage(Map.of(ImagePackageVersion.SALT.getKey(), "3006.10"));
        mockClusterComponents();

        Image actual = force ? underTest.enrichWithClusterComponents(image, CLUSTER_ID) : underTest.enrich(image, CLUSTER_ID);

        assertEquals(CM_VERSION, actual.getPackageVersion(ImagePackageVersion.CM));
        assertEquals(CM_BUILD_NUMBER, actual.getPackageVersion(ImagePackageVersion.CM_BUILD_NUMBER));
        assertEquals(STACK_VERSION, actual.getPackageVersion(ImagePackageVersion.STACK));
        assertEquals(CDH_BUILD_NUMBER, actual.getPackageVersion(ImagePackageVersion.CDH_BUILD_NUMBER));
        assertEquals("2.1.7", actual.getPackageVersion(ImagePackageVersion.CFM));
        assertEquals(image.getPackageVersion(ImagePackageVersion.SALT), actual.getPackageVersion(ImagePackageVersion.SALT));
    }

    @Test
    void enrichShouldReturnSameImageWhenNotBaseImage() {
        Image image = createImage(Map.of(
                ImagePackageVersion.CM.getKey(), CM_VERSION,
                ImagePackageVersion.CDH_BUILD_NUMBER.getKey(), CDH_BUILD_NUMBER));

        Image actual = underTest.enrich(image, CLUSTER_ID);

        assertSame(image, actual);
        verifyNoInteractions(clusterComponentConfigProvider);
    }

    @Test
    void forcedEnrichmentShouldUpdatePrewarmedImageWithCurrentVersions() {
        Image image = createImage(Map.of(
                ImagePackageVersion.CM.getKey(), "7.4.1",
                ImagePackageVersion.CM_BUILD_NUMBER.getKey(), "12345",
                ImagePackageVersion.STACK.getKey(), "7.2.16",
                ImagePackageVersion.CDH_BUILD_NUMBER.getKey(), "54321"));
        mockClusterComponents();

        Image actual = underTest.enrichWithClusterComponents(image, CLUSTER_ID);

        assertEquals(CM_VERSION, actual.getPackageVersion(ImagePackageVersion.CM));
        assertEquals(CM_BUILD_NUMBER, actual.getPackageVersion(ImagePackageVersion.CM_BUILD_NUMBER));
        assertEquals(STACK_VERSION, actual.getPackageVersion(ImagePackageVersion.STACK));
        assertEquals(CDH_BUILD_NUMBER, actual.getPackageVersion(ImagePackageVersion.CDH_BUILD_NUMBER));
    }

    @Test
    void enrichShouldPreserveImagePackagesWhenClusterComponentsAreMissing() {
        Map<String, String> packageVersions = Map.of(ImagePackageVersion.SALT.getKey(), "3006.10");
        Image image = createImage(packageVersions);

        Image actual = underTest.enrich(image, CLUSTER_ID);

        assertEquals(packageVersions, actual.getPackageVersions());
    }

    private void mockClusterComponents() {
        ClouderaManagerRepo repo = new ClouderaManagerRepo().withVersion(CM_VERSION).withBuildNumber(CM_BUILD_NUMBER);
        ClouderaManagerProduct cdh = new ClouderaManagerProduct().withName("CDH").withVersion(STACK_VERSION + "-" + CDH_BUILD_NUMBER);
        ClouderaManagerProduct cfm = new ClouderaManagerProduct().withName("CFM").withVersion("2.1.7");
        when(clusterComponentConfigProvider.getClouderaManagerRepoDetails(CLUSTER_ID)).thenReturn(repo);
        when(clusterComponentConfigProvider.getCdhProduct(CLUSTER_ID)).thenReturn(Optional.of(cdh));
        when(clusterComponentConfigProvider.getClouderaManagerProductDetails(CLUSTER_ID)).thenReturn(List.of(cdh, cfm));
    }

    private Image createImage(Map<String, String> packageVersions) {
        return Image.builder()
                .withImageId("image-id")
                .withImageCatalogName("catalog")
                .withPackageVersions(packageVersions)
                .build();
    }
}
