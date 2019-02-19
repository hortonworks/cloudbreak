package com.sequenceiq.it.cloudbreak.newway.testcase;

import static com.sequenceiq.it.cloudbreak.newway.context.RunningParameter.key;

import javax.ws.rs.BadRequestException;

import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.sequenceiq.it.cloudbreak.newway.Stack;
import com.sequenceiq.it.cloudbreak.newway.StackEntity;
import com.sequenceiq.it.cloudbreak.newway.action.stack.StackPostAction;
import com.sequenceiq.it.cloudbreak.newway.assertion.DatabaseExistsAssertion;
import com.sequenceiq.it.cloudbreak.newway.context.MockedTestContext;
import com.sequenceiq.it.cloudbreak.newway.context.TestContext;
import com.sequenceiq.it.cloudbreak.newway.entity.ClusterEntity;
import com.sequenceiq.it.cloudbreak.newway.entity.database.DatabaseEntity;

public class StackCreationTest extends AbstractIntegrationTest {

    @BeforeMethod
    public void beforeMethod(Object[] data) {
        MockedTestContext testContext = (MockedTestContext) data[0];
        minimalSetupForClusterCreation(testContext);
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    public void testCreateNewRegularCluster(TestContext testContext) {
        testContext.given(StackEntity.class)
                .when(new StackPostAction())
                .await(STACK_AVAILABLE)
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    public void testCreateNewRegularClusterWithDatabase(TestContext testContext) {
        String databaseName = getNameGenerator().getRandomNameForMock();
        String clusterName = getNameGenerator().getRandomNameForMock();
        testContext
                .given(DatabaseEntity.class).valid().withName(databaseName)
                .when(DatabaseEntity.post())
                .when(DatabaseEntity.list())
                .then(DatabaseExistsAssertion.getAssertion(databaseName, 1))
                .given(ClusterEntity.class).withName(clusterName).withDatabase(databaseName)
                .given(StackEntity.class).withName(clusterName).withCluster(testContext.get(ClusterEntity.class))
                .when(Stack.postV4())
                .await(STACK_AVAILABLE)
                .given(DatabaseEntity.class).withName(databaseName)
                .when(DatabaseEntity.deleteV2(), key("badRequest"))
                .expect(BadRequestException.class, key("badRequest"))
                .given(StackEntity.class).withName(clusterName)
                .when(Stack.deleteV4())
                .await(STACK_DELETED)
                .given(DatabaseEntity.class).withName(databaseName)
                .when(DatabaseEntity.deleteV2())
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    public void testAttemptToCreateTwoRegularClusterWithTheSameName(TestContext testContext) {
        testContext.given(StackEntity.class)
                .when(Stack.postV4())
                .when(Stack.postV4(), key("badRequest"))
                .expect(BadRequestException.class, key("badRequest"))
                .validate();
    }

    @AfterMethod(alwaysRun = true)
    public void tear(Object[] data) {
        MockedTestContext testContext = (MockedTestContext) data[0];
        testContext.cleanupTestContextEntity();
    }
}
