package com.sequenceiq.it.cloudbreak.testcase.mock.cluster;

import java.math.BigDecimal;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.Test;

import com.cloudera.api.swagger.model.ApiCommand;
import com.sequenceiq.it.cloudbreak.action.v4.stack.StackDeleteAction;
import com.sequenceiq.it.cloudbreak.assertion.AssertStatusReasonMessage;
import com.sequenceiq.it.cloudbreak.client.StackTestClient;
import com.sequenceiq.it.cloudbreak.cloud.HostGroupType;
import com.sequenceiq.it.cloudbreak.context.Description;
import com.sequenceiq.it.cloudbreak.context.MockedTestContext;
import com.sequenceiq.it.cloudbreak.context.RunningParameter;
import com.sequenceiq.it.cloudbreak.dto.InstanceGroupTestDto;
import com.sequenceiq.it.cloudbreak.dto.stack.StackTestDto;
import com.sequenceiq.it.cloudbreak.mock.SetupCmScalingMock;
import com.sequenceiq.it.cloudbreak.mock.model.ClouderaManagerMock;
import com.sequenceiq.it.cloudbreak.testcase.AbstractIntegrationTest;

public class TerminationTest extends AbstractIntegrationTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(TerminationTest.class);

    @Inject
    private StackTestClient stackTestClient;

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK, enabled = false)
    @Description(
            given = "stack with 3 worker nodes",
            when = "terminate an instance by instance id",
            then = "remove that instance from stack")
    public void testInstanceTermination(MockedTestContext testContext) {
        SetupCmScalingMock mock = new SetupCmScalingMock();
        mock.configure(testContext, 3, 2, 2);
        testContext
                // create stack
                .given("ig", InstanceGroupTestDto.class).withHostGroup(HostGroupType.WORKER).withNodeCount(3)
                .given(StackTestDto.class).replaceInstanceGroups("ig")
                .when(stackTestClient.createV4(), RunningParameter.key("stack-post"))
                .await(STACK_AVAILABLE)
                //select an instance id
                .select(s -> s.getInstanceId(HostGroupType.WORKER.getName()), RunningParameter.key(StackDeleteAction.INSTANCE_ID))
                .capture(s -> s.getInstanceMetaData(HostGroupType.WORKER.getName()).size() - 1, RunningParameter.key("metadatasize"))
                .when(stackTestClient.deleteInstanceV4(), RunningParameter.key("stack-delete-instance"))
                .await(STACK_AVAILABLE)
                .verify(s -> s.getInstanceMetaData(HostGroupType.WORKER.getName()).size(), RunningParameter.key("metadatasize"))
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK, enabled = false)
    @Description(
            given = "stack with 2 worker nodes",
            when = "terminate an instance by instance id",
            then = "cannot remove that instance from stack because there is not enough nodes to dowsncale")
    public void testInstanceTerminationReplicationError(MockedTestContext testContext) {
        String overrideWithFailingDecomission = ClouderaManagerMock.API_ROOT + "/commands/666";
        testContext.getModel().getClouderaManagerMock().getDynamicRouteStack()
                .post(SetupCmScalingMock.HOSTS_DECOMMISSION, (request, response) -> new ApiCommand().id(new BigDecimal("666")));
        testContext.getModel().getClouderaManagerMock().getDynamicRouteStack()
                .get(overrideWithFailingDecomission, (request, response) -> new ApiCommand().success(false));
        testContext
                // create stack
                .given("ig", InstanceGroupTestDto.class).withHostGroup(HostGroupType.WORKER).withNodeCount(2)
                .given(StackTestDto.class).replaceInstanceGroups("ig")
                .when(stackTestClient.createV4(), RunningParameter.key("stack-post"))
                .await(STACK_AVAILABLE)
                .select(s -> s.getInstanceId("worker"), RunningParameter.key(StackDeleteAction.INSTANCE_ID))
                .when(stackTestClient.deleteInstanceV4(), RunningParameter.key("deleteInstance"))
                .await(STACK_AVAILABLE)
                .then(new AssertStatusReasonMessage<>("New node(s) could not be removed from the cluster. Reason Not Found"))
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK, enabled = false)
    @Description(
            given = "stack with 1 worker nodes",
            when = "terminate an instance by instance id with force",
            then = "remove that instance from stack")
    public void testInstanceTerminationForced(MockedTestContext testContext) {
        SetupCmScalingMock mock = new SetupCmScalingMock();
        mock.configure(testContext, 3, 2, 2);
        testContext
                // create stack
                .given(StackTestDto.class)
                .when(stackTestClient.createV4(), RunningParameter.key("stack-post"))
                .await(STACK_AVAILABLE)
                .select(s -> s.getInstanceId("worker"), RunningParameter.key(StackDeleteAction.INSTANCE_ID))
                .select(s -> true, RunningParameter.key("forced"))
                .capture(s -> s.getInstanceMetaData("worker").size() - 1, RunningParameter.key("metadatasize"))
                .when(stackTestClient.deleteInstanceV4(), RunningParameter.key("deleteInstance"))
                .await(STACK_AVAILABLE)
                .verify(s -> s.getInstanceMetaData("worker").size(), RunningParameter.key("metadatasize"))
                .validate();
    }
}
