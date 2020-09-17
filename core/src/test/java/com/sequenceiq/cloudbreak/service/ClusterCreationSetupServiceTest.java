package com.sequenceiq.cloudbreak.service;

import static com.sequenceiq.cloudbreak.RepoTestUtil.getDefaultCDHInfo;
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
import com.sequenceiq.cloudbreak.cloud.model.Image;
import com.sequenceiq.cloudbreak.cloud.model.component.DefaultCDHInfo;
import com.sequenceiq.cloudbreak.cloud.model.component.ImageBasedDefaultCDHEntries;
import com.sequenceiq.cloudbreak.cloud.model.component.ImageBasedDefaultCDHInfo;
import com.sequenceiq.cloudbreak.cmtemplate.utils.BlueprintUtils;
import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.cloudbreak.common.service.TransactionService;
import com.sequenceiq.cloudbreak.common.type.ComponentType;
import com.sequenceiq.cloudbreak.core.CloudbreakImageCatalogException;
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

    private static final String PLATFORM = "aws";

    private static final String IMAGE_CATALOG_NAME = "image catalog name";

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
    private StackMatrixService stackMatrixService;

    @Mock
    private ClusterOperationService clusterOperationService;

    @Mock
    private DefaultClouderaManagerRepoService defaultClouderaManagerRepoService;

    @Mock
    private KerberosConfigService kerberosConfigService;

    @Mock
    private ImageBasedDefaultCDHEntries imageBasedDefaultCDHEntries;

    @InjectMocks
    private ClusterCreationSetupService underTest;

    private ClusterV4Request clusterRequest;

    private Stack stack;

    private Blueprint blueprint;

    private User user;

    private Workspace workspace;

    private Cluster cluster;

    @Before
    public void init() throws CloudbreakImageNotFoundException, CloudbreakImageCatalogException, IOException {
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

        DefaultCDHInfo defaultCDHInfo = getDefaultCDHInfo(CDH_VERSION);

        when(imageBasedDefaultCDHEntries.getEntries(workspace.getId(), PLATFORM, IMAGE_CATALOG_NAME)).thenReturn(Collections.singletonMap(CDH_VERSION,
                new ImageBasedDefaultCDHInfo(defaultCDHInfo, Mockito.mock(com.sequenceiq.cloudbreak.cloud.model.catalog.Image.class))));
        when(componentConfigProviderService.getImage(anyLong())).thenReturn(image);
        StackMatrixV4Response stackMatrixV4Response = new StackMatrixV4Response();
        stackMatrixV4Response.setCdh(Collections.singletonMap(CDH_VERSION, null));
        when(stackMatrixService.getStackMatrix(workspace.getId(), PLATFORM, IMAGE_CATALOG_NAME)).thenReturn(stackMatrixV4Response);
        when(clouderaManagerClusterCreationSetupService.prepareClouderaManagerCluster(any(), any(), any(), any(), any())).
                thenReturn(new ArrayList<>());
    }

    @Test
    public void testMissingKerberosConfig() throws CloudbreakImageCatalogException, IOException, TransactionService.TransactionExecutionException {
        underTest.prepare(clusterRequest, stack, blueprint, user, null);
        assertNull(stack.getCustomDomain());
    }

    @Test
    public void testMissingDomain() throws CloudbreakImageCatalogException, IOException, TransactionService.TransactionExecutionException {
        KerberosConfig kerberosConfig = KerberosConfig.KerberosConfigBuilder.aKerberosConfig().build();
        when(kerberosConfigService.get(anyString(), anyString())).thenReturn(Optional.of(kerberosConfig));
        underTest.prepare(clusterRequest, stack, blueprint, user, null);
        assertNull(stack.getCustomDomain());
    }
}