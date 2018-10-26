package com.sequenceiq.cloudbreak.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.assertj.core.util.Lists;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import com.sequenceiq.cloudbreak.api.model.stack.StackImageChangeRequest;
import com.sequenceiq.cloudbreak.api.model.stack.cluster.ClusterRepairRequest;
import com.sequenceiq.cloudbreak.common.model.user.CloudbreakUser;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.workspace.User;
import com.sequenceiq.cloudbreak.domain.workspace.Workspace;
import com.sequenceiq.cloudbreak.service.CloudbreakRestRequestThreadLocalService;
import com.sequenceiq.cloudbreak.service.OperationRetryService;
import com.sequenceiq.cloudbreak.service.StackCommonService;
import com.sequenceiq.cloudbreak.service.cluster.ClusterService;
import com.sequenceiq.cloudbreak.service.image.ImageCatalogService;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.cloudbreak.service.user.UserService;
import com.sequenceiq.cloudbreak.service.workspace.WorkspaceService;

@RunWith(MockitoJUnitRunner.class)
public class StackV2ControllerTest {

    private static final Long WORKSPACE_ID = 1L;

    @InjectMocks
    private StackV2Controller underTest;

    @Mock
    private StackService stackService;

    @Mock
    private OperationRetryService operationRetryService;

    @Mock
    private ClusterService clusterService;

    @Mock
    private ImageCatalogService imageCatalogService;

    @Mock
    private WorkspaceService workspaceService;

    @Mock
    private StackCommonService stackCommonService;

    @Mock
    private CloudbreakRestRequestThreadLocalService restRequestThreadLocalService;

    @Mock
    private UserService userService;

    @Mock
    private CloudbreakUser cloudbreakUser;

    private Stack stack;

    @Before
    public void setUp() {

        String stackName = "stack-name";
        stack = new Stack();
        stack.setId(1L);
        stack.setName(stackName);
        Workspace workspace = new Workspace();
        workspace.setId(WORKSPACE_ID);
        workspace.setName("Top Sercet FBI");

        when(stackService.getByNameInWorkspace(eq(stackName), anyLong())).thenReturn(stack);
        when(restRequestThreadLocalService.getCloudbreakUser()).thenReturn(cloudbreakUser);
        when(userService.getOrCreate(any(CloudbreakUser.class))).thenReturn(new User());
        when(workspaceService.get(anyLong(), any(User.class))).thenReturn(workspace);
    }

    @Test
    public void retry() {
        underTest.retry(stack.getName());

        verify(stackCommonService, times(1)).retryInWorkspace(stack.getName(), WORKSPACE_ID);
    }

    @Test
    public void repairCluster() {
        ClusterRepairRequest clusterRepairRequest = new ClusterRepairRequest();
        List<String> hostGroups = Lists.newArrayList("master", "worker");
        clusterRepairRequest.setHostGroups(hostGroups);
        clusterRepairRequest.setRemoveOnly(true);

        underTest.repairCluster(stack.getName(), clusterRepairRequest);

        verify(clusterService, times(1)).repairCluster(eq(stack.getId()), eq(hostGroups), eq(true));
    }

    @Test
    public void testChangeImage() {
        StackImageChangeRequest stackImageChangeRequest = new StackImageChangeRequest();
        stackImageChangeRequest.setImageId("asdf");
        underTest.changeImage(stack.getName(), stackImageChangeRequest);
        verify(stackCommonService).changeImageByNameInWorkspace(eq(stack.getName()), eq(WORKSPACE_ID), eq(stackImageChangeRequest));
    }
}