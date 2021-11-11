package com.sequenceiq.cloudbreak.service;

import static com.sequenceiq.cloudbreak.RepoTestUtil.getDefaultCDHInfo;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.powermock.api.mockito.PowerMockito.when;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.ClusterV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.util.responses.StackMatrixV4Response;
import com.sequenceiq.cloudbreak.cloud.model.ClouderaManagerProduct;
import com.sequenceiq.cloudbreak.cloud.model.ClouderaManagerRepo;
import com.sequenceiq.cloudbreak.cloud.model.Image;
import com.sequenceiq.cloudbreak.cloud.model.component.ImageBasedDefaultCDHEntries;
import com.sequenceiq.cloudbreak.cloud.model.component.ImageBasedDefaultCDHInfo;
import com.sequenceiq.cloudbreak.cmtemplate.utils.BlueprintUtils;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.cloudbreak.common.json.Json;
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

public class ClouderaManagerClusterCreationSetupServiceTest {

    private static final String NEWER_CDH_VERSION = "1.9.0";

    private static final String SOME_CDH_VERSION = "1.5.0";

    private static final String OLDER_CDH_VERSION = "1.0.0";

    private static final String CM_VERSION = "6.2.0";

    private static final String DEFAULT_CM_VERSION = "2.0.0";

    private static final String REDHAT_7 = "redhat7";

    private static final String IMAGE_CATALOG_NAME = "imgcatname";

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

    @InjectMocks
    private ClouderaManagerClusterCreationSetupService underTest;

    private ClusterV4Request clusterRequest;

    private Stack stack;

    private Cluster cluster;

    private Component imageComponent;

    @Before
    public void init() throws CloudbreakImageCatalogException {
        MockitoAnnotations.initMocks(this);
        Workspace workspace = new Workspace();
        clusterRequest = new ClusterV4Request();
        stack = new Stack();
        stack.setId(1L);
        stack.setName("test-stack");
        stack.setWorkspace(workspace);
        Blueprint blueprint = new Blueprint();
        blueprint.setBlueprintText("{}");
        Map<InstanceGroupType, String> userData = new HashMap<>();
        userData.put(InstanceGroupType.CORE, "userdata");
        Image image = new Image("imagename", userData, "centos7", REDHAT_7, "url", IMAGE_CATALOG_NAME,
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
        when(imageBasedDefaultCDHEntries.getEntries(workspace.getId(), null, IMAGE_CATALOG_NAME)).thenReturn(defaultCDHInfoMap);
        StackMatrixV4Response stackMatrixV4Response = new StackMatrixV4Response();
        stackMatrixV4Response.setCdh(Collections.singletonMap(OLDER_CDH_VERSION, null));
        when(stackMatrixService.getStackMatrix(Mockito.eq(workspace.getId()), Mockito.eq(null), Mockito.anyString())).thenReturn(stackMatrixV4Response);
    }

    @Test
    public void specificVersionUsedIfAvailable() throws IOException, CloudbreakImageCatalogException {
        Set<ClouderaManagerProduct> clouderaManagerProductSet = new HashSet<>();
        clouderaManagerProductSet.add(clouderaManagerProduct("CDH", "1.0.0"));

        when(blueprintUtils.getCDHStackVersion(any())).thenReturn(OLDER_CDH_VERSION);
        when(parcelFilterService.filterParcelsByBlueprint(anySet(), any(Blueprint.class))).thenReturn(clouderaManagerProductSet);

        List<ClusterComponent> clusterComponents = underTest.prepareClouderaManagerCluster(clusterRequest, cluster,
                Optional.empty(), List.of(), Optional.of(imageComponent));

        assertVersionsMatch(clusterComponents, DEFAULT_CM_VERSION, OLDER_CDH_VERSION);
        verify(parcelFilterService, times(1)).filterParcelsByBlueprint(anySet(), any(Blueprint.class));
    }

    @Test
    public void latestUsedIfVersionUnspecified() throws IOException, CloudbreakImageCatalogException {
        Set<ClouderaManagerProduct> clouderaManagerProductSet = new HashSet<>();
        clouderaManagerProductSet.add(clouderaManagerProduct("CDH", "1.9.0"));

        when(blueprintUtils.getCDHStackVersion(any())).thenReturn(null);
        when(parcelFilterService.filterParcelsByBlueprint(anySet(), any(Blueprint.class))).thenReturn(clouderaManagerProductSet);

        List<ClusterComponent> clusterComponents = underTest.prepareClouderaManagerCluster(clusterRequest, cluster,
                Optional.empty(), List.of(), Optional.of(imageComponent));

        assertVersionsMatch(clusterComponents, DEFAULT_CM_VERSION, NEWER_CDH_VERSION);
        verify(parcelFilterService, times(1)).filterParcelsByBlueprint(anySet(), any(Blueprint.class));
    }

    @Test(expected = BadRequestException.class)
    public void throwsForNonExistentVersion() throws IOException, CloudbreakImageCatalogException {
        when(blueprintUtils.getCDHStackVersion(any())).thenReturn("5.6.7");

        underTest.prepareClouderaManagerCluster(clusterRequest, cluster,
                Optional.empty(), List.of(), Optional.of(imageComponent));
    }

    @Test
    public void testPrewarmedClouderaManagerClusterComponents() throws IOException, CloudbreakImageCatalogException {
        Component cmRepoComponent = spy(new Component(ComponentType.CM_REPO_DETAILS,
                ComponentType.CM_REPO_DETAILS.name(), new Json(getClouderaManagerRepo(false)), stack));
        Component productComponent = spy(new Component(ComponentType.CDH_PRODUCT_DETAILS,
                ComponentType.CDH_PRODUCT_DETAILS.name(), new Json(getClouderaManagerProductRepo()), stack));
        List<Component> productComponentList = List.of(productComponent);
        Set<ClouderaManagerProduct> clouderaManagerProductSet = new HashSet<>();
        clouderaManagerProductSet.add(clouderaManagerProduct("CDH", "1.5.0"));

        when(blueprintUtils.getCDHStackVersion(any())).thenReturn(SOME_CDH_VERSION);
        when(parcelFilterService.filterParcelsByBlueprint(anySet(), any(Blueprint.class))).thenReturn(clouderaManagerProductSet);

        List<ClusterComponent> clusterComponents = underTest.prepareClouderaManagerCluster(clusterRequest, cluster, Optional.of(cmRepoComponent),
                productComponentList, Optional.of(imageComponent));

        assertVersionsMatch(clusterComponents, CM_VERSION, SOME_CDH_VERSION);
        verify(parcelFilterService, times(1)).filterParcelsByBlueprint(anySet(), any(Blueprint.class));
    }

    private void assertVersionsMatch(Collection<ClusterComponent> clusterComponents, String cmVersion, String cdhVersion) {
        assertNotNull(clusterComponents);
        assertEquals(2, clusterComponents.size());
        assertContainsVersion(cmVersion, findComponentByType(clusterComponents, ComponentType.CM_REPO_DETAILS));
        assertContainsVersion(cdhVersion, findComponentByType(clusterComponents, ComponentType.CDH_PRODUCT_DETAILS));
    }

    private void assertContainsVersion(String expectedVersion, ClusterComponent component) {
        String versionStr = String.format("\"version\":\"%s\"", expectedVersion);
        assertTrue(String.format("Expected '%s' to contain '%s'", component.getAttributes().getValue(), versionStr),
                component.getAttributes().getValue().contains(versionStr));
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
        Mockito.when(defaultClouderaManagerRepoService.getDefault(REDHAT_7, "CDH", SOME_CDH_VERSION, null)).thenReturn(clouderaManagerRepo);
        Mockito.when(defaultClouderaManagerRepoService.getDefault(REDHAT_7, "CDH", NEWER_CDH_VERSION, null)).thenReturn(clouderaManagerRepo);
        Mockito.when(defaultClouderaManagerRepoService.getDefault(REDHAT_7, "CDH", OLDER_CDH_VERSION, null)).thenReturn(clouderaManagerRepo);
        Mockito.when(defaultClouderaManagerRepoService.getDefault(REDHAT_7, "CDH", null, null)).thenReturn(clouderaManagerRepo);
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
