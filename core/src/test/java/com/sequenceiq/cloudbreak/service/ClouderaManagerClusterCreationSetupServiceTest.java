package com.sequenceiq.cloudbreak.service;

import static com.sequenceiq.cloudbreak.RepoTestUtil.getDefaultCDHInfo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.spy;
import static org.powermock.api.mockito.PowerMockito.when;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import com.sequenceiq.cloudbreak.RepoTestUtil;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.ClusterV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.util.responses.StackMatrixV4Response;
import com.sequenceiq.cloudbreak.cloud.model.ClouderaManagerProduct;
import com.sequenceiq.cloudbreak.cloud.model.ClouderaManagerRepo;
import com.sequenceiq.cloudbreak.cloud.model.Image;
import com.sequenceiq.cloudbreak.cloud.model.component.DefaultCDHEntries;
import com.sequenceiq.cloudbreak.cloud.model.component.DefaultCDHInfo;
import com.sequenceiq.cloudbreak.cloud.model.component.RepositoryInfo;
import com.sequenceiq.cloudbreak.cmtemplate.utils.BlueprintUtils;
import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.cloudbreak.common.type.ComponentType;
import com.sequenceiq.common.api.type.InstanceGroupType;
import com.sequenceiq.cloudbreak.domain.Blueprint;
import com.sequenceiq.cloudbreak.domain.stack.Component;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.domain.stack.cluster.ClusterComponent;
import com.sequenceiq.cloudbreak.exception.BadRequestException;
import com.sequenceiq.cloudbreak.workspace.model.Workspace;

public class ClouderaManagerClusterCreationSetupServiceTest {

    private static final String NEWER_CDH_VERSION = "1.9.0";

    private static final String SOME_CDH_VERSION = "1.5.0";

    private static final String OLDER_CDH_VERSION = "1.0.0";

    private static final String CM_VERSION = "6.2.0";

    private static final String DEFAULT_CM_VERSION = "2.0.0";

    private static final String REDHAT_7 = "redhat7";

    @Mock
    private DefaultCDHEntries defaultCDHEntries;

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
    public void init() {
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
        Image image = new Image("imagename", userData, "centos7", REDHAT_7, "url", "imgcatname",
                "id", Collections.emptyMap());
        imageComponent = new Component(ComponentType.IMAGE, ComponentType.IMAGE.name(), new Json(image), stack);

        cluster = new Cluster();
        stack.setCluster(cluster);
        cluster.setStack(stack);
        cluster.setBlueprint(blueprint);
        setupDefaultClouderaManagerEntries();

        Map<String, DefaultCDHInfo> defaultCDHInfoMap = Map.of(
                OLDER_CDH_VERSION, getDefaultCDHInfo(DEFAULT_CM_VERSION, OLDER_CDH_VERSION),
                SOME_CDH_VERSION, getDefaultCDHInfo(DEFAULT_CM_VERSION, SOME_CDH_VERSION),
                NEWER_CDH_VERSION, getDefaultCDHInfo(DEFAULT_CM_VERSION, NEWER_CDH_VERSION)
        );
        when(defaultCDHEntries.getEntries()).thenReturn(defaultCDHInfoMap);
        StackMatrixV4Response stackMatrixV4Response = new StackMatrixV4Response();
        stackMatrixV4Response.setCdh(Collections.singletonMap(OLDER_CDH_VERSION, null));
        when(stackMatrixService.getStackMatrix()).thenReturn(stackMatrixV4Response);
    }

    @Test
    public void specificVersionUsedIfAvailable() throws IOException {
        when(blueprintUtils.getCDHStackVersion(any())).thenReturn(OLDER_CDH_VERSION);

        List<ClusterComponent> clusterComponents = underTest.prepareClouderaManagerCluster(clusterRequest, cluster,
                Optional.empty(), List.of(),  Optional.of(imageComponent));

        assertVersionsMatch(clusterComponents, DEFAULT_CM_VERSION, OLDER_CDH_VERSION);
    }

    @Test
    public void latestUsedIfVersionUnspecified() throws IOException {
        when(blueprintUtils.getCDHStackVersion(any())).thenReturn(null);

        List<ClusterComponent> clusterComponents = underTest.prepareClouderaManagerCluster(clusterRequest, cluster,
                Optional.empty(), List.of(),  Optional.of(imageComponent));

        assertVersionsMatch(clusterComponents, DEFAULT_CM_VERSION, NEWER_CDH_VERSION);
    }

    @Test(expected = BadRequestException.class)
    public void throwsForNonExistentVersion() throws IOException {
        when(blueprintUtils.getCDHStackVersion(any())).thenReturn("5.6.7");

        underTest.prepareClouderaManagerCluster(clusterRequest, cluster,
                Optional.empty(), List.of(),  Optional.of(imageComponent));
    }

    @Test
    public void testPrewarmedClouderaManagerClusterComponents() throws IOException {
        when(blueprintUtils.getCDHStackVersion(any())).thenReturn(SOME_CDH_VERSION);
        Component cmRepoComponent = spy(new Component(ComponentType.CM_REPO_DETAILS,
                ComponentType.CM_REPO_DETAILS.name(), new Json(getClouderaManagerRepo(false)), stack));
        Component productComponent = spy(new Component(ComponentType.CDH_PRODUCT_DETAILS,
                ComponentType.CDH_PRODUCT_DETAILS.name(), new Json(getClouderaManagerProductRepo()), stack));
        List<Component> productComponentList = List.of(productComponent);

        List<ClusterComponent> clusterComponents = underTest.prepareClouderaManagerCluster(clusterRequest, cluster, Optional.of(cmRepoComponent),
                productComponentList, Optional.of(imageComponent));

        assertVersionsMatch(clusterComponents, CM_VERSION, SOME_CDH_VERSION);
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
        product.withName("CDH").
                withVersion(SOME_CDH_VERSION).
                withParcel("https://archive.cloudera.com/cdh6/6.2.0/parcels/");
        return product;
    }

    private void setupDefaultClouderaManagerEntries() {
        RepositoryInfo clouderaManagerInfo = RepoTestUtil.getClouderaManagerInfo(DEFAULT_CM_VERSION);
        Map<String, RepositoryInfo> clouderaManagerEntries = new HashMap<>();
        clouderaManagerEntries.put(DEFAULT_CM_VERSION, clouderaManagerInfo);

        Mockito.when(defaultClouderaManagerRepoService.getEntries()).thenReturn(clouderaManagerEntries);
        ClouderaManagerRepo clouderaManagerRepo = getClouderaManagerRepo(true);
        Mockito.when(defaultClouderaManagerRepoService.getDefault(REDHAT_7, "CDH", SOME_CDH_VERSION)).thenReturn(clouderaManagerRepo);
        Mockito.when(defaultClouderaManagerRepoService.getDefault(REDHAT_7, "CDH", NEWER_CDH_VERSION)).thenReturn(clouderaManagerRepo);
        Mockito.when(defaultClouderaManagerRepoService.getDefault(REDHAT_7, "CDH", OLDER_CDH_VERSION)).thenReturn(clouderaManagerRepo);
        Mockito.when(defaultClouderaManagerRepoService.getDefault(REDHAT_7, "CDH", null)).thenReturn(clouderaManagerRepo);
    }
}
