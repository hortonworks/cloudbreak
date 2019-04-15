package com.sequenceiq.it.cloudbreak.newway.testcase.mock;

import static com.sequenceiq.it.cloudbreak.newway.context.RunningParameter.key;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.Test;

import com.sequenceiq.it.cloudbreak.newway.action.v4.stack.StackDeleteAction;
import com.sequenceiq.it.cloudbreak.newway.assertion.AssertStatusReasonMessage;
import com.sequenceiq.it.cloudbreak.newway.client.StackTestClient;
import com.sequenceiq.it.cloudbreak.newway.cloud.HostGroupType;
import com.sequenceiq.it.cloudbreak.newway.context.Description;
import com.sequenceiq.it.cloudbreak.newway.context.TestContext;
import com.sequenceiq.it.cloudbreak.newway.dto.InstanceGroupTestDto;
import com.sequenceiq.it.cloudbreak.newway.dto.stack.StackTestDto;
import com.sequenceiq.it.cloudbreak.newway.testcase.AbstractIntegrationTest;

public class TerminationTest extends AbstractIntegrationTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(TerminationTest.class);

    @Inject
    private StackTestClient stackTestClient;

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    @Description(
            given = "stack with 3 worker nodes",
            when = "terminate an instance by instance id",
            then = "remove that instance from stack")
    public void testInstanceTermination(TestContext testContext) {
        testContext
                // create stack
                .given("ig", InstanceGroupTestDto.class).withHostGroup(HostGroupType.WORKER).withNodeCount(3)
                .given(StackTestDto.class).replaceInstanceGroups("ig")
                .when(stackTestClient.createV4(), key("stack-post"))
                .await(STACK_AVAILABLE)
                //select an instance id
                .select(s -> s.getInstanceId(HostGroupType.WORKER.getName()), key(StackDeleteAction.INSTANCE_ID))
                .capture(s -> s.getInstanceMetaData(HostGroupType.WORKER.getName()).size() - 1, key("metadatasize"))
                .when(stackTestClient.deleteInstanceV4(), key("stack-delete-instance"))
                .await(STACK_AVAILABLE)
                .verify(s -> s.getInstanceMetaData(HostGroupType.WORKER.getName()).size(), key("metadatasize"))
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    @Description(
            given = "stack with 2 worker nodes",
            when = "terminate an instance by instance id",
            then = "cannot remove that instance from stack because there is not enough nodes to dowsncale")
    public void testInstanceTerminationReplicationError(TestContext testContext) {
        testContext
                // create stack
                .given("ig", InstanceGroupTestDto.class).withHostGroup(HostGroupType.WORKER).withNodeCount(2)
                .given(StackTestDto.class).replaceInstanceGroups("ig")
                .when(stackTestClient.createV4(), key("stack-post"))
                .await(STACK_AVAILABLE)
                .select(s -> s.getInstanceId("worker"), key(StackDeleteAction.INSTANCE_ID))
                .when(stackTestClient.deleteInstanceV4(), key("deleteInstance"))
                .await(STACK_AVAILABLE)
                .then(new AssertStatusReasonMessage<>("Node(s) could not be removed from the cluster: There is not enough node to downscale. "
                        + "Check the replication factor and the ApplicationMaster occupation."))
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    @Description(
            given = "stack with 1 worker nodes",
            when = "terminate an instance by instance id with force",
            then = "remove that instance from stack")
    public void testInstanceTerminationForced(TestContext testContext) {
        testContext
                // create stack
                .given(StackTestDto.class)
                .when(stackTestClient.createV4(), key("stack-post"))
                .await(STACK_AVAILABLE)
                .select(s -> s.getInstanceId("worker"), key(StackDeleteAction.INSTANCE_ID))
                .select(s -> true, key("forced"))
                .capture(s -> s.getInstanceMetaData("worker").size() - 1, key("metadatasize"))
                .when(stackTestClient.deleteInstanceV4(), key("deleteInstance"))
                .await(STACK_AVAILABLE)
                .verify(s -> s.getInstanceMetaData("worker").size(), key("metadatasize"))
                .validate();
    }
}
