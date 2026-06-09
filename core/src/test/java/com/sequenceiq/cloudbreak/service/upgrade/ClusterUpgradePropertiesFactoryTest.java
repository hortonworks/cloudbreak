package com.sequenceiq.cloudbreak.service.upgrade;

import static com.sequenceiq.cloudbreak.cloud.model.catalog.ImagePackageVersion.CDH_BUILD_NUMBER;
import static com.sequenceiq.cloudbreak.cloud.model.catalog.ImagePackageVersion.STACK;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.cloud.model.ClouderaManagerProduct;
import com.sequenceiq.cloudbreak.cloud.model.Image;
import com.sequenceiq.cloudbreak.converter.ImageToClouderaManagerRepoConverter;
import com.sequenceiq.cloudbreak.core.CloudbreakImageCatalogException;
import com.sequenceiq.cloudbreak.core.CloudbreakImageNotFoundException;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.service.ComponentConfigProviderService;
import com.sequenceiq.cloudbreak.service.image.ImageCatalogService;
import com.sequenceiq.cloudbreak.service.image.StatedImage;
import com.sequenceiq.cloudbreak.service.parcel.ClouderaManagerProductTransformer;
import com.sequenceiq.cloudbreak.service.stack.StackImageService;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.cloudbreak.workspace.model.Workspace;
import com.sequenceiq.common.model.OsType;

@ExtendWith(MockitoExtension.class)
class ClusterUpgradePropertiesFactoryTest {

    private static final long STACK_ID = 1L;

    private static final long WORKSPACE_ID = 2L;

    private static final String TARGET_IMAGE_ID = "targetImageId";

    private static final String IMAGE_CATALOG_NAME = "imageCatalogName";

    private static final String IMAGE_CATALOG_URL = "imageCatalogUrl";

    @Mock
    private ComponentConfigProviderService componentConfigProviderService;

    @Mock
    private ImageCatalogService imageCatalogService;

    @Mock
    private StackService stackService;

    @Mock
    private StackImageService stackImageService;

    @Mock
    private ClouderaManagerProductTransformer clouderaManagerProductTransformer;

    @Mock
    private ImageToClouderaManagerRepoConverter imageToClouderaManagerRepoConverter;

    @InjectMocks
    private ClusterUpgradePropertiesFactory underTest;

    @Test
    void testCreate() throws CloudbreakImageNotFoundException, CloudbreakImageCatalogException {
        Image currentImage = currentImage();
        com.sequenceiq.cloudbreak.cloud.model.catalog.Image targetCatalogImage = targetCatalogImage();
        Stack stack = mock(Stack.class);
        StatedImage targetStatedImage = StatedImage.statedImage(targetCatalogImage, IMAGE_CATALOG_URL, IMAGE_CATALOG_NAME);
        ClouderaManagerProduct cdhProduct = new ClouderaManagerProduct().withName("CDH").withVersion("7.2.18");
        Set<ClouderaManagerProduct> products = new HashSet<>(Set.of(cdhProduct));
        Image targetCloudImage = Image.builder().withImageName("targetImageName").build();

        when(componentConfigProviderService.getImage(STACK_ID)).thenReturn(currentImage);
        when(stackService.get(STACK_ID)).thenReturn(stack);
        Workspace workspace = mock(Workspace.class);
        when(stack.getWorkspace()).thenReturn(workspace);
        when(workspace.getId()).thenReturn(WORKSPACE_ID);
        when(stack.isDatalake()).thenReturn(false);
        when(imageCatalogService.getImage(WORKSPACE_ID, IMAGE_CATALOG_URL, IMAGE_CATALOG_NAME, TARGET_IMAGE_ID)).thenReturn(targetStatedImage);
        when(clouderaManagerProductTransformer.transform(targetCatalogImage, true, true)).thenReturn(products);
        when(imageToClouderaManagerRepoConverter.convert(targetCatalogImage)).thenReturn(new com.sequenceiq.cloudbreak.cloud.model.ClouderaManagerRepo());
        when(stackImageService.getImageModelFromStatedImage(stack, currentImage, targetStatedImage)).thenReturn(targetCloudImage);

        ClusterUpgradeProperties properties = underTest.create(STACK_ID, TARGET_IMAGE_ID, false, true, false);

        assertEquals(TARGET_IMAGE_ID, properties.targetImageId());
        assertEquals(IMAGE_CATALOG_NAME, properties.imageCatalogName());
        assertEquals(IMAGE_CATALOG_URL, properties.imageCatalogUrl());
        assertEquals("7.2.18", properties.runtimeVersion());
        assertEquals(OsType.RHEL8, properties.currentOsType());
        assertEquals(false, properties.lockComponents());
        assertEquals(true, properties.rollingUpgradeEnabled());
        assertEquals(false, properties.replaceVms());
        assertNotNull(properties.cdhParcel());
        assertEquals(products, properties.getAllTargetProducts());
        assertEquals("currentImageId", properties.currentImageId());
        assertEquals(IMAGE_CATALOG_NAME, properties.currentImageCatalogName());
        assertEquals("7.2.17", properties.currentRuntimeVersion());
        assertEquals("12345", properties.cdhBuildNumber());
        assertEquals("targetImageName", properties.targetImageName());
        assertEquals(OsType.RHEL8, properties.targetOsType());
        assertEquals("currentImageName", properties.getCurrentImage().imageName());
        assertEquals("redhat8", properties.getCurrentImage().os());
        assertEquals("x86_64", properties.getCurrentImage().architecture());
        assertEquals("2024-01-01", properties.getCurrentImage().date());
        assertEquals(1L, properties.getCurrentImage().created());
        com.sequenceiq.cloudbreak.cloud.model.Image rebuiltCurrentImage = properties.toCurrentCloudImage();
        assertEquals("currentImageId", rebuiltCurrentImage.getImageId());
        assertEquals("currentImageName", rebuiltCurrentImage.getImageName());
        assertEquals(IMAGE_CATALOG_URL, rebuiltCurrentImage.getImageCatalogUrl());
        assertEquals("x86_64", rebuiltCurrentImage.getArchitecture());
        verify(imageCatalogService).getImage(WORKSPACE_ID, IMAGE_CATALOG_URL, IMAGE_CATALOG_NAME, TARGET_IMAGE_ID);
        verify(stackImageService).getImageModelFromStatedImage(stack, currentImage, targetStatedImage);
    }

    @Test
    void testCreateForDatalakeExcludesPreWarmParcels() throws CloudbreakImageNotFoundException, CloudbreakImageCatalogException {
        Image currentImage = currentImage();
        com.sequenceiq.cloudbreak.cloud.model.catalog.Image targetCatalogImage = targetCatalogImage();
        Stack stack = setupStack(true);
        StatedImage targetStatedImage = StatedImage.statedImage(targetCatalogImage, IMAGE_CATALOG_URL, IMAGE_CATALOG_NAME);
        ClouderaManagerProduct cdhProduct = new ClouderaManagerProduct().withName("CDH").withVersion("7.2.18");
        Set<ClouderaManagerProduct> products = new HashSet<>(Set.of(cdhProduct));

        when(componentConfigProviderService.getImage(STACK_ID)).thenReturn(currentImage);
        when(imageCatalogService.getImage(WORKSPACE_ID, IMAGE_CATALOG_URL, IMAGE_CATALOG_NAME, TARGET_IMAGE_ID)).thenReturn(targetStatedImage);
        when(clouderaManagerProductTransformer.transform(targetCatalogImage, true, false)).thenReturn(products);
        when(imageToClouderaManagerRepoConverter.convert(targetCatalogImage)).thenReturn(new com.sequenceiq.cloudbreak.cloud.model.ClouderaManagerRepo());
        when(stackImageService.getImageModelFromStatedImage(stack, currentImage, targetStatedImage))
                .thenReturn(Image.builder().withImageName("targetImageName").build());

        ClusterUpgradeProperties properties = underTest.create(STACK_ID, TARGET_IMAGE_ID, false, true, false);

        assertTrue(properties.getPreWarmParcels().isEmpty());
        verify(clouderaManagerProductTransformer).transform(targetCatalogImage, true, false);
    }

    @Test
    void testCreateWithoutCdhParcelFiltersPreWarmParcels() throws CloudbreakImageNotFoundException, CloudbreakImageCatalogException {
        Image currentImage = currentImage();
        com.sequenceiq.cloudbreak.cloud.model.catalog.Image targetCatalogImage = targetCatalogImage();
        Stack stack = setupStack(false);
        StatedImage targetStatedImage = StatedImage.statedImage(targetCatalogImage, IMAGE_CATALOG_URL, IMAGE_CATALOG_NAME);
        ClouderaManagerProduct preWarmProduct = new ClouderaManagerProduct().withName("FLINK").withVersion("1.0.0");
        Set<ClouderaManagerProduct> products = new HashSet<>(Set.of(preWarmProduct));

        when(componentConfigProviderService.getImage(STACK_ID)).thenReturn(currentImage);
        when(imageCatalogService.getImage(WORKSPACE_ID, IMAGE_CATALOG_URL, IMAGE_CATALOG_NAME, TARGET_IMAGE_ID)).thenReturn(targetStatedImage);
        when(clouderaManagerProductTransformer.transform(targetCatalogImage, true, true)).thenReturn(products);
        when(imageToClouderaManagerRepoConverter.convert(targetCatalogImage)).thenReturn(new com.sequenceiq.cloudbreak.cloud.model.ClouderaManagerRepo());
        when(stackImageService.getImageModelFromStatedImage(stack, currentImage, targetStatedImage))
                .thenReturn(Image.builder().withImageName("targetImageName").build());

        ClusterUpgradeProperties properties = underTest.create(STACK_ID, TARGET_IMAGE_ID, false, true, false);

        assertNull(properties.cdhParcel());
        assertEquals(products, properties.getPreWarmParcels());
        assertEquals(products, properties.getAllTargetProducts());
    }

    @Test
    void testCreateRemovesCdhFromPreWarmParcels() throws CloudbreakImageNotFoundException, CloudbreakImageCatalogException {
        Image currentImage = currentImage();
        com.sequenceiq.cloudbreak.cloud.model.catalog.Image targetCatalogImage = targetCatalogImage();
        Stack stack = setupStack(false);
        StatedImage targetStatedImage = StatedImage.statedImage(targetCatalogImage, IMAGE_CATALOG_URL, IMAGE_CATALOG_NAME);
        ClouderaManagerProduct cdhProduct = new ClouderaManagerProduct().withName("CDH").withVersion("7.2.18");
        ClouderaManagerProduct preWarmProduct = new ClouderaManagerProduct().withName("FLINK").withVersion("1.0.0");
        Set<ClouderaManagerProduct> products = new HashSet<>(Set.of(cdhProduct, preWarmProduct));

        when(componentConfigProviderService.getImage(STACK_ID)).thenReturn(currentImage);
        when(imageCatalogService.getImage(WORKSPACE_ID, IMAGE_CATALOG_URL, IMAGE_CATALOG_NAME, TARGET_IMAGE_ID)).thenReturn(targetStatedImage);
        when(clouderaManagerProductTransformer.transform(targetCatalogImage, true, true)).thenReturn(products);
        when(imageToClouderaManagerRepoConverter.convert(targetCatalogImage)).thenReturn(new com.sequenceiq.cloudbreak.cloud.model.ClouderaManagerRepo());
        when(stackImageService.getImageModelFromStatedImage(stack, currentImage, targetStatedImage))
                .thenReturn(Image.builder().withImageName("targetImageName").build());

        ClusterUpgradeProperties properties = underTest.create(STACK_ID, TARGET_IMAGE_ID, false, true, false);

        assertEquals(cdhProduct, properties.cdhParcel());
        assertEquals(Set.of(preWarmProduct), properties.getPreWarmParcels());
        assertEquals(products, properties.getAllTargetProducts());
    }

    @Test
    void testCreateUsesStackPackageVersionForRuntimeVersion() throws CloudbreakImageNotFoundException, CloudbreakImageCatalogException {
        Image currentImage = currentImage();
        com.sequenceiq.cloudbreak.cloud.model.catalog.Image targetCatalogImage = com.sequenceiq.cloudbreak.cloud.model.catalog.Image.builder()
                .withUuid(TARGET_IMAGE_ID)
                .withVersion("image-version-only")
                .withOsType(OsType.RHEL8.getOsType())
                .withPackageVersions(Map.of(STACK.getKey(), "stack-runtime-version", CDH_BUILD_NUMBER.getKey(), "12345"))
                .build();
        Stack stack = setupStack(false);
        StatedImage targetStatedImage = StatedImage.statedImage(targetCatalogImage, IMAGE_CATALOG_URL, IMAGE_CATALOG_NAME);
        ClouderaManagerProduct cdhProduct = new ClouderaManagerProduct().withName("CDH").withVersion("7.2.18");

        when(componentConfigProviderService.getImage(STACK_ID)).thenReturn(currentImage);
        when(imageCatalogService.getImage(WORKSPACE_ID, IMAGE_CATALOG_URL, IMAGE_CATALOG_NAME, TARGET_IMAGE_ID)).thenReturn(targetStatedImage);
        when(clouderaManagerProductTransformer.transform(targetCatalogImage, true, true)).thenReturn(new HashSet<>(Set.of(cdhProduct)));
        when(imageToClouderaManagerRepoConverter.convert(targetCatalogImage)).thenReturn(new com.sequenceiq.cloudbreak.cloud.model.ClouderaManagerRepo());
        when(stackImageService.getImageModelFromStatedImage(stack, currentImage, targetStatedImage))
                .thenReturn(Image.builder().withImageName("targetImageName").build());

        ClusterUpgradeProperties properties = underTest.create(STACK_ID, TARGET_IMAGE_ID, false, true, false);

        assertEquals("stack-runtime-version", properties.runtimeVersion());
        assertEquals("image-version-only", properties.getTargetImageVersion());
    }

    @Test
    void testCreateFallsBackToImageVersionWhenStackPackageMissing() throws CloudbreakImageNotFoundException, CloudbreakImageCatalogException {
        Image currentImage = currentImage();
        com.sequenceiq.cloudbreak.cloud.model.catalog.Image targetCatalogImage = com.sequenceiq.cloudbreak.cloud.model.catalog.Image.builder()
                .withUuid(TARGET_IMAGE_ID)
                .withVersion("7.2.19")
                .withOsType(OsType.RHEL8.getOsType())
                .withPackageVersions(Map.of(CDH_BUILD_NUMBER.getKey(), "12345"))
                .build();
        Stack stack = setupStack(false);
        StatedImage targetStatedImage = StatedImage.statedImage(targetCatalogImage, IMAGE_CATALOG_URL, IMAGE_CATALOG_NAME);
        ClouderaManagerProduct cdhProduct = new ClouderaManagerProduct().withName("CDH").withVersion("7.2.19");

        when(componentConfigProviderService.getImage(STACK_ID)).thenReturn(currentImage);
        when(imageCatalogService.getImage(WORKSPACE_ID, IMAGE_CATALOG_URL, IMAGE_CATALOG_NAME, TARGET_IMAGE_ID)).thenReturn(targetStatedImage);
        when(clouderaManagerProductTransformer.transform(targetCatalogImage, true, true)).thenReturn(new HashSet<>(Set.of(cdhProduct)));
        when(imageToClouderaManagerRepoConverter.convert(targetCatalogImage)).thenReturn(new com.sequenceiq.cloudbreak.cloud.model.ClouderaManagerRepo());
        when(stackImageService.getImageModelFromStatedImage(stack, currentImage, targetStatedImage))
                .thenReturn(Image.builder().withImageName("targetImageName").build());

        ClusterUpgradeProperties properties = underTest.create(STACK_ID, TARGET_IMAGE_ID, false, true, false);

        assertEquals("7.2.19", properties.runtimeVersion());
    }

    @Test
    void testCreateWithNullPackageVersionMaps() throws CloudbreakImageNotFoundException, CloudbreakImageCatalogException {
        Image currentImage = Image.builder()
                .withImageId("currentImageId")
                .withImageCatalogName(IMAGE_CATALOG_NAME)
                .withImageCatalogUrl(IMAGE_CATALOG_URL)
                .withOsType(OsType.RHEL8.getOsType())
                .build();
        com.sequenceiq.cloudbreak.cloud.model.catalog.Image targetCatalogImage = com.sequenceiq.cloudbreak.cloud.model.catalog.Image.builder()
                .withUuid(TARGET_IMAGE_ID)
                .withVersion("7.2.18")
                .withOsType(OsType.RHEL8.getOsType())
                .build();
        Stack stack = setupStack(false);
        StatedImage targetStatedImage = StatedImage.statedImage(targetCatalogImage, IMAGE_CATALOG_URL, IMAGE_CATALOG_NAME);
        ClouderaManagerProduct cdhProduct = new ClouderaManagerProduct().withName("CDH").withVersion("7.2.18");

        when(componentConfigProviderService.getImage(STACK_ID)).thenReturn(currentImage);
        when(imageCatalogService.getImage(WORKSPACE_ID, IMAGE_CATALOG_URL, IMAGE_CATALOG_NAME, TARGET_IMAGE_ID)).thenReturn(targetStatedImage);
        when(clouderaManagerProductTransformer.transform(targetCatalogImage, true, true)).thenReturn(new HashSet<>(Set.of(cdhProduct)));
        when(imageToClouderaManagerRepoConverter.convert(targetCatalogImage)).thenReturn(new com.sequenceiq.cloudbreak.cloud.model.ClouderaManagerRepo());
        when(stackImageService.getImageModelFromStatedImage(stack, currentImage, targetStatedImage))
                .thenReturn(Image.builder().withImageName("targetImageName").build());

        ClusterUpgradeProperties properties = underTest.create(STACK_ID, TARGET_IMAGE_ID, false, true, false);

        assertNotNull(properties.getCurrentPackageVersions());
        assertNotNull(properties.getTargetPackageVersions());
        assertEquals(IMAGE_CATALOG_URL, properties.getCurrentImage().catalogUrl());
    }

    private Stack setupStack(boolean datalake) {
        Stack stack = mock(Stack.class);
        Workspace workspace = mock(Workspace.class);
        when(stackService.get(STACK_ID)).thenReturn(stack);
        when(stack.getWorkspace()).thenReturn(workspace);
        when(workspace.getId()).thenReturn(WORKSPACE_ID);
        when(stack.isDatalake()).thenReturn(datalake);
        return stack;
    }

    private Image currentImage() {
        return Image.builder()
                .withImageId("currentImageId")
                .withImageCatalogName(IMAGE_CATALOG_NAME)
                .withImageCatalogUrl(IMAGE_CATALOG_URL)
                .withImageName("currentImageName")
                .withOs("redhat8")
                .withOsType(OsType.RHEL8.getOsType())
                .withArchitecture("x86_64")
                .withDate("2024-01-01")
                .withCreated(1L)
                .withTags(Map.of("tag", "value"))
                .withPackageVersions(Map.of(STACK.getKey(), "7.2.17"))
                .build();
    }

    private com.sequenceiq.cloudbreak.cloud.model.catalog.Image targetCatalogImage() {
        return com.sequenceiq.cloudbreak.cloud.model.catalog.Image.builder()
                .withUuid(TARGET_IMAGE_ID)
                .withVersion("7.2.18")
                .withOsType(OsType.RHEL8.getOsType())
                .withPackageVersions(Map.of(STACK.getKey(), "7.2.18", CDH_BUILD_NUMBER.getKey(), "12345"))
                .build();
    }
}
