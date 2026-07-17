package com.sequenceiq.cloudbreak.service.upgrade;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.cloud.model.ClouderaManagerProduct;
import com.sequenceiq.cloudbreak.cloud.model.ClouderaManagerRepo;
import com.sequenceiq.cloudbreak.cloud.model.Image;
import com.sequenceiq.cloudbreak.cloud.model.catalog.ImagePackageVersion;
import com.sequenceiq.cloudbreak.cluster.service.ClusterComponentConfigProvider;
import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;
import com.sequenceiq.cloudbreak.service.stack.InstanceMetaDataService;

@ExtendWith(MockitoExtension.class)
class BaseImagePackageVersionsEnricherTest {

    private static final Long STACK_ID = 1L;

    private static final Long CLUSTER_ID = 2L;

    private static final String CM_VERSION = "7.4.2";

    private static final String CM_BUILD_NUMBER = "15633910";

    private static final String STACK_VERSION = "7.2.17";

    private static final String CDH_BUILD_NUMBER = "64507825";

    @InjectMocks
    private BaseImagePackageVersionsEnricher underTest;

    @Mock
    private InstanceMetaDataService instanceMetaDataService;

    @Mock
    private ClusterComponentConfigProvider clusterComponentConfigProvider;

    @Test
    void enrichShouldReturnSameImageWhenNotBaseImage() {
        Image image = createImage(Map.of(
                ImagePackageVersion.CM.getKey(), CM_VERSION,
                ImagePackageVersion.CDH_BUILD_NUMBER.getKey(), CDH_BUILD_NUMBER));

        Image actual = underTest.enrich(image, createStack());

        assertSame(image, actual);
    }

    @Test
    void enrichShouldUseInstanceMetadataPackageVersionsForBaseImage() {
        Image image = createBaseImage();
        Stack stack = createStack();
        Map<String, String> instancePackageVersions = Map.of(
                ImagePackageVersion.CM.getKey(), CM_VERSION,
                ImagePackageVersion.STACK.getKey(), STACK_VERSION);
        when(instanceMetaDataService.getNotDeletedAndNotZombieInstanceMetadataByStackId(STACK_ID))
                .thenReturn(Set.of(createInstanceMetaData(instancePackageVersions)));

        Image actual = underTest.enrich(image, stack);

        assertEquals(CM_VERSION, actual.getPackageVersion(ImagePackageVersion.CM));
        assertEquals(STACK_VERSION, actual.getPackageVersion(ImagePackageVersion.STACK));
    }

    @Test
    void enrichShouldSkipInstanceMetadataWithNullImageAndUseNextInstance() {
        Image image = createBaseImage();
        Stack stack = createStack();
        InstanceMetaData requestedInstance = createInstanceMetaDataWithoutImage();
        Map<String, String> instancePackageVersions = Map.of(
                ImagePackageVersion.CM.getKey(), CM_VERSION,
                ImagePackageVersion.STACK.getKey(), STACK_VERSION);
        when(instanceMetaDataService.getNotDeletedAndNotZombieInstanceMetadataByStackId(STACK_ID))
                .thenReturn(Set.of(requestedInstance, createInstanceMetaData(instancePackageVersions)));

        Image actual = underTest.enrich(image, stack);

        assertEquals(CM_VERSION, actual.getPackageVersion(ImagePackageVersion.CM));
        assertEquals(STACK_VERSION, actual.getPackageVersion(ImagePackageVersion.STACK));
    }

    @Test
    void enrichShouldFallbackToClusterComponentsWhenInstanceMetadataHasNullImage() {
        Image image = createBaseImage();
        Stack stack = createStack();
        when(instanceMetaDataService.getNotDeletedAndNotZombieInstanceMetadataByStackId(STACK_ID))
                .thenReturn(Set.of(createInstanceMetaDataWithoutImage()));

        ClouderaManagerRepo clouderaManagerRepo = new ClouderaManagerRepo();
        clouderaManagerRepo.setVersion(CM_VERSION);
        clouderaManagerRepo.setBuildNumber(CM_BUILD_NUMBER);
        when(clusterComponentConfigProvider.getClouderaManagerRepoDetails(CLUSTER_ID)).thenReturn(clouderaManagerRepo);
        when(clusterComponentConfigProvider.getCdhProduct(CLUSTER_ID)).thenReturn(java.util.Optional.of(
                new ClouderaManagerProduct().withName("CDH").withVersion(STACK_VERSION + "-" + CDH_BUILD_NUMBER)));
        when(clusterComponentConfigProvider.getClouderaManagerProductDetails(CLUSTER_ID)).thenReturn(List.of());

        Image actual = underTest.enrich(image, stack);

        assertEquals(CM_VERSION, actual.getPackageVersion(ImagePackageVersion.CM));
        assertEquals(CM_BUILD_NUMBER, actual.getPackageVersion(ImagePackageVersion.CM_BUILD_NUMBER));
        assertEquals(STACK_VERSION, actual.getPackageVersion(ImagePackageVersion.STACK));
        assertEquals(CDH_BUILD_NUMBER, actual.getPackageVersion(ImagePackageVersion.CDH_BUILD_NUMBER));
    }

    @Test
    void enrichShouldFallbackToClusterComponentsWhenInstanceMetadataIsEmpty() {
        Image image = createBaseImage();
        Stack stack = createStack();
        when(instanceMetaDataService.getNotDeletedAndNotZombieInstanceMetadataByStackId(STACK_ID)).thenReturn(Set.of());

        ClouderaManagerRepo clouderaManagerRepo = new ClouderaManagerRepo();
        clouderaManagerRepo.setVersion(CM_VERSION);
        clouderaManagerRepo.setBuildNumber(CM_BUILD_NUMBER);
        when(clusterComponentConfigProvider.getClouderaManagerRepoDetails(CLUSTER_ID)).thenReturn(clouderaManagerRepo);
        when(clusterComponentConfigProvider.getCdhProduct(CLUSTER_ID)).thenReturn(java.util.Optional.of(
                new ClouderaManagerProduct().withName("CDH").withVersion(STACK_VERSION + "-" + CDH_BUILD_NUMBER)));
        when(clusterComponentConfigProvider.getClouderaManagerProductDetails(CLUSTER_ID)).thenReturn(List.of());

        Image actual = underTest.enrich(image, stack);

        assertEquals(CM_VERSION, actual.getPackageVersion(ImagePackageVersion.CM));
        assertEquals(CM_BUILD_NUMBER, actual.getPackageVersion(ImagePackageVersion.CM_BUILD_NUMBER));
        assertEquals(STACK_VERSION, actual.getPackageVersion(ImagePackageVersion.STACK));
        assertEquals(CDH_BUILD_NUMBER, actual.getPackageVersion(ImagePackageVersion.CDH_BUILD_NUMBER));
    }

    @Test
    void enrichShouldReturnOriginalImageWhenNoPackageVersionsCanBeResolved() {
        Image image = createBaseImage();
        Stack stack = createStack();
        when(instanceMetaDataService.getNotDeletedAndNotZombieInstanceMetadataByStackId(STACK_ID)).thenReturn(Set.of());
        when(clusterComponentConfigProvider.getClouderaManagerRepoDetails(CLUSTER_ID)).thenReturn(null);
        when(clusterComponentConfigProvider.getCdhProduct(CLUSTER_ID)).thenReturn(java.util.Optional.empty());
        when(clusterComponentConfigProvider.getClouderaManagerProductDetails(CLUSTER_ID)).thenReturn(List.of());

        Image actual = underTest.enrich(image, stack);

        assertSame(image, actual);
    }

    private Image createBaseImage() {
        return createImage(new HashMap<>());
    }

    private Image createImage(Map<String, String> packageVersions) {
        return Image.builder()
                .withImageId("image-id")
                .withImageCatalogName("catalog")
                .withPackageVersions(packageVersions)
                .build();
    }

    private Stack createStack() {
        Cluster cluster = new Cluster();
        cluster.setId(CLUSTER_ID);
        Stack stack = new Stack();
        stack.setId(STACK_ID);
        stack.setCluster(cluster);
        return stack;
    }

    private InstanceMetaData createInstanceMetaDataWithoutImage() {
        InstanceMetaData instanceMetaData = new InstanceMetaData();
        instanceMetaData.setInstanceId("requested-instance");
        assertNull(instanceMetaData.getImage());
        return instanceMetaData;
    }

    private InstanceMetaData createInstanceMetaData(Map<String, String> packageVersions) {
        InstanceMetaData instanceMetaData = new InstanceMetaData();
        instanceMetaData.setInstanceId("instance-1");
        instanceMetaData.setImage(new Json(Image.builder().withPackageVersions(packageVersions).build()));
        return instanceMetaData;
    }
}
