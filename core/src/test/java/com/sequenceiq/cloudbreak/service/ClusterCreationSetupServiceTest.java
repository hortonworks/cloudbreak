package com.sequenceiq.cloudbreak.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
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
import org.springframework.core.convert.ConversionService;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.common.collect.Sets;
import com.sequenceiq.cloudbreak.api.model.stack.StackMatrix;
import com.sequenceiq.cloudbreak.api.model.stack.cluster.ClusterRequest;
import com.sequenceiq.cloudbreak.api.model.stack.instance.InstanceGroupType;
import com.sequenceiq.cloudbreak.blueprint.utils.BlueprintUtils;
import com.sequenceiq.cloudbreak.cloud.model.AmbariRepo;
import com.sequenceiq.cloudbreak.cloud.model.Image;
import com.sequenceiq.cloudbreak.cloud.model.component.DefaultHDPEntries;
import com.sequenceiq.cloudbreak.cloud.model.component.DefaultHDPInfo;
import com.sequenceiq.cloudbreak.cloud.model.component.DefaultStackRepoDetails;
import com.sequenceiq.cloudbreak.cloud.model.component.StackRepoDetails;
import com.sequenceiq.cloudbreak.common.type.ComponentType;
import com.sequenceiq.cloudbreak.controller.exception.BadRequestException;
import com.sequenceiq.cloudbreak.core.CloudbreakImageNotFoundException;
import com.sequenceiq.cloudbreak.domain.Blueprint;
import com.sequenceiq.cloudbreak.domain.KerberosConfig;
import com.sequenceiq.cloudbreak.domain.json.Json;
import com.sequenceiq.cloudbreak.domain.stack.Component;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.domain.workspace.User;
import com.sequenceiq.cloudbreak.domain.workspace.Workspace;
import com.sequenceiq.cloudbreak.service.blueprint.BlueprintService;
import com.sequenceiq.cloudbreak.service.cluster.ClusterService;
import com.sequenceiq.cloudbreak.service.cluster.ambari.AmbariRepositoryVersionService;
import com.sequenceiq.cloudbreak.service.decorator.ClusterDecorator;

public class ClusterCreationSetupServiceTest {

    @Mock
    private ConversionService conversionService;

    @Mock
    private ClusterDecorator clusterDecorator;

    @Mock
    private ComponentConfigProvider componentConfigProvider;

    @Mock
    private BlueprintUtils blueprintUtils;

    @Mock
    private BlueprintService blueprintService;

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

    private ClusterRequest clusterRequest;

    private Stack stack;

    private Blueprint blueprint;

    private User user;

    private Workspace workspace;

    private Cluster cluster;

    @Before
    public void init() throws CloudbreakImageNotFoundException, JsonProcessingException {
        MockitoAnnotations.initMocks(this);
        workspace = new Workspace();
        clusterRequest = new ClusterRequest();
        stack = new Stack();
        stack.setId(1L);
        stack.setWorkspace(workspace);
        blueprint = new Blueprint();
        blueprint.setBlueprintText("{}");
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
        when(conversionService.convert(any(ClusterRequest.class), eq(Cluster.class))).thenReturn(cluster);
        when(clusterDecorator
                .decorate(any(), any(), any(), any(), any(), any())).thenReturn(cluster);
        when(componentConfigProvider.getAllComponentsByStackIdAndType(any(), any())).thenReturn(Sets.newHashSet(component, imageComponent));
        String version = "3.0";
        when(blueprintUtils.getBlueprintStackVersion(any())).thenReturn(version);
        when(blueprintUtils.getBlueprintStackName(any())).thenReturn("HDP");
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
        StackMatrix stackMatrix = new StackMatrix();
        stackMatrix.setHdp(Collections.singletonMap(version, null));
        when(stackMatrixService.getStackMatrix()).thenReturn(stackMatrix);
        when(ambariRepositoryVersionService.isVersionNewerOrEqualThanLimited(any(), any())).thenReturn(false);
        when(clusterService.save(any(Cluster.class))).thenReturn(cluster);
    }

    @Test
    public void testDomainIsSet() throws CloudbreakImageNotFoundException, IOException, TransactionService.TransactionExecutionException {
        underTest.prepare(clusterRequest, stack, blueprint, user, workspace);

        assertEquals(cluster.getKerberosConfig().getDomain(), stack.getCustomDomain());
    }

    @Test
    public void testMissingKerberosConfig() throws CloudbreakImageNotFoundException, IOException, TransactionService.TransactionExecutionException {
        cluster.setKerberosConfig(null);

        underTest.prepare(clusterRequest, stack, blueprint, user, workspace);

        assertNull(stack.getCustomDomain());
    }

    @Test
    public void testMissingDomain() throws CloudbreakImageNotFoundException, IOException, TransactionService.TransactionExecutionException {
        cluster.getKerberosConfig().setDomain(null);

        underTest.prepare(clusterRequest, stack, blueprint, user, workspace);

        assertNull(stack.getCustomDomain());
    }

    @Test
    public void testIncorrectKerberosSettingForWorkloadCluster() {
        stack.setDatalakeId(1L);
        clusterRequest.setKerberosConfigName("attached_kerberos_which_not_allowed");

        assertThrows(BadRequestException.class, () -> underTest.validate(clusterRequest, stack, user, workspace));
    }
}