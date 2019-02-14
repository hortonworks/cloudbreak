package com.sequenceiq.cloudbreak.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.spy;
import static org.powermock.api.mockito.PowerMockito.when;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.common.collect.Sets;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceGroupType;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.ClusterV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.util.responses.StackMatrixV4Response;
import com.sequenceiq.cloudbreak.api.util.ConverterUtil;
import com.sequenceiq.cloudbreak.clusterdefinition.utils.AmbariBlueprintUtils;
import com.sequenceiq.cloudbreak.cloud.model.AmbariRepo;
import com.sequenceiq.cloudbreak.cloud.model.Image;
import com.sequenceiq.cloudbreak.cloud.model.component.DefaultHDPEntries;
import com.sequenceiq.cloudbreak.cloud.model.component.DefaultHDPInfo;
import com.sequenceiq.cloudbreak.cloud.model.component.DefaultStackRepoDetails;
import com.sequenceiq.cloudbreak.cloud.model.component.StackRepoDetails;
import com.sequenceiq.cloudbreak.common.type.ComponentType;
import com.sequenceiq.cloudbreak.controller.exception.BadRequestException;
import com.sequenceiq.cloudbreak.core.CloudbreakImageNotFoundException;
import com.sequenceiq.cloudbreak.domain.ClusterDefinition;
import com.sequenceiq.cloudbreak.domain.KerberosConfig;
import com.sequenceiq.cloudbreak.domain.json.Json;
import com.sequenceiq.cloudbreak.domain.stack.Component;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.domain.workspace.User;
import com.sequenceiq.cloudbreak.domain.workspace.Workspace;
import com.sequenceiq.cloudbreak.service.clusterdefinition.ClusterDefinitionService;
import com.sequenceiq.cloudbreak.service.cluster.ClusterService;
import com.sequenceiq.cloudbreak.service.cluster.ambari.AmbariRepositoryVersionService;
import com.sequenceiq.cloudbreak.service.decorator.ClusterDecorator;

public class ClusterCreationSetupServiceTest {

    @Mock
    private ConverterUtil converterUtil;

    @Mock
    private ClusterDecorator clusterDecorator;

    @Mock
    private ComponentConfigProvider componentConfigProvider;

    @Mock
    private AmbariBlueprintUtils ambariBlueprintUtils;

    @Mock
    private ClusterDefinitionService clusterDefinitionService;

    @Mock
    private DefaultHDPEntries defaultHDPEntries;

    @Mock
    private StackMatrixService stackMatrixService;

    @Mock
    private AmbariRepositoryVersionService ambariRepositoryVersionService;

    @Mock
    private ClusterService clusterService;

    @InjectMocks
    private ClusterCreationSetupService underTest;

    private ClusterV4Request clusterRequest;

    private Stack stack;

    private ClusterDefinition clusterDefinition;

    private User user;

    private Workspace workspace;

    private Cluster cluster;

    @Before
    public void init() throws CloudbreakImageNotFoundException, JsonProcessingException {
        MockitoAnnotations.initMocks(this);
        workspace = new Workspace();
        clusterRequest = new ClusterV4Request();
        stack = new Stack();
        stack.setId(1L);
        stack.setWorkspace(workspace);
        clusterDefinition = new ClusterDefinition();
        clusterDefinition.setClusterDefinitionText("{}");
        user = new User();
        Map<InstanceGroupType, String> userData = new HashMap<>();
        userData.put(InstanceGroupType.CORE, "asf");
        Image image = new Image("asdf", userData, "centos7", "uuid", "url", "imgcatname",
                "id", Collections.emptyMap());
        Component component = spy(new Component(ComponentType.AMBARI_REPO_DETAILS, ComponentType.AMBARI_REPO_DETAILS.name(), new Json(new AmbariRepo()), stack));
        Component imageComponent = new Component(ComponentType.IMAGE, ComponentType.IMAGE.name(), new Json(image), stack);

        cluster = new Cluster();
        KerberosConfig kerberosConfig = new KerberosConfig();
        kerberosConfig.setDomain("domain");
        cluster.setKerberosConfig(kerberosConfig);
        when(clusterDecorator.decorate(any(), any(), any(), any(), any(), any())).thenReturn(cluster);
        when(componentConfigProvider.getAllComponentsByStackIdAndType(any(), any())).thenReturn(Sets.newHashSet(component, imageComponent));
        String version = "3.0";
        when(ambariBlueprintUtils.getBlueprintStackVersion(any())).thenReturn(version);
        when(ambariBlueprintUtils.getBlueprintStackName(any())).thenReturn("HDP");
        DefaultHDPInfo defaultHDPInfo = new DefaultHDPInfo();
        DefaultStackRepoDetails stackRepoDetails = new DefaultStackRepoDetails();
        stackRepoDetails.setHdpVersion(version);
        Map<String, String> stackRepo = new HashMap<>();
        stackRepo.put("centos7", "http://centos7-repo/" + version);
        stackRepo.put("centos6", "http://centos6-repo/" + version);
        stackRepo.put(StackRepoDetails.REPO_ID_TAG, "HDP");
        stackRepoDetails.setStack(stackRepo);
        defaultHDPInfo.setRepo(stackRepoDetails);
        defaultHDPInfo.setVersion(version);
        when(defaultHDPEntries.getEntries()).thenReturn(Collections.singletonMap(version, defaultHDPInfo));
        when(componentConfigProvider.getImage(anyLong())).thenReturn(image);
        StackMatrixV4Response stackMatrixV4Response = new StackMatrixV4Response();
        stackMatrixV4Response.setHdp(Collections.singletonMap(version, null));
        when(stackMatrixService.getStackMatrix()).thenReturn(stackMatrixV4Response);
        when(ambariRepositoryVersionService.isVersionNewerOrEqualThanLimited(any(), any())).thenReturn(false);
        when(clusterService.save(any(Cluster.class))).thenReturn(cluster);

        stack.setCluster(cluster);
    }

    @Test
    public void testDomainIsSet() throws CloudbreakImageNotFoundException, IOException, TransactionService.TransactionExecutionException {
        underTest.prepare(clusterRequest, stack, clusterDefinition, user, workspace);

        assertEquals(cluster.getKerberosConfig().getDomain(), stack.getCustomDomain());
    }

    @Test
    public void testMissingKerberosConfig() throws CloudbreakImageNotFoundException, IOException, TransactionService.TransactionExecutionException {
        cluster.setKerberosConfig(null);

        underTest.prepare(clusterRequest, stack, clusterDefinition, user, workspace);

        assertNull(stack.getCustomDomain());
    }

    @Test
    public void testMissingDomain() throws CloudbreakImageNotFoundException, IOException, TransactionService.TransactionExecutionException {
        cluster.getKerberosConfig().setDomain(null);

        underTest.prepare(clusterRequest, stack, clusterDefinition, user, workspace);

        assertNull(stack.getCustomDomain());
    }

    @Test
    public void testIncorrectKerberosSettingForWorkloadCluster() {
        stack.setDatalakeResourceId(1L);
        clusterRequest.setKerberosName("attached_kerberos_which_not_allowed");

        assertThrows(BadRequestException.class, () -> underTest.validate(clusterRequest, stack, user, workspace));
    }
}