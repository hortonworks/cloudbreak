package com.sequenceiq.cloudbreak.service;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.google.common.collect.Sets;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.ClusterV4Request;
import com.sequenceiq.cloudbreak.cloud.model.Image;
import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.cloudbreak.common.service.TransactionService;
import com.sequenceiq.cloudbreak.common.type.ComponentType;
import com.sequenceiq.cloudbreak.controller.validation.environment.ClusterCreationEnvironmentValidator;
import com.sequenceiq.cloudbreak.controller.validation.rds.RdsConfigValidator;
import com.sequenceiq.cloudbreak.converter.v4.stacks.cluster.CloudStorageConverter;
import com.sequenceiq.cloudbreak.core.CloudbreakImageCatalogException;
import com.sequenceiq.cloudbreak.core.CloudbreakImageNotFoundException;
import com.sequenceiq.cloudbreak.domain.Blueprint;
import com.sequenceiq.cloudbreak.domain.stack.Component;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.service.cluster.flow.ClusterOperationService;
import com.sequenceiq.cloudbreak.service.decorator.ClusterDecorator;
import com.sequenceiq.cloudbreak.service.filesystem.FileSystemConfigService;
import com.sequenceiq.cloudbreak.service.stack.CentralCDHVersionCoordinator;
import com.sequenceiq.cloudbreak.workspace.model.User;
import com.sequenceiq.cloudbreak.workspace.model.Workspace;
import com.sequenceiq.common.api.type.InstanceGroupType;

@ExtendWith(MockitoExtension.class)
class ClusterCreationSetupServiceTest {

    public static final String REDHAT_7 = "redhat7";

    @Mock
    private ClouderaManagerClusterCreationSetupService clouderaManagerClusterCreationSetupService;

    @Mock
    private FileSystemConfigService fileSystemConfigService;

    @Mock
    private ClusterDecorator clusterDecorator;

    @Mock
    private ClusterOperationService clusterOperationService;

    @Mock
    private ComponentConfigProviderService componentConfigProviderService;

    @Mock
    private RdsConfigValidator rdsConfigValidator;

    @Mock
    private ClusterCreationEnvironmentValidator environmentValidator;

    @Mock
    private CloudStorageConverter cloudStorageConverter;

    @Mock
    private CentralCDHVersionCoordinator centralCDHVersionCoordinator;

    @InjectMocks
    private ClusterCreationSetupService underTest;

    private ClusterV4Request clusterRequest;

    private Stack stack;

    private Blueprint blueprint;

    private User user;

    @BeforeEach
    void init() throws CloudbreakImageNotFoundException, CloudbreakImageCatalogException, IOException {
        Workspace workspace = new Workspace();
        clusterRequest = new ClusterV4Request();
        stack = new Stack();
        stack.setId(1L);
        stack.setWorkspace(workspace);
        stack.setEnvironmentCrn("env");
        stack.setName("name");
        blueprint = new Blueprint();
        blueprint.setBlueprintText("{}");
        user = new User();
        Image image = Image.builder()
                .withImageName("imagename")
                .withUserdata(Map.of(InstanceGroupType.CORE, "userdata"))
                .withOs("centos7")
                .withOsType(REDHAT_7)
                .withImageCatalogUrl("url")
                .withImageCatalogName("imgcatname")
                .withImageId("id")
                .build();
        Component imageComponent = new Component(ComponentType.IMAGE, ComponentType.IMAGE.name(), new Json(image), stack);
        Component stackCmRepoConfig = new Component(ComponentType.CM_REPO_DETAILS, ComponentType.CM_REPO_DETAILS.name(), new Json(""), stack);

        Cluster cluster = new Cluster();
        stack.setCluster(cluster);
        when(clusterDecorator.decorate(any(), any(), any(), any(), any(), any())).thenReturn(cluster);
        when(componentConfigProviderService.getAllComponentsByStackIdAndType(any(), any())).thenReturn(Sets.newHashSet(imageComponent, stackCmRepoConfig));

        lenient().when(componentConfigProviderService.getImage(anyLong())).thenReturn(image);
        when(clouderaManagerClusterCreationSetupService.prepareClouderaManagerCluster(any(), any(), any(), any(), any()))
                .thenReturn(new ArrayList<>());
    }

    @Test
    void testMissingDomain() throws CloudbreakImageCatalogException, IOException, TransactionService.TransactionExecutionException {
        when(centralCDHVersionCoordinator.isCdhProductDetails(any(Component.class))).thenReturn(true);
        underTest.prepare(clusterRequest, stack, blueprint, user);
        assertNull(stack.getCustomDomain());
    }
}
