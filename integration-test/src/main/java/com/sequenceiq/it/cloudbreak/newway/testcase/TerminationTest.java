package com.sequenceiq.it.cloudbreak.newway.testcase;

import static com.sequenceiq.it.cloudbreak.newway.context.RunningParameter.key;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.sequenceiq.it.cloudbreak.newway.Stack;
import com.sequenceiq.it.cloudbreak.newway.StackEntity;
import com.sequenceiq.it.cloudbreak.newway.action.StackDeleteInstanceAction;
import com.sequenceiq.it.cloudbreak.newway.assertion.AssertStatusReasonMessage;
import com.sequenceiq.it.cloudbreak.newway.cloud.HostGroupType;
import com.sequenceiq.it.cloudbreak.newway.context.TestContext;
import com.sequenceiq.it.cloudbreak.newway.entity.InstanceGroupEntity;

public class TerminationTest extends AbstractIntegrationTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(TerminationTest.class);

    @BeforeMethod
    public void beforeMethod(Object[] data) {
        TestContext testContext = (TestContext) data[0];
        createDefaultUser(testContext);
        createDefaultCredential(testContext);
        createDefaultImageCatalog(testContext);
    }

    @AfterMethod(alwaysRun = true)
    public void tear(Object[] data) {
        TestContext testContext = (TestContext) data[0];
        testContext.cleanupTestContextEntity();
    }

    @Test(dataProvider = "testContext")
    public void testInstanceTermination(TestContext testContext) {
        testContext
                // create stack
                .given("ig", InstanceGroupEntity.class).withHostGroup(HostGroupType.WORKER).withNodeCount(3)
                .given(StackEntity.class).replaceInstanceGroups("ig")
                .when(Stack.postV3(), key("stack-post"))
                .await(STACK_AVAILABLE)
                //select an instance id
                .select(s -> s.getInstanceId(HostGroupType.WORKER.getName()), key(StackDeleteInstanceAction.INSTANCE_ID))
                .capture(s -> s.getInstanceMetaData(HostGroupType.WORKER.getName()).size() - 1, key("metadatasize"))
                .when(new StackDeleteInstanceAction(), key("stack-delete-instance"))
                .await(STACK_AVAILABLE)
                .verify(s -> s.getInstanceMetaData(HostGroupType.WORKER.getName()).size(), key("metadatasize"))
                .validate();
    }

    @Test(dataProvider = "testContext")
    public void testInstanceTerminationReplicationError(TestContext testContext) {
        testContext
                // create stack
                .given("ig", InstanceGroupEntity.class).withHostGroup(HostGroupType.WORKER).withNodeCount(2)
                .given(StackEntity.class).replaceInstanceGroups("ig")
                .when(Stack.postV3(), key("stack-post"))
                .await(STACK_AVAILABLE)
                .select(s -> s.getInstanceId("worker"), key(StackDeleteInstanceAction.INSTANCE_ID))
                .when(new StackDeleteInstanceAction(), key("deleteInstance"))
                .await(STACK_AVAILABLE)
                .then(new AssertStatusReasonMessage<>("Node(s) could not be removed from the cluster: There is not enough node to downscale. "
                        + "Check the replication factor and the ApplicationMaster occupation."))
                .validate();
    }

    @Test(dataProvider = "testContext")
    public void testInstanceTerminationForced(TestContext testContext) {
        testContext
                // create stack
                .given(StackEntity.class)
                .when(Stack.postV3(), key("stack-post"))
                .await(STACK_AVAILABLE)
                .select(s -> s.getInstanceId("worker"), key(StackDeleteInstanceAction.INSTANCE_ID))
                .select(s -> true, key("forced"))
                .capture(s -> s.getInstanceMetaData("worker").size() - 1, key("metadatasize"))
                .when(new StackDeleteInstanceAction(), key("deleteInstance"))
                .await(STACK_AVAILABLE)
                .verify(s -> s.getInstanceMetaData("worker").size(), key("metadatasize"))
                .validate();
    }
}
