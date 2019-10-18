package com.sequenceiq.cloudbreak.service.template;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.Optional;

import org.junit.Assert;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.ResourceStatus;
import com.sequenceiq.cloudbreak.domain.Blueprint;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.domain.stack.cluster.ClusterTemplate;
import com.sequenceiq.cloudbreak.exception.BadRequestException;
import com.sequenceiq.cloudbreak.repository.cluster.ClusterTemplateRepository;
import com.sequenceiq.cloudbreak.service.RestRequestThreadLocalService;
import com.sequenceiq.cloudbreak.service.blueprint.BlueprintService;
import com.sequenceiq.cloudbreak.service.user.UserService;
import com.sequenceiq.cloudbreak.service.workspace.WorkspaceService;
import com.sequenceiq.cloudbreak.workspace.model.Workspace;

class ClusterTemplateServiceCreationValidationTest {

    private static final long WORKSPACE_ID = 1L;

    private static final String ACCOUNT_ID = "someAccountId";

    @Mock
    private ClusterTemplateRepository clusterTemplateRepository;

    @Mock
    private RestRequestThreadLocalService restRequestThreadLocalService;

    @Mock
    private UserService userService;

    @Mock
    private WorkspaceService workspaceService;

    @Mock
    private BlueprintService blueprintService;

    @InjectMocks
    private ClusterTemplateService underTest;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    void testWhenClusterTemplateDoesNotContainAClusterBadRequestExceptionComes() {
        Stack stack = new Stack();
        stack.setEnvironmentCrn("someCrn");
        ClusterTemplate clusterTemplate = new ClusterTemplate();
        clusterTemplate.setStackTemplate(stack);
        clusterTemplate.setStatus(ResourceStatus.USER_MANAGED);
        when(clusterTemplateRepository.findByNameAndWorkspace(any(), any())).thenReturn(Optional.empty());

        Exception e = Assertions.assertThrows(BadRequestException.class, () -> underTest.createForLoggedInUser(clusterTemplate, WORKSPACE_ID, ACCOUNT_ID));
        Assert.assertEquals("Stack template in cluster definition should contain a – valid – cluster request!", e.getMessage());
    }

    @Test
    void testWhenClusterTemplateDoesNotContainBlueprintBadRequestExceptionComes() {
        Cluster cluster = new Cluster();
        Stack stack = new Stack();
        stack.setEnvironmentCrn("someCrn");
        stack.setCluster(cluster);
        ClusterTemplate clusterTemplate = new ClusterTemplate();
        clusterTemplate.setStackTemplate(stack);
        clusterTemplate.setStatus(ResourceStatus.DEFAULT);
        when(clusterTemplateRepository.findByNameAndWorkspace(any(), any())).thenReturn(Optional.empty());

        Exception e = Assertions.assertThrows(BadRequestException.class, () -> underTest.createForLoggedInUser(clusterTemplate, WORKSPACE_ID, ACCOUNT_ID));
        Assert.assertEquals("Cluster definition should contain a cluster template!", e.getMessage());
    }

    @Test
    void testWhenClusterTemplateDoesNotContainAnExistingBlueprintBadRequestExceptionComes() {
        Workspace workspace = new Workspace();
        Blueprint blueprint = new Blueprint();
        Cluster cluster = new Cluster();
        cluster.setBlueprint(blueprint);
        Stack stack = new Stack();
        stack.setEnvironmentCrn("someCrn");
        stack.setCluster(cluster);
        ClusterTemplate clusterTemplate = new ClusterTemplate();
        clusterTemplate.setStackTemplate(stack);
        clusterTemplate.setStatus(ResourceStatus.DEFAULT);
        clusterTemplate.setWorkspace(workspace);
        when(clusterTemplateRepository.findByNameAndWorkspace(any(), any())).thenReturn(Optional.empty());
        when(blueprintService.getAllAvailableInWorkspace(workspace)).thenReturn(Collections.emptySet());

        Exception e = Assertions.assertThrows(BadRequestException.class, () -> underTest.createForLoggedInUser(clusterTemplate, WORKSPACE_ID, ACCOUNT_ID));
        Assert.assertEquals("The cluster template (aka blueprint) in the cluster definition should be an existing one!", e.getMessage());
    }

}