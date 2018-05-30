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
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import com.sequenceiq.cloudbreak.api.model.stack.cluster.ClusterRepairRequest;
import com.sequenceiq.cloudbreak.common.model.user.IdentityUser;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.service.AuthenticatedUserService;
import com.sequenceiq.cloudbreak.service.OperationRetryService;
import com.sequenceiq.cloudbreak.service.cluster.ClusterService;
import com.sequenceiq.cloudbreak.service.stack.StackService;

@RunWith(MockitoJUnitRunner.class)
public class StackV2ControllerTest {

    private static final String STACK_NAME = "stackName";

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

    @Test
    public void retry() {
        IdentityUser identityUser = new IdentityUser("userId", "username", "account", Collections.emptyList(),
                "givenName", "familyName", Date.from(Instant.now()));
        when(authenticatedUserService.getCbUser()).thenReturn(identityUser);

        Stack stack = new Stack();
        when(stackService.getPublicStack(STACK_NAME, identityUser)).thenReturn(stack);

        underTest.retry(STACK_NAME);

        verify(operationRetryService, times(1)).retry(stack);
    }

    @Test
    public void repairCluster() {
        String stackName = "stack-name";
        IdentityUser identityUser = new IdentityUser("id", "username", "account", Lists.emptyList(), "given", "family", new Date());

        when(authenticatedUserService.getCbUser()).thenReturn(identityUser);

        Stack stack = new Stack();

        Long stackId = 1L;
        stack.setId(stackId);
        when(stackService.getPublicStack(eq(stackName), eq(identityUser))).thenReturn(stack);

        ClusterRepairRequest clusterRepairRequest = new ClusterRepairRequest();
        List<String> hostGroups = Lists.newArrayList("master", "worker");
        clusterRepairRequest.setHostGroups(hostGroups);
        clusterRepairRequest.setRemoveOnly(true);

        underTest.repairCluster(stackName, clusterRepairRequest);

        verify(clusterService, times(1)).repairCluster(eq(stackId), eq(hostGroups), eq(true));
    }
}