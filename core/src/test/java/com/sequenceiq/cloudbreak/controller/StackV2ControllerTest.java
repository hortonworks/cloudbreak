package com.sequenceiq.cloudbreak.controller;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Collections;
import java.util.Date;
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
import com.sequenceiq.cloudbreak.common.model.user.IdentityUser;
import com.sequenceiq.cloudbreak.domain.ImageCatalog;
import com.sequenceiq.cloudbreak.domain.organization.Organization;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.service.AuthenticatedUserService;
import com.sequenceiq.cloudbreak.service.OperationRetryService;
import com.sequenceiq.cloudbreak.service.cluster.ClusterService;
import com.sequenceiq.cloudbreak.service.image.ImageCatalogService;
import com.sequenceiq.cloudbreak.service.organization.OrganizationService;
import com.sequenceiq.cloudbreak.service.stack.StackService;

@RunWith(MockitoJUnitRunner.class)
public class StackV2ControllerTest {

    private static final long ORG_ID = 100L;

    @InjectMocks
    private StackV2Controller underTest;

    @Mock
    private AuthenticatedUserService authenticatedUserService;

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

    private IdentityUser identityUser;

    private Stack stack;

    @Before
    public void setUp() {
        identityUser = new IdentityUser("userId", "username", "account", Collections.emptyList(),
                "givenName", "familyName", Date.from(Instant.now()));
        when(authenticatedUserService.getCbUser()).thenReturn(identityUser);

        String stackName = "stack-name";
        stack = new Stack();
        stack.setId(1L);
        stack.setName(stackName);

        when(stackService.getPublicStack(eq(stackName), eq(identityUser))).thenReturn(stack);
    }

    @Test
    public void retry() {
        underTest.retry(stack.getName());

        verify(operationRetryService, times(1)).retry(stack);
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

        verify(stackService).updateImage(eq(stack.getId()), eq(stackImageChangeRequest.getImageId()), eq(null), eq(null));
    }

    @Test
    public void testChangeImageWithImageCatalog() {
        ImageCatalog imageCatalog = new ImageCatalog();
        imageCatalog.setName("hgfjfg");
        imageCatalog.setImageCatalogUrl("url");
        Organization organization = new Organization();
        organization.setId(ORG_ID);
        when(organizationService.getDefaultOrganizationForCurrentUser()).thenReturn(organization);
        when(imageCatalogService.get(eq(ORG_ID), eq(imageCatalog.getName()))).thenReturn(imageCatalog);

        StackImageChangeRequest stackImageChangeRequest = new StackImageChangeRequest();
        stackImageChangeRequest.setImageId("asdf");
        stackImageChangeRequest.setImageCatalogName(imageCatalog.getName());
        underTest.changeImage(stack.getName(), stackImageChangeRequest);

        verify(stackService).updateImage(eq(stack.getId()), eq(stackImageChangeRequest.getImageId()), eq(imageCatalog.getName()),
                eq(imageCatalog.getImageCatalogUrl()));
    }
}