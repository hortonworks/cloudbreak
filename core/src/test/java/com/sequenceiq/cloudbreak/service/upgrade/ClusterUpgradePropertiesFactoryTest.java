package com.sequenceiq.cloudbreak.service.upgrade;

import static com.sequenceiq.cloudbreak.cloud.model.catalog.ImagePackageVersion.CDH_BUILD_NUMBER;
import static com.sequenceiq.cloudbreak.cloud.model.catalog.ImagePackageVersion.STACK;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.cm.ClouderaManagerV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.cm.product.ClouderaManagerProductV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.cm.repository.ClouderaManagerRepositoryV4Request;
import com.sequenceiq.cloudbreak.cloud.model.ClouderaManagerProduct;
import com.sequenceiq.cloudbreak.cloud.model.ClouderaManagerRepo;
import com.sequenceiq.cloudbreak.cloud.model.Image;
import com.sequenceiq.cloudbreak.cloud.model.catalog.ImageStackDetails;
import com.sequenceiq.cloudbreak.cloud.model.catalog.StackRepoDetails;
import com.sequenceiq.cloudbreak.cluster.service.ClusterComponentConfigProvider;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.common.exception.NotFoundException;
import com.sequenceiq.cloudbreak.common.json.JsonUtil;
import com.sequenceiq.cloudbreak.converter.ImageToClouderaManagerRepoConverter;
import com.sequenceiq.cloudbreak.converter.UpgradeComponentsConverter;
import com.sequenceiq.cloudbreak.core.CloudbreakImageCatalogException;
import com.sequenceiq.cloudbreak.core.CloudbreakImageNotFoundException;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.service.ComponentConfigProviderService;
import com.sequenceiq.cloudbreak.service.image.ImageCatalogService;
import com.sequenceiq.cloudbreak.service.image.ImageChangeDto;
import com.sequenceiq.cloudbreak.service.image.StatedImage;
import com.sequenceiq.cloudbreak.service.parcel.ClouderaManagerProductTransformer;
import com.sequenceiq.cloudbreak.service.stack.StackImageService;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.cloudbreak.service.upgrade.image.OsChangeService;
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

    @Mock
    private BaseImagePackageVersionsEnricher baseImagePackageVersionsEnricher;

    @Mock
    private ClusterComponentConfigProvider clusterComponentConfigProvider;

    @Spy
    private OsChangeService osChangeService;

    @InjectMocks
    private ClusterUpgradePropertiesFactory underTest;

    @BeforeEach
    void setupPackageVersionMapping() {
        lenient().when(baseImagePackageVersionsEnricher.getPackageVersionsFromComponents(
                any(), anySet())).thenCallRealMethod();
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void buildsBaseImagePropertiesFromRequestedComponents(boolean datalake) throws Exception {
        Stack stack = new Stack();
        stack.setType(datalake ? com.sequenceiq.cloudbreak.api.endpoint.v4.common.StackType.DATALAKE
                : com.sequenceiq.cloudbreak.api.endpoint.v4.common.StackType.WORKLOAD);
        Workspace workspace = new Workspace();
        workspace.setId(WORKSPACE_ID);
        stack.setWorkspace(workspace);
        Image current = currentImage();
        com.sequenceiq.cloudbreak.cloud.model.catalog.Image baseImage = com.sequenceiq.cloudbreak.cloud.model.catalog.Image.builder().withUuid(TARGET_IMAGE_ID)
                .withOs("redhat8").withOsType("redhat8").withArchitecture("x86_64")
                .withPackageVersions(Map.of("salt", "3006.10")).build();
        StatedImage statedImage = StatedImage.statedImage(baseImage, IMAGE_CATALOG_URL, IMAGE_CATALOG_NAME);
        when(componentConfigProviderService.getImage(STACK_ID)).thenReturn(current);
        when(stackService.get(STACK_ID)).thenReturn(stack);
        when(imageCatalogService.getImage(WORKSPACE_ID, IMAGE_CATALOG_URL, IMAGE_CATALOG_NAME, TARGET_IMAGE_ID)).thenReturn(statedImage);
        when(stackImageService.getImageModelFromStatedImage(stack, current, statedImage)).thenReturn(Image.builder().withImageName("base-ami").build());
        ClouderaManagerV4Request components = requestedComponents();
        ImageChangeDto dto = new ImageChangeDto(STACK_ID, TARGET_IMAGE_ID, IMAGE_CATALOG_NAME, IMAGE_CATALOG_URL,
                UpgradeComponentsConverter.fromRequest(components));
        ImageChangeDto restored = JsonUtil.readValue(JsonUtil.writeValueAsString(dto), ImageChangeDto.class);

        ClusterUpgradeProperties properties = underTest.create(restored, false, true, false);

        assertTrue(properties.targetImage().isBaseImage());
        assertEquals("7.3.2", properties.runtimeVersion());
        assertEquals("83281140", properties.cdhBuildNumber());
        assertEquals("7.13.2.20000-83309838", properties.targetImage().clouderaManagerRepo().getFullVersion());
        assertEquals("https://build/cm/", properties.targetImage().clouderaManagerRepo().getBaseUrl());
        assertEquals("3006.10", properties.toCloudImage().getPackageVersions().get("salt"));
        assertEquals("7.3.2", properties.toCloudImage().getPackageVersions().get("stack"));
        assertEquals("83309838", properties.toCloudImage().getPackageVersions().get("cm-build-number"));
        assertEquals(2, properties.getAllTargetProducts().size());
        ClouderaManagerProduct additional = properties.getPreWarmParcels().iterator().next();
        assertEquals("https://build/flink/FLINK-1.2.3-el8.parcel", additional.getParcelFileUrl());
        assertEquals(List.of("https://build/flink.jar"), additional.getCsd());
        assertEquals("https://build/cdh/CDH-7.3.2-1.cdh7.3.2.p20000.83281140-el8.parcel", properties.cdhParcel().getParcelFileUrl());
        verifyNoInteractions(clouderaManagerProductTransformer, imageToClouderaManagerRepoConverter);
    }

    @Test
    void rejectsIncompleteComponentsBeforeImageLookup() {
        ImageChangeDto dto = new ImageChangeDto(STACK_ID, TARGET_IMAGE_ID, null, null, new UpgradeComponents(null, null));

        assertThatThrownBy(() -> underTest.create(dto, false, false, false))
                .isInstanceOf(BadRequestException.class).hasMessageContaining("repository.version");
        verifyNoInteractions(imageCatalogService, componentConfigProviderService, stackService);
    }

    @ParameterizedTest
    @CsvSource({"7.13.1.0-12345,7.13.1.0,12345,x86_64,el8", "7.13.1.0,7.13.1.0,,arm64,el8.aarch64le"})
    void parsesCmVersionWithOptionalBuildNumber(String fullVersion, String version, String buildNumber,
            String architecture, String parcelPostfix) throws Exception {
        Stack stack = new Stack();
        Workspace workspace = new Workspace();
        workspace.setId(WORKSPACE_ID);
        stack.setWorkspace(workspace);
        Image current = currentImage();
        com.sequenceiq.cloudbreak.cloud.model.catalog.Image baseImage = com.sequenceiq.cloudbreak.cloud.model.catalog.Image.builder()
                .withUuid(TARGET_IMAGE_ID).withOsType("redhat8").withArchitecture(architecture).withPackageVersions(Map.of()).build();
        StatedImage statedImage = StatedImage.statedImage(baseImage, IMAGE_CATALOG_URL, IMAGE_CATALOG_NAME);
        when(stackService.get(STACK_ID)).thenReturn(stack);
        when(componentConfigProviderService.getImage(STACK_ID)).thenReturn(current);
        when(imageCatalogService.getImage(WORKSPACE_ID, IMAGE_CATALOG_URL, IMAGE_CATALOG_NAME, TARGET_IMAGE_ID)).thenReturn(statedImage);
        when(stackImageService.getImageModelFromStatedImage(stack, current, statedImage)).thenReturn(Image.builder().build());
        ClouderaManagerV4Request components = requestedComponents();
        components.getRepository().setVersion(fullVersion);

        ClusterUpgradeProperties result = underTest.create(new ImageChangeDto(STACK_ID, TARGET_IMAGE_ID, null, null,
                UpgradeComponentsConverter.fromRequest(components)), false, false, false);

        assertEquals(version, result.targetImage().clouderaManagerRepo().getVersion());
        assertEquals(buildNumber, result.targetImage().clouderaManagerRepo().getBuildNumber());
        assertEquals("https://build/cdh/CDH-7.3.2-1.cdh7.3.2.p20000.83281140-" + parcelPostfix + ".parcel", result.cdhParcel().getParcelFileUrl());
    }

    @Test
    void resolvesCurrentRuntimeForAClusterProvisionedWithABaseImage() throws Exception {
        Stack stack = new Stack();
        Image current = Image.builder().withOsType("redhat8").withPackageVersions(Map.of("salt", "3006.10")).build();
        com.sequenceiq.cloudbreak.cloud.model.catalog.Image baseImage = com.sequenceiq.cloudbreak.cloud.model.catalog.Image.builder()
                .withUuid(TARGET_IMAGE_ID).withOsType("redhat8").withPackageVersions(Map.of()).build();
        StatedImage statedImage = StatedImage.statedImage(baseImage, IMAGE_CATALOG_URL, IMAGE_CATALOG_NAME);
        when(stackImageService.getImageModelFromStatedImage(stack, current, statedImage)).thenReturn(Image.builder().build());
        when(baseImagePackageVersionsEnricher.enrich(current, stack)).thenReturn(currentImage());

        Workspace workspace = new Workspace();
        workspace.setId(WORKSPACE_ID);
        stack.setWorkspace(workspace);
        when(stackService.get(STACK_ID)).thenReturn(stack);
        when(componentConfigProviderService.getImage(STACK_ID)).thenReturn(current);
        when(imageCatalogService.getImage(WORKSPACE_ID, IMAGE_CATALOG_URL, IMAGE_CATALOG_NAME, TARGET_IMAGE_ID)).thenReturn(statedImage);
        ClusterUpgradeProperties properties = underTest.create(
                new ImageChangeDto(STACK_ID, TARGET_IMAGE_ID, IMAGE_CATALOG_NAME, IMAGE_CATALOG_URL,
                        UpgradeComponentsConverter.fromRequest(requestedComponents())), false, false, false);

        assertEquals("7.2.17", properties.currentImage().runtimeVersion());
        assertEquals("7.3.2", properties.targetImage().runtimeVersion());
    }

    @Test
    void resolvesCurrentRuntimeForBaseImageSourceWithPrewarmTarget() throws Exception {
        Stack stack = new Stack();
        Workspace workspace = new Workspace();
        workspace.setId(WORKSPACE_ID);
        stack.setWorkspace(workspace);
        Image current = Image.builder().withImageId("currentImageId").withOsType("redhat8").withPackageVersions(Map.of("salt", "3006.10")).build();
        com.sequenceiq.cloudbreak.cloud.model.catalog.Image target = com.sequenceiq.cloudbreak.cloud.model.catalog.Image.builder()
                .withUuid(TARGET_IMAGE_ID).withVersion("7.2.18").withOsType("redhat8").withPackageVersions(Map.of("stack", "7.2.18"))
                .withStackDetails(new ImageStackDetails("7.2.18", new StackRepoDetails(Map.of(StackRepoDetails.REPO_ID_TAG, "CDH"), Map.of()), "12345"))
                .build();
        StatedImage statedImage = StatedImage.statedImage(target, IMAGE_CATALOG_URL, IMAGE_CATALOG_NAME);
        when(componentConfigProviderService.getImage(STACK_ID)).thenReturn(current);
        when(stackService.get(STACK_ID)).thenReturn(stack);
        when(imageCatalogService.getImage(WORKSPACE_ID, IMAGE_CATALOG_URL, IMAGE_CATALOG_NAME, TARGET_IMAGE_ID)).thenReturn(statedImage);
        when(stackImageService.getImageModelFromStatedImage(stack, current, statedImage)).thenReturn(Image.builder().build());
        when(clouderaManagerProductTransformer.transform(target, true, true)).thenReturn(Set.of(new ClouderaManagerProduct().withName("CDH")
                .withVersion("7.2.18-1.cdh7.2.18.p0.12345")));
        when(imageToClouderaManagerRepoConverter.convert(target)).thenReturn(new ClouderaManagerRepo().withVersion("7.11.3"));
        when(baseImagePackageVersionsEnricher.enrich(current, stack)).thenReturn(currentImage());

        ClusterUpgradeProperties properties = underTest.create(new ImageChangeDto(STACK_ID, TARGET_IMAGE_ID, IMAGE_CATALOG_NAME, IMAGE_CATALOG_URL),
                false, false, true);

        assertFalse(properties.targetImage().isBaseImage());
        assertEquals("7.2.17", properties.currentImage().runtimeVersion());
    }

    @Test
    void capturesCurrentClusterComponentsBeforeUpgrade() throws Exception {
        Stack stack = new Stack();
        Workspace workspace = new Workspace();
        workspace.setId(WORKSPACE_ID);
        stack.setWorkspace(workspace);
        Cluster cluster = new Cluster();
        cluster.setId(3L);
        stack.setCluster(cluster);
        Image current = currentImage();
        com.sequenceiq.cloudbreak.cloud.model.catalog.Image target = targetCatalogImage();
        StatedImage statedImage = StatedImage.statedImage(target, IMAGE_CATALOG_URL, IMAGE_CATALOG_NAME);
        when(componentConfigProviderService.getImage(STACK_ID)).thenReturn(current);
        when(stackService.get(STACK_ID)).thenReturn(stack);
        when(imageCatalogService.getImage(WORKSPACE_ID, IMAGE_CATALOG_URL, IMAGE_CATALOG_NAME, TARGET_IMAGE_ID)).thenReturn(statedImage);
        when(stackImageService.getImageModelFromStatedImage(stack, current, statedImage)).thenReturn(Image.builder().build());
        when(clouderaManagerProductTransformer.transform(target, true, true)).thenReturn(Set.of(new ClouderaManagerProduct().withName("CDH")
                .withVersion("7.2.18")));
        when(imageToClouderaManagerRepoConverter.convert(target)).thenReturn(new ClouderaManagerRepo().withVersion("7.11.3"));
        ClouderaManagerProduct currentFlink = new ClouderaManagerProduct().withName("FLINK").withVersion("1.20.0-custom.1");
        ClouderaManagerRepo currentCmRepo = new ClouderaManagerRepo().withVersion("7.13.1").withBuildNumber("1");
        when(clusterComponentConfigProvider.getClouderaManagerProductDetails(3L)).thenReturn(List.of(currentFlink));
        when(clusterComponentConfigProvider.getClouderaManagerRepoDetails(3L)).thenReturn(currentCmRepo);

        ClusterUpgradeProperties properties = underTest.create(new ImageChangeDto(STACK_ID, TARGET_IMAGE_ID, IMAGE_CATALOG_NAME, IMAGE_CATALOG_URL),
                false, false, false);

        assertEquals(Set.of(currentFlink), properties.currentImage().products());
        assertEquals(currentCmRepo, properties.currentImage().clouderaManagerRepo());
        ClusterUpgradeProperties restored = JsonUtil.readValue(JsonUtil.writeValueAsString(properties), ClusterUpgradeProperties.class);
        assertEquals("1.20.0-custom.1", restored.currentImage().products().iterator().next().getVersion());
        assertEquals("7.13.1-1", restored.currentImage().clouderaManagerRepo().getFullVersion());
    }

    @Test
    void normalizesRequestedCmBaseUrlWithoutTrailingSlash() throws Exception {
        Stack stack = new Stack();
        Workspace workspace = new Workspace();
        workspace.setId(WORKSPACE_ID);
        stack.setWorkspace(workspace);
        Image current = currentImage();
        com.sequenceiq.cloudbreak.cloud.model.catalog.Image baseImage = com.sequenceiq.cloudbreak.cloud.model.catalog.Image.builder().withUuid(TARGET_IMAGE_ID)
                .withOsType("redhat8").withPackageVersions(Map.of()).build();
        StatedImage statedImage = StatedImage.statedImage(baseImage, IMAGE_CATALOG_URL, IMAGE_CATALOG_NAME);
        when(componentConfigProviderService.getImage(STACK_ID)).thenReturn(current);
        when(stackService.get(STACK_ID)).thenReturn(stack);
        when(imageCatalogService.getImage(WORKSPACE_ID, IMAGE_CATALOG_URL, IMAGE_CATALOG_NAME, TARGET_IMAGE_ID)).thenReturn(statedImage);
        when(stackImageService.getImageModelFromStatedImage(stack, current, statedImage)).thenReturn(Image.builder().build());
        ClouderaManagerV4Request components = requestedComponents();
        components.getRepository().setBaseUrl("https://build/cm/redhat8/yum");

        ClusterUpgradeProperties properties = underTest.create(
                new ImageChangeDto(STACK_ID, TARGET_IMAGE_ID, IMAGE_CATALOG_NAME, IMAGE_CATALOG_URL,
                UpgradeComponentsConverter.fromRequest(components)), false, false, false);

        assertEquals("https://build/cm/redhat8/yum/", properties.targetImage().clouderaManagerRepo().getBaseUrl());
    }

    @Test
    void alignsRequestedRepositoriesWithBaseImageOsWithoutChangingRequest() throws Exception {
        Stack stack = new Stack();
        Workspace workspace = new Workspace();
        workspace.setId(WORKSPACE_ID);
        stack.setWorkspace(workspace);
        Image current = currentImage();
        com.sequenceiq.cloudbreak.cloud.model.catalog.Image baseImage = com.sequenceiq.cloudbreak.cloud.model.catalog.Image.builder()
                .withUuid(TARGET_IMAGE_ID).withOs("redhat8").withOsType("redhat8").withArchitecture("x86_64").withPackageVersions(Map.of()).build();
        StatedImage statedImage = StatedImage.statedImage(baseImage, IMAGE_CATALOG_URL, IMAGE_CATALOG_NAME);
        when(componentConfigProviderService.getImage(STACK_ID)).thenReturn(current);
        when(stackService.get(STACK_ID)).thenReturn(stack);
        when(imageCatalogService.getImage(WORKSPACE_ID, IMAGE_CATALOG_URL, IMAGE_CATALOG_NAME, TARGET_IMAGE_ID)).thenReturn(statedImage);
        when(stackImageService.getImageModelFromStatedImage(stack, current, statedImage)).thenReturn(Image.builder().build());
        ClouderaManagerV4Request components = requestedComponents();
        String cmRepo = "https://build/cm/redhat9/yum/";
        String cfmRepo = "https://build/cfm/redhat9/yum/tars/parcel/";
        components.getRepository().setBaseUrl(cmRepo);
        components.getRepository().setGpgKeyUrl(cmRepo + "RPM-GPG-KEY-cloudera");
        ClouderaManagerProductV4Request cfm = components.getProducts().get(1);
        cfm.setName("CFM");
        cfm.setVersion("4.12.0.20000-231");
        cfm.setParcel(cfmRepo);
        cfm.setCsd(List.of(cfmRepo + "NIFI.jar", cfmRepo + "NIFIREGISTRY.jar", "https://build/shared/NIFI.jar"));

        ClusterUpgradeProperties properties = underTest.create(
                new ImageChangeDto(STACK_ID, TARGET_IMAGE_ID, IMAGE_CATALOG_NAME, IMAGE_CATALOG_URL,
                UpgradeComponentsConverter.fromRequest(components)), false, false, false);

        assertEquals("https://build/cm/redhat8/yum/", properties.targetImage().clouderaManagerRepo().getBaseUrl());
        assertEquals("https://build/cm/redhat8/yum/RPM-GPG-KEY-cloudera", properties.targetImage().clouderaManagerRepo().getGpgKeyUrl());
        ClouderaManagerProduct targetCfm = properties.getPreWarmParcels().iterator().next();
        assertEquals("https://build/cfm/redhat8/yum/tars/parcel/", targetCfm.getParcel());
        assertEquals("https://build/cfm/redhat8/yum/tars/parcel/CFM-4.12.0.20000-231-el8.parcel", targetCfm.getParcelFileUrl());
        assertEquals(List.of("https://build/cfm/redhat8/yum/tars/parcel/NIFI.jar", "https://build/cfm/redhat8/yum/tars/parcel/NIFIREGISTRY.jar",
                "https://build/shared/NIFI.jar"), targetCfm.getCsd());
        assertEquals("https://build/cdh/", properties.cdhParcel().getParcel());
        assertEquals(cmRepo, components.getRepository().getBaseUrl());
        assertEquals(cmRepo + "RPM-GPG-KEY-cloudera", components.getRepository().getGpgKeyUrl());
        assertEquals(cfmRepo, cfm.getParcel());
        assertEquals(cfmRepo + "NIFI.jar", cfm.getCsd().getFirst());
    }

    private ClouderaManagerV4Request requestedComponents() {
        ClouderaManagerRepositoryV4Request repository = new ClouderaManagerRepositoryV4Request();
        repository.setVersion("7.13.2.20000-83309838");
        repository.setBaseUrl("https://build/cm/");
        return new ClouderaManagerV4Request().withRepository(repository).withProducts(List.of(
                new ClouderaManagerProductV4Request().withName("CDH").withVersion("7.3.2-1.cdh7.3.2.p20000.83281140")
                        .withParcel("https://build/cdh"),
                new ClouderaManagerProductV4Request().withName("FLINK").withVersion("1.2.3").withParcel("https://build/flink/")
                        .withCsd(List.of("https://build/flink.jar"))));
    }

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

        ClusterUpgradeProperties properties = underTest.create(new ImageChangeDto(STACK_ID, TARGET_IMAGE_ID), false, true, false);

        assertEquals(TARGET_IMAGE_ID, properties.targetImageId());
        assertEquals(IMAGE_CATALOG_NAME, properties.imageCatalogName());
        assertEquals(IMAGE_CATALOG_URL, properties.imageCatalogUrl());
        assertEquals("7.2.18", properties.runtimeVersion());
        assertEquals(OsType.RHEL8, properties.currentOsType());
        assertFalse(properties.lockComponents());
        assertTrue(properties.rollingUpgradeEnabled());
        assertFalse(properties.replaceVms());
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

        ClusterUpgradeProperties properties = underTest.create(new ImageChangeDto(STACK_ID, TARGET_IMAGE_ID), false, true, false);

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

        ClusterUpgradeProperties properties = underTest.create(new ImageChangeDto(STACK_ID, TARGET_IMAGE_ID), false, true, false);

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

        ClusterUpgradeProperties properties = underTest.create(new ImageChangeDto(STACK_ID, TARGET_IMAGE_ID), false, true, false);

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

        ClusterUpgradeProperties properties = underTest.create(new ImageChangeDto(STACK_ID, TARGET_IMAGE_ID), false, true, false);

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

        ClusterUpgradeProperties properties = underTest.create(new ImageChangeDto(STACK_ID, TARGET_IMAGE_ID), false, true, false);

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
        when(baseImagePackageVersionsEnricher.enrich(currentImage, stack)).thenReturn(currentImage);

        ClusterUpgradeProperties properties = underTest.create(new ImageChangeDto(STACK_ID, TARGET_IMAGE_ID), false, true, false);

        assertNotNull(properties.getCurrentPackageVersions());
        assertNotNull(properties.getTargetPackageVersions());
        assertEquals(IMAGE_CATALOG_URL, properties.getCurrentImage().catalogUrl());
    }

    @Test
    void testCreateUsesExplicitTargetCatalog() throws Exception {
        Image current = currentImage();
        Stack stack = setupStack(false);
        com.sequenceiq.cloudbreak.cloud.model.catalog.Image target = targetCatalogImage();
        StatedImage statedImage = StatedImage.statedImage(target, "custom-url", "custom-catalog");
        when(componentConfigProviderService.getImage(STACK_ID)).thenReturn(current);
        when(imageCatalogService.getImage(WORKSPACE_ID, "custom-url", "custom-catalog", TARGET_IMAGE_ID)).thenReturn(statedImage);
        when(stackImageService.getImageModelFromStatedImage(stack, current, statedImage)).thenReturn(Image.builder().build());
        when(clouderaManagerProductTransformer.transform(target, true, true)).thenReturn(Set.of());

        ClusterUpgradeProperties properties = underTest.create(
                new ImageChangeDto(STACK_ID, TARGET_IMAGE_ID, "custom-catalog", "custom-url"), true, false, true);

        assertEquals("custom-catalog", properties.targetImage().catalogName());
        assertEquals("custom-url", properties.targetImage().catalogUrl());
        assertEquals(new ClusterUpgradeProperties.UpgradeRequestOptions(true, true, false), properties.options());
        verify(imageCatalogService).getImage(WORKSPACE_ID, "custom-url", "custom-catalog", TARGET_IMAGE_ID);
    }

    @Test
    void testCreateWrapsMissingImage() throws Exception {
        CloudbreakImageNotFoundException failure = new CloudbreakImageNotFoundException("missing image");
        when(componentConfigProviderService.getImage(STACK_ID)).thenThrow(failure);

        assertThatThrownBy(() -> underTest.create(new ImageChangeDto(STACK_ID, TARGET_IMAGE_ID), false, false, false))
                .isInstanceOf(NotFoundException.class).hasMessage("Image not found for cluster upgrade").hasCause(failure);
    }

    @Test
    void testCreateWrapsCatalogFailure() throws Exception {
        Image current = currentImage();
        Stack stack = mock(Stack.class);
        Workspace workspace = new Workspace();
        workspace.setId(WORKSPACE_ID);
        when(stack.getWorkspace()).thenReturn(workspace);
        when(stackService.get(STACK_ID)).thenReturn(stack);
        when(componentConfigProviderService.getImage(STACK_ID)).thenReturn(current);
        CloudbreakImageCatalogException failure = new CloudbreakImageCatalogException("unavailable catalog");
        when(imageCatalogService.getImage(WORKSPACE_ID, IMAGE_CATALOG_URL, IMAGE_CATALOG_NAME, TARGET_IMAGE_ID)).thenThrow(failure);

        assertThatThrownBy(() -> underTest.create(new ImageChangeDto(STACK_ID, TARGET_IMAGE_ID), false, false, false))
                .isInstanceOf(CloudbreakServiceException.class).hasMessage("Image catalog is not reachable").hasCause(failure);
    }

    @Test
    void testCreatePreservesNotFoundForMissingTargetImage() throws Exception {
        Image current = currentImage();
        Stack stack = mock(Stack.class);
        Workspace workspace = new Workspace();
        workspace.setId(WORKSPACE_ID);
        when(stack.getWorkspace()).thenReturn(workspace);
        when(stackService.get(STACK_ID)).thenReturn(stack);
        when(componentConfigProviderService.getImage(STACK_ID)).thenReturn(current);
        CloudbreakImageNotFoundException failure = new CloudbreakImageNotFoundException("missing target");
        when(imageCatalogService.getImage(WORKSPACE_ID, IMAGE_CATALOG_URL, IMAGE_CATALOG_NAME, TARGET_IMAGE_ID)).thenThrow(failure);

        assertThatThrownBy(() -> underTest.create(new ImageChangeDto(STACK_ID, TARGET_IMAGE_ID), false, false, false))
                .isInstanceOf(NotFoundException.class).hasMessage("Image not found for cluster upgrade").hasCause(failure);
    }

    @Test
    void testLegacySnapshotRebuildPreservesOriginalImageWithoutCatalogLookup() {
        Stack stack = mock(Stack.class);
        when(stackService.get(STACK_ID)).thenReturn(stack);
        when(stack.isDatalake()).thenReturn(false);
        Image original = currentImage();
        com.sequenceiq.cloudbreak.cloud.model.catalog.Image target = targetCatalogImage();
        StatedImage stated = StatedImage.statedImage(target, "persisted-url", "persisted-catalog");
        when(stackImageService.getImageModelFromStatedImage(stack, original, stated)).thenReturn(Image.builder().withImageName("target-vm").build());
        when(clouderaManagerProductTransformer.transform(target, true, true)).thenReturn(Set.of());

        ClusterUpgradeProperties properties = underTest.create(STACK_ID, original, stated,
                new ClusterUpgradeProperties.UpgradeRequestOptions(false, false, true));

        assertEquals(original.getImageId(), properties.currentImageId());
        assertEquals(original.getPackageVersions(), properties.currentImage().packageVersions());
        assertEquals(OsType.RHEL8, properties.currentImage().osType());
        assertEquals("persisted-catalog", properties.targetImage().catalogName());
        assertEquals("persisted-url", properties.targetImage().catalogUrl());
        verifyNoInteractions(imageCatalogService, componentConfigProviderService);
    }

    @Test
    void testLegacyTriggerRebuildUsesOriginalImageAfterDatabaseImageWasUpdated() throws Exception {
        Stack stack = setupStack(false);
        Image original = currentImage();
        Image databaseImage = Image.builder().withImageId(TARGET_IMAGE_ID).withImageCatalogName("updated-catalog")
                .withImageCatalogUrl("updated-url").withOsType("redhat9").build();
        com.sequenceiq.cloudbreak.cloud.model.catalog.Image target = targetCatalogImage();
        StatedImage stated = StatedImage.statedImage(target, "updated-url", "updated-catalog");
        when(componentConfigProviderService.getImage(STACK_ID)).thenReturn(databaseImage);
        when(imageCatalogService.getImage(WORKSPACE_ID, "updated-url", "updated-catalog", TARGET_IMAGE_ID)).thenReturn(stated);
        when(stackImageService.getImageModelFromStatedImage(stack, original, stated)).thenReturn(Image.builder().withImageName("target-vm").build());
        when(clouderaManagerProductTransformer.transform(target, true, true)).thenReturn(Set.of());

        ClusterUpgradeProperties properties = underTest.create(new ImageChangeDto(STACK_ID, TARGET_IMAGE_ID), original,
                new ClusterUpgradeProperties.UpgradeRequestOptions(false, false, true));

        assertEquals(original.getImageId(), properties.currentImageId());
        assertEquals(OsType.RHEL8, properties.currentImage().osType());
        assertEquals("updated-catalog", properties.targetImage().catalogName());
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
