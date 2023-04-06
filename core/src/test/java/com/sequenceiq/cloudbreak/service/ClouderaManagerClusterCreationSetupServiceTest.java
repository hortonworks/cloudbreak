package com.sequenceiq.cloudbreak.service;

import static com.sequenceiq.cloudbreak.RepoTestUtil.getDefaultCDHInfo;
import static com.sequenceiq.common.model.ImageCatalogPlatform.imageCatalogPlatform;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.StackType;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.ClusterV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.util.responses.StackMatrixV4Response;
import com.sequenceiq.cloudbreak.cloud.model.ClouderaManagerProduct;
import com.sequenceiq.cloudbreak.cloud.model.ClouderaManagerRepo;
import com.sequenceiq.cloudbreak.cloud.model.Image;
import com.sequenceiq.cloudbreak.cloud.model.component.ImageBasedDefaultCDHEntries;
import com.sequenceiq.cloudbreak.cloud.model.component.ImageBasedDefaultCDHInfo;
import com.sequenceiq.cloudbreak.cluster.service.ClouderaManagerProductsProvider;
import com.sequenceiq.cloudbreak.cmtemplate.utils.BlueprintUtils;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.cloudbreak.common.service.PlatformStringTransformer;
import com.sequenceiq.cloudbreak.common.type.ComponentType;
import com.sequenceiq.cloudbreak.core.CloudbreakImageCatalogException;
import com.sequenceiq.cloudbreak.domain.Blueprint;
import com.sequenceiq.cloudbreak.domain.stack.Component;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.domain.stack.cluster.ClusterComponent;
import com.sequenceiq.cloudbreak.service.parcel.ParcelFilterService;
import com.sequenceiq.cloudbreak.workspace.model.Workspace;
import com.sequenceiq.common.api.type.InstanceGroupType;

@ExtendWith(MockitoExtension.class)
class ClouderaManagerClusterCreationSetupServiceTest {

    private static final String NEWER_CDH_VERSION = "1.9.0";

    private static final String SOME_CDH_VERSION = "1.5.0";

    private static final String OLDER_CDH_VERSION = "1.0.0";

    private static final String CM_VERSION = "6.2.0";

    private static final String DEFAULT_CM_VERSION = "2.0.0";

    private static final String REDHAT_7 = "redhat7";

    private static final String CENTOS_7 = "centos7";

    private static final String IMAGE_CATALOG_NAME = "imgcatname";

    private static final long STACK_ID = 1L;

    private static final long WORKSPACE_ID = 2L;

    @Mock
    private ImageBasedDefaultCDHEntries imageBasedDefaultCDHEntries;

    @Mock
    private ParcelFilterService parcelFilterService;

    @Mock
    private StackMatrixService stackMatrixService;

    @Mock
    private DefaultClouderaManagerRepoService defaultClouderaManagerRepoService;

    @Mock
    private BlueprintUtils blueprintUtils;

    @Mock
    private PlatformStringTransformer platformStringTransformer;

    @Mock
    private ClouderaManagerProductsProvider clouderaManagerProductsProvider;

    @InjectMocks
    private ClouderaManagerClusterCreationSetupService underTest;

    private ClusterV4Request clusterRequest;

    private Stack stack;

    private Cluster cluster;

    private Component imageComponent;

    @BeforeEach
    void init() throws CloudbreakImageCatalogException {
        Workspace workspace = new Workspace();
        workspace.setId(WORKSPACE_ID);

        clusterRequest = new ClusterV4Request();
        stack = new Stack();
        stack.setId(STACK_ID);
        stack.setName("test-stack");
        stack.setWorkspace(workspace);
        stack.setCloudPlatform("AWS");
        stack.setPlatformVariant("AWS");
        Blueprint blueprint = new Blueprint();
        blueprint.setBlueprintText("{}");
        Map<InstanceGroupType, String> userData = new HashMap<>();
        userData.put(InstanceGroupType.CORE, "userdata");
        Image image = new Image("imagename", userData, CENTOS_7, REDHAT_7, "url", IMAGE_CATALOG_NAME,
                "id", Collections.emptyMap());
        imageComponent = new Component(ComponentType.IMAGE, ComponentType.IMAGE.name(), new Json(image), stack);

        cluster = new Cluster();
        stack.setCluster(cluster);
        cluster.setStack(stack);
        cluster.setBlueprint(blueprint);
        cluster.setWorkspace(workspace);
        setupDefaultClouderaManagerEntries();

        Map<String, ImageBasedDefaultCDHInfo> defaultCDHInfoMap = Map.of(
                OLDER_CDH_VERSION, new ImageBasedDefaultCDHInfo(getDefaultCDHInfo(OLDER_CDH_VERSION),
                        mock(com.sequenceiq.cloudbreak.cloud.model.catalog.Image.class)),
                SOME_CDH_VERSION, new ImageBasedDefaultCDHInfo(getDefaultCDHInfo(SOME_CDH_VERSION),
                        mock(com.sequenceiq.cloudbreak.cloud.model.catalog.Image.class)),
                NEWER_CDH_VERSION, new ImageBasedDefaultCDHInfo(getDefaultCDHInfo(NEWER_CDH_VERSION),
                        mock(com.sequenceiq.cloudbreak.cloud.model.catalog.Image.class)));
        lenient().when(imageBasedDefaultCDHEntries.getEntries(workspace.getId(), imageCatalogPlatform("AWS"), CENTOS_7, IMAGE_CATALOG_NAME))
                .thenReturn(defaultCDHInfoMap);
        StackMatrixV4Response stackMatrixV4Response = new StackMatrixV4Response();
        stackMatrixV4Response.setCdh(Collections.singletonMap(OLDER_CDH_VERSION, null));
        lenient().when(stackMatrixService.getStackMatrix(eq(workspace.getId()), eq(imageCatalogPlatform("AWS")), eq(CENTOS_7), anyString()))
                .thenReturn(stackMatrixV4Response);
        when(platformStringTransformer.getPlatformStringForImageCatalog(anyString(), anyString())).thenReturn(imageCatalogPlatform("AWS"));
    }

    @Test
    void specificVersionUsedIfAvailable() throws IOException, CloudbreakImageCatalogException {
        when(blueprintUtils.getCDHStackVersion(any())).thenReturn(OLDER_CDH_VERSION);

        List<ClusterComponent> clusterComponents = underTest.prepareClouderaManagerCluster(clusterRequest, cluster,
                null, List.of(), imageComponent);

        assertVersionsMatch(clusterComponents, DEFAULT_CM_VERSION, OLDER_CDH_VERSION);
    }

    @Test
    void latestUsedIfVersionUnspecified() throws IOException, CloudbreakImageCatalogException {
        when(blueprintUtils.getCDHStackVersion(any())).thenReturn(null);

        List<ClusterComponent> clusterComponents = underTest.prepareClouderaManagerCluster(clusterRequest, cluster,
                null, List.of(), imageComponent);

        assertVersionsMatch(clusterComponents, DEFAULT_CM_VERSION, NEWER_CDH_VERSION);
    }

    @Test
    void throwsForNonExistentVersion() throws IOException, CloudbreakImageCatalogException {
        when(blueprintUtils.getCDHStackVersion(any())).thenReturn("5.6.7");

        assertThrows(BadRequestException.class, () ->
                underTest.prepareClouderaManagerCluster(clusterRequest, cluster,
                        null, List.of(), imageComponent));
    }

    @Test
    void testPrewarmedClouderaManagerClusterComponents() throws IOException, CloudbreakImageCatalogException {
        Component cmRepoComponent = spy(new Component(ComponentType.CM_REPO_DETAILS,
                ComponentType.CM_REPO_DETAILS.name(), new Json(getClouderaManagerRepo(false)), stack));
        Component productComponent = spy(new Component(ComponentType.CDH_PRODUCT_DETAILS,
                ComponentType.CDH_PRODUCT_DETAILS.name(), new Json(getClouderaManagerProductRepo()), stack));
        List<Component> productComponentList = List.of(productComponent);
        Set<ClouderaManagerProduct> clouderaManagerProductSet = new HashSet<>();
        clouderaManagerProductSet.add(clouderaManagerProduct("CDH", "1.5.0"));

        when(blueprintUtils.getCDHStackVersion(any())).thenReturn(SOME_CDH_VERSION);
        when(parcelFilterService.filterParcelsByBlueprint(eq(WORKSPACE_ID), eq(STACK_ID), anySet(), any(Blueprint.class))).thenReturn(clouderaManagerProductSet);

        List<ClusterComponent> clusterComponents = underTest.prepareClouderaManagerCluster(clusterRequest, cluster, cmRepoComponent,
                productComponentList, imageComponent);

        assertVersionsMatch(clusterComponents, CM_VERSION, SOME_CDH_VERSION);
        verify(parcelFilterService, times(1)).filterParcelsByBlueprint(eq(WORKSPACE_ID), eq(STACK_ID), anySet(), any(Blueprint.class));
    }

    @Test
    void testPrewarmedClouderaManagerClusterComponentsWhenTheStackTypeIsDataLake() throws IOException, CloudbreakImageCatalogException {
        Component cmRepoComponent = spy(new Component(ComponentType.CM_REPO_DETAILS,
                ComponentType.CM_REPO_DETAILS.name(), new Json(getClouderaManagerRepo(false)), stack));
        Component productComponent = spy(new Component(ComponentType.CDH_PRODUCT_DETAILS,
                ComponentType.CDH_PRODUCT_DETAILS.name(), new Json(getClouderaManagerProductRepo()), stack));
        List<Component> productComponentList = List.of(productComponent);
        ClouderaManagerProduct cdhProduct = clouderaManagerProduct("CDH", "1.5.0");
        stack.setType(StackType.DATALAKE);

        when(blueprintUtils.getCDHStackVersion(any())).thenReturn(SOME_CDH_VERSION);
        when(clouderaManagerProductsProvider.getCdhProducts(anySet())).thenReturn(cdhProduct);

        List<ClusterComponent> clusterComponents = underTest.prepareClouderaManagerCluster(clusterRequest, cluster, cmRepoComponent,
                productComponentList, imageComponent);

        assertVersionsMatch(clusterComponents, CM_VERSION, SOME_CDH_VERSION);
        verify(clouderaManagerProductsProvider).getCdhProducts(anySet());
        verifyNoInteractions(parcelFilterService);
    }

    private void assertVersionsMatch(Collection<ClusterComponent> clusterComponents, String cmVersion, String cdhVersion) {
        assertNotNull(clusterComponents);
        assertEquals(2, clusterComponents.size());
        assertContainsVersion(cmVersion, findComponentByType(clusterComponents, ComponentType.CM_REPO_DETAILS));
        assertContainsVersion(cdhVersion, findComponentByType(clusterComponents, ComponentType.CDH_PRODUCT_DETAILS));
    }

    private void assertContainsVersion(String expectedVersion, ClusterComponent component) {
        String versionStr = String.format("\"version\":\"%s\"", expectedVersion);
        assertTrue(component.getAttributes().getValue().contains(versionStr),
                String.format("Expected '%s' to contain '%s'", component.getAttributes().getValue(), versionStr));
    }

    private ClusterComponent findComponentByType(Collection<ClusterComponent> clusterComponents, ComponentType type) {
        return clusterComponents.stream()
                .filter(component -> type.equals(component.getComponentType()))
                .findAny()
                .orElseThrow(() -> new AssertionError("component not found: " + type));
    }

    private ClouderaManagerRepo getClouderaManagerRepo(boolean defaultRepo) {
        ClouderaManagerRepo clouderaManagerRepo = new ClouderaManagerRepo();
        clouderaManagerRepo.setBaseUrl("http://public-repo-1.hortonworks.com/cm/centos7/6.2.0/updates/6.2.0");
        clouderaManagerRepo.setVersion(defaultRepo ? DEFAULT_CM_VERSION : CM_VERSION);
        return clouderaManagerRepo;
    }

    private ClouderaManagerProduct getClouderaManagerProductRepo() {
        ClouderaManagerProduct product = new ClouderaManagerProduct();
        product.withName("CDH").withVersion(SOME_CDH_VERSION).withParcel("https://archive.cloudera.com/cdh6/6.2.0/parcels/");
        return product;
    }

    private void setupDefaultClouderaManagerEntries() throws CloudbreakImageCatalogException {
        ClouderaManagerRepo clouderaManagerRepo = getClouderaManagerRepo(true);
        lenient().when(defaultClouderaManagerRepoService.getDefault(REDHAT_7, CENTOS_7, "CDH", SOME_CDH_VERSION, imageCatalogPlatform("AWS")))
                .thenReturn(clouderaManagerRepo);
        lenient().when(defaultClouderaManagerRepoService.getDefault(REDHAT_7, CENTOS_7, "CDH", NEWER_CDH_VERSION, (imageCatalogPlatform("AWS"))))
                .thenReturn(clouderaManagerRepo);
        lenient().when(defaultClouderaManagerRepoService.getDefault(REDHAT_7, CENTOS_7, "CDH", OLDER_CDH_VERSION, imageCatalogPlatform("AWS")))
                .thenReturn(clouderaManagerRepo);
        lenient().when(defaultClouderaManagerRepoService.getDefault(REDHAT_7, CENTOS_7, "CDH", null, imageCatalogPlatform("AWS")))
                .thenReturn(clouderaManagerRepo);
    }

    private ClouderaManagerProduct clouderaManagerProduct(String name, String version) {
        ClouderaManagerProduct clouderaManagerProduct = new ClouderaManagerProduct();
        clouderaManagerProduct.setCsd(new ArrayList<>());
        clouderaManagerProduct.setVersion(version);
        clouderaManagerProduct.setParcel(String.format("http://public-repo-1.hortonworks.com/cm/centos7/%s/updates/%s", version, version));
        clouderaManagerProduct.setName(name);
        clouderaManagerProduct.setDisplayName(name);
        return clouderaManagerProduct;
    }
}
