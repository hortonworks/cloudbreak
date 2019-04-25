package com.sequenceiq.cloudbreak.service;

import static com.sequenceiq.cloudbreak.RepoTestUtil.getDefaultHDPInfo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.spy;
import static org.powermock.api.mockito.PowerMockito.when;

import java.io.IOException;
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
import org.mockito.Spy;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.common.collect.Sets;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceGroupType;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.ClusterV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.util.responses.StackMatrixV4Response;
import com.sequenceiq.cloudbreak.cloud.model.AmbariRepo;
import com.sequenceiq.cloudbreak.cloud.model.Image;
import com.sequenceiq.cloudbreak.cloud.model.component.DefaultHDPEntries;
import com.sequenceiq.cloudbreak.cloud.model.component.DefaultHDPInfo;
import com.sequenceiq.cloudbreak.cloud.model.component.RepositoryInfo;
import com.sequenceiq.cloudbreak.cloud.model.component.StackRepoDetails;
import com.sequenceiq.cloudbreak.cluster.api.ClusterPreCreationApi;
import com.sequenceiq.cloudbreak.blueprint.utils.BlueprintUtils;
import com.sequenceiq.cloudbreak.common.type.ComponentType;
import com.sequenceiq.cloudbreak.core.CloudbreakImageNotFoundException;
import com.sequenceiq.cloudbreak.domain.Blueprint;
import com.sequenceiq.cloudbreak.domain.KerberosConfig;
import com.sequenceiq.cloudbreak.domain.json.Json;
import com.sequenceiq.cloudbreak.domain.stack.Component;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.domain.stack.cluster.ClusterComponent;
import com.sequenceiq.cloudbreak.domain.workspace.Workspace;
import com.sequenceiq.cloudbreak.service.cluster.ClusterApiConnectors;
import com.sequenceiq.cloudbreak.service.cluster.ClusterService;
import com.sequenceiq.cloudbreak.service.blueprint.BlueprintService;
import com.sequenceiq.cloudbreak.service.decorator.ClusterDecorator;

public class AmbariClusterCreationSetupServiceTest {

    public static final String REDHAT_7 = "redhat7";

    public static final String HDP_VERSION = "3.1.0.0";

    public static final String DEFAULT_HDP_VERSION = "2.6.5.0";

    public static final String AMBARI_VERSION = "2.6.2.2";

    public static final String DEFAULT_AMBARI_VERSION = "2.7.3.0";

    @Mock
    private ClusterDecorator clusterDecorator;

    @Mock
    private ComponentConfigProviderService componentConfigProviderService;

    @Mock
    private BlueprintUtils blueprintUtils;

    @Mock
    private BlueprintService blueprintService;

    @Mock
    private DefaultHDPEntries defaultHDPEntries;

    @Mock
    private StackMatrixService stackMatrixService;

    @Mock
    private ClusterService clusterService;

    @Mock
    private ClusterApiConnectors clusterApiConnectors;

    @Mock
    private DefaultAmbariRepoService defaultAmbariRepoService;

    @Spy
    private ClusterPreCreationApi clusterPreCreationApi;

    @InjectMocks
    private AmbariClusterCreationSetupService underTest;

    private ClusterV4Request clusterRequest;

    private Stack stack;

    private Blueprint blueprint;

    private Workspace workspace;

    private Cluster cluster;

    private Component ambariComponent;

    private Component imageComponent;

    private Component stackComponent;

    @Before
    public void init() throws CloudbreakImageNotFoundException, JsonProcessingException {
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
        ambariComponent = spy(new Component(ComponentType.AMBARI_REPO_DETAILS,
                ComponentType.AMBARI_REPO_DETAILS.name(), new Json(getAmbariRepo(false)), stack));
        imageComponent = new Component(ComponentType.IMAGE, ComponentType.IMAGE.name(), new Json(image), stack);
        stackComponent = spy(new Component(ComponentType.HDP_REPO_DETAILS,
                ComponentType.HDP_REPO_DETAILS.name(), new Json(getStackRepo(false)), stack));

        cluster = new Cluster();
        stack.setCluster(cluster);
        KerberosConfig kerberosConfig = new KerberosConfig();
        kerberosConfig.setDomain("domain");
        cluster.setKerberosConfig(kerberosConfig);
        cluster.setBlueprint(blueprint);
        when(clusterDecorator.decorate(any(), any(), any(), any(), any(), any())).thenReturn(cluster);
        when(componentConfigProviderService.getAllComponentsByStackIdAndType(any(), any())).thenReturn(Sets.newHashSet(ambariComponent, imageComponent));
        when(blueprintUtils.getBlueprintStackVersion(any())).thenReturn(DEFAULT_HDP_VERSION);
        when(blueprintUtils.getBlueprintStackName(any())).thenReturn("HDP");

        DefaultHDPInfo defaultHDPInfo = getDefaultHDPInfo("2.7.0", DEFAULT_HDP_VERSION);
        setupDefaultAmbariEntries();

        when(defaultHDPEntries.getEntries()).thenReturn(Collections.singletonMap(DEFAULT_HDP_VERSION, defaultHDPInfo));
        when(componentConfigProviderService.getImage(anyLong())).thenReturn(image);
        StackMatrixV4Response stackMatrixV4Response = new StackMatrixV4Response();
        stackMatrixV4Response.setHdp(Collections.singletonMap(DEFAULT_HDP_VERSION, null));
        when(stackMatrixService.getStackMatrix()).thenReturn(stackMatrixV4Response);
        when(clusterService.save(any(Cluster.class))).thenReturn(cluster);
    }

    @Test
    public void testBaseAmbariClusterComponents() throws CloudbreakImageNotFoundException, IOException {
        when(blueprintService.isAmbariBlueprint(blueprint)).thenReturn(true);
        when(clusterApiConnectors.getConnector(any(Cluster.class))).thenReturn(clusterPreCreationApi);
        when(clusterPreCreationApi.isVdfReady(any(AmbariRepo.class))).thenReturn(false);
        List<ClusterComponent> clusterComponents = underTest.prepareAmbariCluster(clusterRequest, stack, blueprint, cluster, null, null,
                Optional.of(imageComponent));

        assertEquals(2, clusterComponents.size());
        assertTrue(clusterComponents.stream().anyMatch(component -> ComponentType.AMBARI_REPO_DETAILS.equals(component.getComponentType())
                && component.getAttributes().getValue().contains(DEFAULT_AMBARI_VERSION)));
        assertTrue(clusterComponents.stream().anyMatch(component -> ComponentType.HDP_REPO_DETAILS.equals(component.getComponentType())
                && component.getAttributes().getValue().contains(DEFAULT_HDP_VERSION)));
    }

    @Test
    public void testPrewarmedAmbariClusterComponents() throws CloudbreakImageNotFoundException, IOException {
        when(blueprintService.isAmbariBlueprint(blueprint)).thenReturn(true);
        when(clusterApiConnectors.getConnector(any(Cluster.class))).thenReturn(clusterPreCreationApi);
        when(clusterPreCreationApi.isVdfReady(any(AmbariRepo.class))).thenReturn(false);
        List<ClusterComponent> clusterComponents = underTest.prepareAmbariCluster(clusterRequest, stack, blueprint,
                cluster, Optional.of(ambariComponent), Optional.of(stackComponent), Optional.of(imageComponent));

        assertEquals(2, clusterComponents.size());
        assertTrue(clusterComponents.stream().anyMatch(component -> ComponentType.AMBARI_REPO_DETAILS.equals(component.getComponentType())
                && component.getAttributes().getValue().contains(AMBARI_VERSION)));
        assertTrue(clusterComponents.stream().anyMatch(component -> ComponentType.HDP_REPO_DETAILS.equals(component.getComponentType())
                && component.getAttributes().getValue().contains(HDP_VERSION)));
    }

    private AmbariRepo getAmbariRepo(boolean defaultRepo) {
        String version = defaultRepo ? DEFAULT_AMBARI_VERSION : AMBARI_VERSION;
        AmbariRepo ambariRepo = new AmbariRepo();
        ambariRepo.setBaseUrl("http://public-repo-1.hortonworks.com/ambari/centos7/2.x/updates/" + version);
        ambariRepo.setVersion(version);
        return ambariRepo;
    }

    private void setupDefaultAmbariEntries() {
        Map<String, RepositoryInfo> ambariEntries = new HashMap<>();
        Mockito.when(defaultAmbariRepoService.getEntries()).thenReturn(ambariEntries);
        AmbariRepo ambariRepo = getAmbariRepo(true);
        Mockito.when(defaultAmbariRepoService.getDefault(REDHAT_7)).thenReturn(ambariRepo);
        Mockito.when(defaultAmbariRepoService.getDefault(REDHAT_7, "HDP", DEFAULT_HDP_VERSION)).thenReturn(ambariRepo);
    }

    private StackRepoDetails getStackRepo(boolean defaultRepo) {
        String version = defaultRepo ? DEFAULT_HDP_VERSION : HDP_VERSION;
        StackRepoDetails repoDetails = new StackRepoDetails();
        repoDetails.setHdpVersion(version);
        Map stackMap = new HashMap<>();
        stackMap.put(StackRepoDetails.REPO_ID_TAG, version);
        stackMap.put(StackRepoDetails.REPOSITORY_VERSION, version + "-292");
        stackMap.put(StackRepoDetails.CUSTOM_VDF_REPO_KEY,
                "http://public-repo-1.hortonworks.com/HDP/centos7/2.x/updates/" + version + "/HDP-" + version + "-292.xml");
        stackMap.put("redhat7", "http://public-repo-1.hortonworks.com/HDP/centos7/2.x/updates/" + version);
        repoDetails.setStack(stackMap);
        return repoDetails;
    }
}