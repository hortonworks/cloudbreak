package com.sequenceiq.cloudbreak.service;

import static com.sequenceiq.cloudbreak.RepoTestUtil.getDefaultCDHInfo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.spy;
import static org.powermock.api.mockito.PowerMockito.when;

import java.io.IOException;
import java.util.ArrayList;
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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.sequenceiq.cloudbreak.RepoTestUtil;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceGroupType;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.ClusterV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.util.responses.StackMatrixV4Response;
import com.sequenceiq.cloudbreak.cloud.model.ClouderaManagerProduct;
import com.sequenceiq.cloudbreak.cloud.model.ClouderaManagerRepo;
import com.sequenceiq.cloudbreak.cloud.model.Image;
import com.sequenceiq.cloudbreak.cloud.model.component.DefaultCDHEntries;
import com.sequenceiq.cloudbreak.cloud.model.component.DefaultCDHInfo;
import com.sequenceiq.cloudbreak.cloud.model.component.RepositoryInfo;
import com.sequenceiq.cloudbreak.blueprint.utils.BlueprintUtils;
import com.sequenceiq.cloudbreak.common.type.ComponentType;
import com.sequenceiq.cloudbreak.domain.Blueprint;
import com.sequenceiq.cloudbreak.domain.KerberosConfig;
import com.sequenceiq.cloudbreak.domain.json.Json;
import com.sequenceiq.cloudbreak.domain.stack.Component;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.domain.stack.cluster.ClusterComponent;
import com.sequenceiq.cloudbreak.domain.workspace.Workspace;

public class ClouderaManagerClusterCreationSetupServiceTest {

    public static final String CDH_VERSION = "6.2.0";

    public static final String DEFAULT_CDH_VERSION = "1.0.0";

    public static final String CM_VERSION = "6.2.0";

    public static final String DEFAULT_CM_VERSION = "2.0.0";

    public static final String REDHAT_7 = "redhat7";

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

    private Blueprint blueprint;

    private Workspace workspace;

    private Cluster cluster;

    private Component cmRepoComponent;

    private Component imageComponent;

    private List<Component> productComponentList;

    @Before
    public void init() throws JsonProcessingException {
        MockitoAnnotations.initMocks(this);
        workspace = new Workspace();
        clusterRequest = new ClusterV4Request();
        stack = new Stack();
        stack.setId(1L);
        stack.setWorkspace(workspace);
        blueprint = new Blueprint();
        blueprint.setBlueprintText("{}");
        Map<InstanceGroupType, String> userData = new HashMap<>();
        userData.put(InstanceGroupType.CORE, "userdata");
        Image image = new Image("imagename", userData, "centos7", REDHAT_7, "url", "imgcatname",
                "id", Collections.emptyMap());
        cmRepoComponent = spy(new Component(ComponentType.CM_REPO_DETAILS,
                ComponentType.CM_REPO_DETAILS.name(), new Json(getClouderaManagerRepo(false)), stack));
        imageComponent = new Component(ComponentType.IMAGE, ComponentType.IMAGE.name(), new Json(image), stack);
        Component productComponent = spy(new Component(ComponentType.CDH_PRODUCT_DETAILS,
                ComponentType.CDH_PRODUCT_DETAILS.name(), new Json(getClouderaManagerProductRepo()), stack));
        productComponentList = new ArrayList();
        productComponentList.add(productComponent);

        cluster = new Cluster();
        stack.setCluster(cluster);
        KerberosConfig kerberosConfig = new KerberosConfig();
        kerberosConfig.setDomain("domain");
        cluster.setKerberosConfig(kerberosConfig);
        cluster.setBlueprint(blueprint);
        DefaultCDHInfo defaultCDHInfo = getDefaultCDHInfo(DEFAULT_CM_VERSION, DEFAULT_CDH_VERSION);
        setupDefaultClouderaManagerEntries();

        when(defaultCDHEntries.getEntries()).thenReturn(Collections.singletonMap(DEFAULT_CDH_VERSION, defaultCDHInfo));
        StackMatrixV4Response stackMatrixV4Response = new StackMatrixV4Response();
        stackMatrixV4Response.setCdh(Collections.singletonMap(DEFAULT_CDH_VERSION, null));
        when(stackMatrixService.getStackMatrix()).thenReturn(stackMatrixV4Response);
        when(blueprintUtils.getCDHStackVersion(any())).thenReturn(CDH_VERSION);
    }

    @Test
    public void testBaseClouderaManagerClusterComponents() throws IOException {

        List<ClusterComponent> clusterComponents = underTest.prepareClouderaManagerCluster(clusterRequest, cluster,
                null, null,  Optional.of(imageComponent));
        assertEquals(2, clusterComponents.size());
        assertTrue(clusterComponents.stream().anyMatch(component -> ComponentType.CM_REPO_DETAILS.equals(component.getComponentType())
                && component.getAttributes().getValue().contains(DEFAULT_CM_VERSION)));
        assertTrue(clusterComponents.stream().anyMatch(component -> ComponentType.CDH_PRODUCT_DETAILS.equals(component.getComponentType())
                && component.getAttributes().getValue().contains(DEFAULT_CDH_VERSION)));
    }

    @Test
    public void testPrewarmedClouderaManagerClusterComponents() throws IOException {

        List<ClusterComponent> clusterComponents = underTest.prepareClouderaManagerCluster(clusterRequest, cluster, Optional.of(cmRepoComponent),
                productComponentList, Optional.of(imageComponent));
        assertEquals(2, clusterComponents.size());
        assertTrue(clusterComponents.stream().anyMatch(component -> ComponentType.CM_REPO_DETAILS.equals(component.getComponentType())
                && component.getAttributes().getValue().contains(CM_VERSION)));
        assertTrue(clusterComponents.stream().anyMatch(component -> ComponentType.CDH_PRODUCT_DETAILS.equals(component.getComponentType())
                && component.getAttributes().getValue().contains(CDH_VERSION)));
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
                withVersion(CDH_VERSION).
                withParcel("https://archive.cloudera.com/cdh6/6.2.0/parcels/");
        return product;
    }

    private void setupDefaultClouderaManagerEntries() {
        RepositoryInfo clouderaManagerInfo = RepoTestUtil.getClouderaManagerInfo(DEFAULT_CM_VERSION);
        Map<String, RepositoryInfo> clouderaManagerEntries = new HashMap<>();
        clouderaManagerEntries.put(DEFAULT_CM_VERSION, clouderaManagerInfo);

        Mockito.when(defaultClouderaManagerRepoService.getEntries()).thenReturn(clouderaManagerEntries);
        ClouderaManagerRepo clouderaManagerRepo = getClouderaManagerRepo(true);
        Mockito.when(defaultClouderaManagerRepoService.getDefault(REDHAT_7, "CDH", CM_VERSION)).thenReturn(clouderaManagerRepo);
    }
}