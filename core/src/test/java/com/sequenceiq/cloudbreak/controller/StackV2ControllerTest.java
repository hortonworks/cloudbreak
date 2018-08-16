package com.sequenceiq.cloudbreak.controller;

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
import com.sequenceiq.cloudbreak.domain.organization.Organization;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.service.OperationRetryService;
import com.sequenceiq.cloudbreak.service.StackCommonService;
import com.sequenceiq.cloudbreak.service.cluster.ClusterService;
import com.sequenceiq.cloudbreak.service.image.ImageCatalogService;
import com.sequenceiq.cloudbreak.service.organization.OrganizationService;
import com.sequenceiq.cloudbreak.service.stack.StackService;

@RunWith(MockitoJUnitRunner.class)
public class StackV2ControllerTest {

    private static final Long ORGANIZATION_ID = 1L;

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
    private OrganizationService organizationService;

    @Mock
    private StackCommonService stackCommonService;

    private Stack stack;

    @Before
    public void setUp() {

        String stackName = "stack-name";
        stack = new Stack();
        stack.setId(1L);
        stack.setName(stackName);
        Organization organization = new Organization();
        organization.setId(ORGANIZATION_ID);
        organization.setName("Top Sercet FBI");

        when(stackService.getByNameInDefaultOrg(eq(stackName))).thenReturn(stack);
        when(organizationService.getDefaultOrganizationForCurrentUser()).thenReturn(organization);
    }

    @Test
    public void retry() {
        underTest.retry(stack.getName());

        verify(stackCommonService, times(1)).retryInOrganization(stack.getName(), ORGANIZATION_ID);
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
        verify(stackCommonService).changeImageByNameInOrg(eq(stack.getName()), eq(ORGANIZATION_ID), eq(stackImageChangeRequest));
    }
}