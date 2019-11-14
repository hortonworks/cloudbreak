package com.sequenceiq.it.cloudbreak.testcase.mock;

import javax.inject.Inject;

import org.testng.annotations.Test;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceStatus;
import com.sequenceiq.it.cloudbreak.client.StackTestClient;
import com.sequenceiq.it.cloudbreak.cloud.HostGroupType;
import com.sequenceiq.it.cloudbreak.context.Description;
import com.sequenceiq.it.cloudbreak.context.MockedTestContext;
import com.sequenceiq.it.cloudbreak.context.RunningParameter;
import com.sequenceiq.it.cloudbreak.dto.InstanceGroupTestDto;
import com.sequenceiq.it.cloudbreak.dto.stack.StackTestDto;
import com.sequenceiq.it.cloudbreak.testcase.AbstractIntegrationTest;
import com.sequenceiq.it.cloudbreak.spark.StatefulRoute;

public class RecoveryItTest extends AbstractIntegrationTest {

    private static final String HOSTS = "/api/v1/hosts";

    @Inject
    private StackTestClient stackTestClient;

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK, enabled = false)
    @Description(
            given = "a created cluster",
            when = "calling test action which says there is an unhealthy node",
            then = "cluster status should be CREATE_FAILED")
    public void testWhenSyncTellsNodesAreUnhealthyThenClusterStatusHaveToChange(MockedTestContext testContext) {
        String stackName = resourcePropertyProvider().getName();
        String workerId = resourcePropertyProvider().getName();

        mockAmbari(testContext);
        testContext
                .given(workerId, InstanceGroupTestDto.class)
                .withHostGroup(HostGroupType.WORKER)
                .withNodeCount(1)
                .given(stackName, StackTestDto.class)
                .withName(stackName)
                .replaceInstanceGroups(workerId)
                .when(stackTestClient.createV4(), RunningParameter.key(stackName))
                .await(STACK_AVAILABLE, RunningParameter.key(stackName))
                .when(stackTestClient.syncV4(), RunningParameter.key(stackName))
                .await(STACK_FAILED, RunningParameter.key(stackName))
                .validate();
    }

    private void mockAmbari(MockedTestContext testContext) {
        testContext.getModel().getAmbariMock().getDynamicRouteStack().clearPost(HOSTS);
        modifyStatusResponses(testContext, InstanceStatus.SERVICES_UNHEALTHY, 2);
        modifyStatusResponses(testContext, InstanceStatus.SERVICES_HEALTHY, 1);
    }

    private void modifyStatusResponses(MockedTestContext testContext, InstanceStatus state, int quantity) {
        for (int i = 0; i < quantity; i++) {
            testContext.getModel().getAmbariMock().getDynamicRouteStack().post(HOSTS, createHostResponseForAmbariWithStatus(state));
        }
    }

    private StatefulRoute createHostResponseForAmbariWithStatus(InstanceStatus overridedStatus) {
        return (request, response, model) -> {
            response.type("text/plain");
            response.status(200);
            return "";
        };
    }

}