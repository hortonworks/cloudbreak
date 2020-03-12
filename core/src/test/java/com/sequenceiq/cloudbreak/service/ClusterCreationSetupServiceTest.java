package com.sequenceiq.cloudbreak.service;

import static com.sequenceiq.cloudbreak.RepoTestUtil.getDefaultCDHInfo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.powermock.api.mockito.PowerMockito.when;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import com.google.common.collect.Sets;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.ClusterV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.util.responses.StackMatrixV4Response;
import com.sequenceiq.cloudbreak.cloud.model.ClouderaManagerRepo;
import com.sequenceiq.cloudbreak.cloud.model.Image;
import com.sequenceiq.cloudbreak.cloud.model.component.DefaultCDHEntries;
import com.sequenceiq.cloudbreak.cloud.model.component.DefaultCDHInfo;
import com.sequenceiq.cloudbreak.cloud.model.component.RepositoryInfo;
import com.sequenceiq.cloudbreak.cmtemplate.utils.BlueprintUtils;
import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.cloudbreak.common.service.TransactionService;
import com.sequenceiq.cloudbreak.common.type.ComponentType;
import com.sequenceiq.cloudbreak.core.CloudbreakImageNotFoundException;
import com.sequenceiq.cloudbreak.domain.Blueprint;
import com.sequenceiq.cloudbreak.domain.stack.Component;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.dto.KerberosConfig;
import com.sequenceiq.cloudbreak.kerberos.KerberosConfigService;
import com.sequenceiq.cloudbreak.service.blueprint.BlueprintService;
import com.sequenceiq.cloudbreak.service.cluster.flow.ClusterOperationService;
import com.sequenceiq.cloudbreak.service.decorator.ClusterDecorator;
import com.sequenceiq.cloudbreak.workspace.model.User;
import com.sequenceiq.cloudbreak.workspace.model.Workspace;
import com.sequenceiq.common.api.type.InstanceGroupType;

public class ClusterCreationSetupServiceTest {

    public static final String REDHAT_7 = "redhat7";

    public static final String HDP_VERSION = "3.0";

    public static final String CDH_VERSION = "6.1.0";

    @Mock
    private ClouderaManagerClusterCreationSetupService clouderaManagerClusterCreationSetupService;

    @Mock
    private ClusterDecorator clusterDecorator;

    @Mock
    private ComponentConfigProviderService componentConfigProviderService;

    @Mock
    private BlueprintUtils blueprintUtils;

    @Mock
    private BlueprintService blueprintService;

    @Mock
    private DefaultCDHEntries defaultCDHEntries;

    @Mock
    private StackMatrixService stackMatrixService;

    @Mock
    private ClusterOperationService clusterOperationService;

    @Mock
    private DefaultClouderaManagerRepoService defaultClouderaManagerRepoService;

    @Mock
    private KerberosConfigService kerberosConfigService;

    @InjectMocks
    private ClusterCreationSetupService underTest;

    private ClusterV4Request clusterRequest;

    private Stack stack;

    private Blueprint blueprint;

    private User user;

    private Workspace workspace;

    private Cluster cluster;

    @Before
    public void init() throws CloudbreakImageNotFoundException, IOException {
        MockitoAnnotations.initMocks(this);
        workspace = new Workspace();
        clusterRequest = new ClusterV4Request();
        stack = new Stack();
        stack.setId(1L);
        stack.setWorkspace(workspace);
        stack.setEnvironmentCrn("env");
        stack.setName("name");
        blueprint = new Blueprint();
        blueprint.setBlueprintText("{}");
        user = new User();
        Map<InstanceGroupType, String> userData = new HashMap<>();
        userData.put(InstanceGroupType.CORE, "userdata");
        Image image = new Image("imagename", userData, "centos7", REDHAT_7, "url", "imgcatname",
                "id", Collections.emptyMap());
        Component imageComponent = new Component(ComponentType.IMAGE, ComponentType.IMAGE.name(), new Json(image), stack);

        cluster = new Cluster();
        stack.setCluster(cluster);
        when(clusterDecorator.decorate(any(), any(), any(), any(), any(), any(), any())).thenReturn(cluster);
        when(componentConfigProviderService.getAllComponentsByStackIdAndType(any(), any())).thenReturn(Sets.newHashSet(imageComponent));
        when(blueprintUtils.getBlueprintStackVersion(any())).thenReturn(HDP_VERSION);
        when(blueprintUtils.getBlueprintStackName(any())).thenReturn("HDP");

        DefaultCDHInfo defaultCDHInfo = getDefaultCDHInfo("6.1.0", CDH_VERSION);
        setupClouderaManagerEntries();

        when(defaultCDHEntries.getEntries()).thenReturn(Collections.singletonMap(CDH_VERSION, defaultCDHInfo));
        when(componentConfigProviderService.getImage(anyLong())).thenReturn(image);
        StackMatrixV4Response stackMatrixV4Response = new StackMatrixV4Response();
        stackMatrixV4Response.setCdh(Collections.singletonMap(CDH_VERSION, null));
        when(stackMatrixService.getStackMatrix()).thenReturn(stackMatrixV4Response);
        when(clouderaManagerClusterCreationSetupService.prepareClouderaManagerCluster(any(), any(), any(), any(), any())).
                thenReturn(new ArrayList<>());
    }

    @Test
    public void testDomainIsSet() throws CloudbreakImageNotFoundException, IOException, TransactionService.TransactionExecutionException {
        KerberosConfig kerberosConfig = KerberosConfig.KerberosConfigBuilder.aKerberosConfig()
                .withDomain("domain").build();
        when(kerberosConfigService.get(anyString(), anyString())).thenReturn(Optional.of(kerberosConfig));
        underTest.prepare(clusterRequest, stack, blueprint, user, null);
        assertEquals("domain", stack.getCustomDomain());
    }

    @Test
    public void testMissingKerberosConfig() throws CloudbreakImageNotFoundException, IOException, TransactionService.TransactionExecutionException {
        underTest.prepare(clusterRequest, stack, blueprint, user, null);
        assertNull(stack.getCustomDomain());
    }

    @Test
    public void testMissingDomain() throws CloudbreakImageNotFoundException, IOException, TransactionService.TransactionExecutionException {
        KerberosConfig kerberosConfig = KerberosConfig.KerberosConfigBuilder.aKerberosConfig().build();
        when(kerberosConfigService.get(anyString(), anyString())).thenReturn(Optional.of(kerberosConfig));
        underTest.prepare(clusterRequest, stack, blueprint, user, null);
        assertNull(stack.getCustomDomain());
    }

    private ClouderaManagerRepo getClouderaManagerRepo() {
        ClouderaManagerRepo clouderaManagerRepo = new ClouderaManagerRepo();
        clouderaManagerRepo.setBaseUrl("http://public-repo-1.hortonworks.com/cm/centos7/6.1.0/updates/6.1.0");
        clouderaManagerRepo.setVersion("6.1.0");
        return clouderaManagerRepo;
    }

    private void setupClouderaManagerEntries() {
        Map<String, RepositoryInfo> clouderaManagerEntries = new HashMap<>();
        Mockito.when(defaultClouderaManagerRepoService.getEntries()).thenReturn(clouderaManagerEntries);
        ClouderaManagerRepo clouderaManagerRepo = getClouderaManagerRepo();
        Mockito.when(defaultClouderaManagerRepoService.getDefault(REDHAT_7)).thenReturn(clouderaManagerRepo);
    }
}