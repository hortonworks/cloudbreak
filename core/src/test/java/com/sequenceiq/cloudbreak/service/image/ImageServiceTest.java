package com.sequenceiq.cloudbreak.service.image;

import static com.sequenceiq.cloudbreak.cloud.model.Platform.platform;
import static org.junit.Assert.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.core.convert.ConversionService;

import com.sequenceiq.cloudbreak.TestUtil;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.image.ImageSettingsV4Request;
import com.sequenceiq.cloudbreak.cloud.CloudConnector;
import com.sequenceiq.cloudbreak.cloud.init.CloudPlatformConnectors;
import com.sequenceiq.cloudbreak.cloud.model.ClouderaManagerRepo;
import com.sequenceiq.cloudbreak.cloud.model.catalog.Image;
import com.sequenceiq.cloudbreak.cloud.model.catalog.StackDetails;
import com.sequenceiq.cloudbreak.cloud.model.catalog.StackRepoDetails;
import com.sequenceiq.cloudbreak.cluster.service.ClusterComponentConfigProvider;
import com.sequenceiq.cloudbreak.cmtemplate.utils.BlueprintUtils;
import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.cloudbreak.common.type.ComponentType;
import com.sequenceiq.cloudbreak.core.CloudbreakImageCatalogException;
import com.sequenceiq.cloudbreak.core.CloudbreakImageNotFoundException;
import com.sequenceiq.cloudbreak.domain.ImageCatalog;
import com.sequenceiq.cloudbreak.domain.stack.Component;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.domain.stack.cluster.ClusterComponent;
import com.sequenceiq.cloudbreak.exception.NotFoundException;
import com.sequenceiq.cloudbreak.service.ComponentConfigProviderService;
import com.sequenceiq.cloudbreak.service.StackMatrixService;
import com.sequenceiq.cloudbreak.workspace.model.Workspace;

@RunWith(MockitoJUnitRunner.class)
public class ImageServiceTest {

    private static final Long WORKSPACE_ID = 1L;

    private static final String PLATFORM = "AZURE";

    private static final String STACK_VERSION = "7.1.0";

    private static final String TARGET_STACK_VERSION = "7.1.1";

    private static final String CUSTOM_IMAGE_CATALOG_URL = "http://localhost/custom-imagecatalog-url";

    private static final long ORG_ID = 100L;

    private static final String OS = "anOS";

    private static final long USER_ID = 1000L;

    private static final String USER_ID_STRING = "aUserId";

    private static final String CDH = "CDH";

    private static final String CDH_VERSION = "7.0.0-1.cdh7.0.0.p0.1376867";

    private static final String PARCEL_TEMPLATE = "{\"name\":\"%s\",\"version\":\"%s\","
            + "\"parcel\":\"https://archive.cloudera.com/cdh7/7.0.0/parcels/\"}";

    private static final String CDH_ATTRIBUTES = String.format(PARCEL_TEMPLATE, CDH, CDH_VERSION);

    private static final String CM_ATTRIBUTES = "{\"predefined\":false,\"version\":\"7.0.0\","
            + "\"baseUrl\":\"https://archive.cloudera.com/cm7/7.0.0/redhat7/yum/\","
            + "\"gpgKeyUrl\":\"https://archive.cloudera.com/cm7/7.0.0/redhat7/yum/RPM-GPG-KEY-cloudera\"}";

    @Mock
    private ImageCatalogService imageCatalogService;

    @Mock
    private StackMatrixService stackMatrixService;

    @Mock
    private BlueprintUtils blueprintUtils;

    @Mock
    private CloudConnector cloudConnector;

    @Mock
    private CloudPlatformConnectors cloudPlatformConnectors;

    @Mock
    private ConversionService conversionService;

    @Mock
    private ComponentConfigProviderService componentConfigProviderService;

    @Mock
    private ClusterComponentConfigProvider clusterComponentConfigProvider;

    @InjectMocks
    private ImageService underTest;

    private ImageSettingsV4Request imageSettingsV4Request;

    @Before
    public void setUp() {
        imageSettingsV4Request = new ImageSettingsV4Request();
        imageSettingsV4Request.setCatalog("aCatalog");
        imageSettingsV4Request.setId("anImageId");
        imageSettingsV4Request.setOs(OS);
        when(blueprintUtils.getCDHStackVersion(any())).thenReturn(STACK_VERSION);
        when(imageCatalogService.get(WORKSPACE_ID, "aCatalog")).thenReturn(getImageCatalog());
    }

    @Test
    public void testUseBaseImageAndDisabledBaseImageShouldReturnError() {
        CloudbreakImageCatalogException exception = assertThrows(CloudbreakImageCatalogException.class, () ->
                underTest.determineImageFromCatalog(
                        WORKSPACE_ID,
                        imageSettingsV4Request,
                        PLATFORM,
                        TestUtil.blueprint(),
                        true,
                        false,
                        TestUtil.user(USER_ID, USER_ID_STRING),
                        image -> true));
        assertEquals("Inconsistent request, base images are disabled but custom repo information is submitted!", exception.getMessage());
    }

    @Test
    public void testDetermineImageFromCatalogWithNonExistingCatalogNameAndIdSpecified()
            throws CloudbreakImageNotFoundException, CloudbreakImageCatalogException {
        when(imageCatalogService.getImageByCatalogName(WORKSPACE_ID, "anImageId", "aCatalog"))
                .thenThrow(new CloudbreakImageCatalogException("Image catalog not found with name: aCatalog"));

        CloudbreakImageCatalogException exception = assertThrows(CloudbreakImageCatalogException.class, () ->
                underTest.determineImageFromCatalog(
                        WORKSPACE_ID,
                        imageSettingsV4Request,
                        PLATFORM,
                        TestUtil.blueprint(),
                        true,
                        true,
                        TestUtil.user(USER_ID, USER_ID_STRING),
                        image -> true));

        assertEquals("Image catalog not found with name: aCatalog", exception.getMessage());
    }

    @Test
    public void testDetermineImageFromCatalogWithNonExistingCatalogName() {
        when(imageCatalogService.get(WORKSPACE_ID, "aCatalog")).thenThrow(new NotFoundException("Image catalog not found with name: aCatalog"));
        ImageSettingsV4Request imageRequest = new ImageSettingsV4Request();
        imageRequest.setCatalog("aCatalog");
        imageRequest.setOs(OS);

        CloudbreakImageCatalogException exception = assertThrows(CloudbreakImageCatalogException.class, () ->
                underTest.determineImageFromCatalog(
                        WORKSPACE_ID,
                        imageRequest,
                        PLATFORM,
                        TestUtil.blueprint(),
                        true,
                        true,
                        TestUtil.user(USER_ID, USER_ID_STRING),
                        image -> true));

        assertEquals("Image catalog not found with name: aCatalog", exception.getMessage());
    }

    @Test
    public void testGivenBaseImageIdAndDisabledBaseImageShouldReturnError() throws CloudbreakImageNotFoundException, CloudbreakImageCatalogException {
        when(imageCatalogService.getImageByCatalogName(anyLong(), anyString(), anyString())).thenReturn(getImageFromCatalog(false, "uuid", STACK_VERSION));
        CloudbreakImageCatalogException exception = assertThrows(CloudbreakImageCatalogException.class, () ->
                underTest.determineImageFromCatalog(
                        WORKSPACE_ID,
                        imageSettingsV4Request,
                        PLATFORM,
                        TestUtil.blueprint(),
                        false,
                        false,
                        TestUtil.user(USER_ID, USER_ID_STRING),
                        image -> true));
        assertEquals("Inconsistent request, base images are disabled but image with id uuid is base image!", exception.getMessage());
    }

    @Test
    public void testGivenPrewarmedImageIdAndDisabledBaseImageShouldReturnOk() throws CloudbreakImageNotFoundException, CloudbreakImageCatalogException {
        when(imageCatalogService.getImageByCatalogName(anyLong(), anyString(), anyString())).thenReturn(getImageFromCatalog(true, "uuid", STACK_VERSION));
        StatedImage statedImage = underTest.determineImageFromCatalog(
                WORKSPACE_ID,
                imageSettingsV4Request,
                PLATFORM,
                TestUtil.blueprint(),
                false,
                false,
                TestUtil.user(USER_ID, USER_ID_STRING),
                image -> true);
        assertEquals("uuid", statedImage.getImage().getUuid());
        assertTrue(statedImage.getImage().isPrewarmed());
    }

    @Test
    public void testGivenBaseImageIdAndEnabledBaseImageShouldReturnOk() throws CloudbreakImageNotFoundException, CloudbreakImageCatalogException {
        when(imageCatalogService.getImageByCatalogName(anyLong(), anyString(), anyString())).thenReturn(getImageFromCatalog(false, "uuid", STACK_VERSION));
        StatedImage statedImage = underTest.determineImageFromCatalog(
                WORKSPACE_ID,
                imageSettingsV4Request,
                PLATFORM,
                TestUtil.blueprint(),
                false,
                true,
                TestUtil.user(USER_ID, USER_ID_STRING),
                image -> true);
        assertEquals("uuid", statedImage.getImage().getUuid());
        assertFalse(statedImage.getImage().isPrewarmed());
    }

    @Test
    public void testGivenPrewarmedImageIdAndEnabledBaseImageShouldReturnOk() throws CloudbreakImageNotFoundException, CloudbreakImageCatalogException {
        when(imageCatalogService.getImageByCatalogName(anyLong(), anyString(), anyString())).thenReturn(getImageFromCatalog(true, "uuid", STACK_VERSION));
        StatedImage statedImage = underTest.determineImageFromCatalog(
                WORKSPACE_ID,
                imageSettingsV4Request,
                PLATFORM,
                TestUtil.blueprint(),
                false,
                true,
                TestUtil.user(USER_ID, USER_ID_STRING),
                image -> true);
        assertEquals("uuid", statedImage.getImage().getUuid());
        assertTrue(statedImage.getImage().isPrewarmed());
    }

    @Test
    public void testUseBaseImageAndEnabledBaseImageShouldReturnCorrectImage() throws CloudbreakImageNotFoundException, CloudbreakImageCatalogException {
        imageSettingsV4Request.setId(null);
        when(imageCatalogService.getLatestBaseImageDefaultPreferred(any(), any())).thenReturn(getImageFromCatalog(false, "uuid", STACK_VERSION));
        StatedImage statedImage = underTest.determineImageFromCatalog(
                WORKSPACE_ID,
                imageSettingsV4Request,
                PLATFORM,
                TestUtil.blueprint(),
                true,
                true,
                TestUtil.user(USER_ID, USER_ID_STRING),
                image -> true);
        assertEquals("uuid", statedImage.getImage().getUuid());
        assertFalse(statedImage.getImage().isPrewarmed());
    }

    @Test
    public void testNoUseBaseImageAndEnabledBaseImageShouldReturnCorrectImage() throws CloudbreakImageCatalogException, CloudbreakImageNotFoundException {
        imageSettingsV4Request.setId(null);
        when(imageCatalogService.getImagePrewarmedDefaultPreferred(any(), any())).thenReturn(getImageFromCatalog(false, "uuid", STACK_VERSION));
        StatedImage statedImage = underTest.determineImageFromCatalog(
                WORKSPACE_ID,
                imageSettingsV4Request,
                PLATFORM,
                TestUtil.blueprint(),
                false,
                true,
                TestUtil.user(USER_ID, USER_ID_STRING),
                image -> true);
        assertEquals("uuid", statedImage.getImage().getUuid());
        assertFalse(statedImage.getImage().isPrewarmed());
    }

    @Test
    public void testDisabledBaseImageShouldReturnCorrectImage() throws CloudbreakImageCatalogException, CloudbreakImageNotFoundException {
        imageSettingsV4Request.setId(null);
        when(imageCatalogService.getLatestPrewarmedImageDefaultPreferred(any(), any())).thenReturn(getImageFromCatalog(true, "uuid", STACK_VERSION));
        StatedImage statedImage = underTest.determineImageFromCatalog(
                WORKSPACE_ID,
                imageSettingsV4Request,
                PLATFORM,
                TestUtil.blueprint(),
                false,
                false,
                TestUtil.user(USER_ID, USER_ID_STRING),
                image -> true);
        assertEquals("uuid", statedImage.getImage().getUuid());
        assertTrue(statedImage.getImage().isPrewarmed());
    }

    @Test
    public void testUpdateImageComponents() throws CloudbreakImageCatalogException, CloudbreakImageNotFoundException {

        Stack stack = TestUtil.stack();
        Cluster cluster = TestUtil.cluster();
        stack.setCluster(cluster);
        StatedImage targetImage = getImageFromCatalog(true, "targetImageUuid", TARGET_STACK_VERSION);

        Image originalImage = getImage(true, "originalImageUuid", STACK_VERSION);
        Component originalImageComponent = new Component(ComponentType.IMAGE, ComponentType.IMAGE.name(), new Json(originalImage), stack);

        String platform = stack.cloudPlatform();

        Set<ClusterComponent> originalClusterComponents = clusterComponentSet(cluster);

        when(componentConfigProviderService.getComponentsByStackId(stack.getId())).thenReturn(Set.of(originalImageComponent));
        when(clusterComponentConfigProvider.getComponentsByClusterId(cluster.getId())).thenReturn(originalClusterComponents);
        when(cloudPlatformConnectors.getDefault(platform(platform.toUpperCase()))).thenReturn(cloudConnector);
        when(cloudConnector.regionToDisplayName(stack.getRegion())).thenReturn(stack.getRegion());
        when(conversionService.convert(any(), eq(ClouderaManagerRepo.class))).thenReturn(getClouderaManagerRepo());

        underTest.updateComponentsByStackId(stack, targetImage);

        ArgumentCaptor<Set<Component>> componentCatcher = ArgumentCaptor.forClass(Set.class);
        verify(componentConfigProviderService, times(1)).store(componentCatcher.capture());
        assertEquals(3, componentCatcher.getValue().size());
        assertTrue(componentCatcher.getValue().stream().anyMatch(
                component -> {
                    Object version = component.getAttributes().getValue("version");
                    if (Objects.nonNull(version)) {
                        return ((String) version).contains(TARGET_STACK_VERSION);
                    } else {
                        return false;
                    }
                }));

        ArgumentCaptor<Set<ClusterComponent>> clusterComponentCatcher = ArgumentCaptor.forClass(Set.class);
        verify(clusterComponentConfigProvider, times(1)).store(clusterComponentCatcher.capture());
        // IMAGE is not saved as cluster component
        assertEquals(2, clusterComponentCatcher.getValue().size());
        assertTrue(clusterComponentCatcher.getValue().stream().anyMatch(
                component -> ((String) component.getAttributes().getValue("version")).contains(TARGET_STACK_VERSION)));
    }

    private ImageCatalog getImageCatalog() {
        ImageCatalog imageCatalog = new ImageCatalog();
        imageCatalog.setImageCatalogUrl(CUSTOM_IMAGE_CATALOG_URL);
        imageCatalog.setName("default");
        Workspace ws = new Workspace();
        ws.setId(ORG_ID);
        imageCatalog.setWorkspace(ws);
        imageCatalog.setCreator("someone");
        imageCatalog.setResourceCrn("someCrn");
        return imageCatalog;
    }

    private StatedImage getImageFromCatalog(boolean prewarmed, String uuid, String stackVersion) {
        Image image = getImage(prewarmed, uuid, stackVersion);
        return StatedImage.statedImage(image, "url", "name");
    }

    private Image getImage(boolean prewarmed, String uuid, String stackVersion) {
        Map<String, String> packageVersions = Collections.singletonMap("package", "version");

        Map<String, String> regionImageIdMap = new HashMap<>();
        regionImageIdMap.put("region", uuid);
        Map<String, String> stackDetailsMap = new HashMap<>();
        stackDetailsMap.put("redhat7", "http://foo/parcels");
        stackDetailsMap.put("repoid", String.format("CDH-%s", stackVersion));
        stackDetailsMap.put("repository-version", String.format("%s-1.cdh%s.p0.2457278", stackVersion, stackVersion));
        Map<String, Map<String, String>> imageSetsByProvider = new HashMap<>();
        imageSetsByProvider.put(PLATFORM, regionImageIdMap);

        StackDetails stackDetails = null;
        if (prewarmed) {
            StackRepoDetails repoDetails = new StackRepoDetails(stackDetailsMap, Collections.emptyMap());
            stackDetails = new StackDetails(stackVersion, repoDetails, "1");
        }
        return new Image("imageDate", System.currentTimeMillis(), "imageDesc", "centos7", uuid, stackVersion, Collections.emptyMap(),
                imageSetsByProvider, stackDetails, "centos", packageVersions,
                Collections.emptyList(), Collections.emptyList(), "1");
    }

    private ClusterComponent createClusterComponent(String attributeString, String name, ComponentType componentType, Cluster cluster) {
        Json attributes = new Json(attributeString);
        return new ClusterComponent(componentType, name, attributes, cluster);
    }

    private  Set<ClusterComponent> clusterComponentSet(Cluster cluster) {
        ClusterComponent cdhComponent = createClusterComponent(CDH_ATTRIBUTES, CDH, ComponentType.CDH_PRODUCT_DETAILS, cluster);
        ClusterComponent cmComponent = createClusterComponent(CM_ATTRIBUTES, ComponentType.CM_REPO_DETAILS.name(), ComponentType.CM_REPO_DETAILS, cluster);
        return Set.of(cdhComponent, cmComponent);
    }

    private ClouderaManagerRepo getClouderaManagerRepo() {
        ClouderaManagerRepo clouderaManagerRepo = new ClouderaManagerRepo();
        clouderaManagerRepo.setBaseUrl("http://public-repo-1.hortonworks.com/cm/centos7/7.1.0/updates/7.1.0");
        clouderaManagerRepo.setVersion("7.1.0");
        return clouderaManagerRepo;
    }
}