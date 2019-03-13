package com.sequenceiq.it.cloudbreak.newway.testcase.mock;

import java.util.Map;

import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status;
import com.sequenceiq.it.cloudbreak.newway.Stack;
import com.sequenceiq.it.cloudbreak.newway.action.stack.StackTestAction;
import com.sequenceiq.it.cloudbreak.newway.context.Description;
import com.sequenceiq.it.cloudbreak.newway.context.MockedTestContext;
import com.sequenceiq.it.cloudbreak.newway.context.TestContext;
import com.sequenceiq.it.cloudbreak.newway.entity.StackRepositoryEntity;
import com.sequenceiq.it.cloudbreak.newway.entity.stack.StackTestDto;
import com.sequenceiq.it.cloudbreak.newway.testcase.AbstractIntegrationTest;
import com.sequenceiq.it.cloudbreak.newway.v4.ChangeImageAction;
import com.sequenceiq.it.cloudbreak.newway.v4.MaintenanceModePostAction;
import com.sequenceiq.it.cloudbreak.newway.v4.UpdateStackDataAction;

public class MaintenanceModeTest extends AbstractIntegrationTest {

    private static final Map<String, Status> CLUSTER_MAINTENANCE_MODE = Map.of("status", Status.AVAILABLE, "clusterStatus", Status.MAINTENANCE_MODE_ENABLED);

    @BeforeMethod
    public void beforeMethod(Object[] data) {
        MockedTestContext testContext = (MockedTestContext) data[0];
        createDefaultUser(testContext);
        createDefaultCredential(testContext);
        createDefaultEnvironment(testContext);
        createDefaultImageCatalog(testContext);
        initializeDefaultClusterDefinitions(testContext);
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    @Description(
            given = "a valid cluster request",
            when = "calling create cluster and then modelling a maintenance mode on it",
            then = "cluster should be available after the whole process")
    public void testMaintenanceMode(TestContext testContext) {
        testContext
                .given(StackTestDto.class)
                .when(Stack.postV4())
                .await(STACK_AVAILABLE)
                .given(StackTestDto.class)
                .when(MaintenanceModePostAction.enable())
                .await(CLUSTER_MAINTENANCE_MODE)
                .given(StackTestDto.class)
                .when(ChangeImageAction.valid())
                .await(CLUSTER_MAINTENANCE_MODE)
                .when(StackTestAction::sync)
                .await(CLUSTER_MAINTENANCE_MODE)

                .given(StackRepositoryEntity.class)
                .withOsType("RHEL")
                .withUtilsBaseURL("http://public-repo-1.hortonworks.com/HDP/centos7/2.x/updates/2.7.5.0")
                .withUtilsRepoId("HDP-2.7.5")
                .withEnableGplRepo(true)
                .withStackBaseURL("http://public-repo-1.hortonworks.com/HDP/centos7/2.x/updates/2.7.5.0")
                .withRepositoryVersion("2.7.5")
                .when(new UpdateStackDataAction())

                .given(StackTestDto.class)
                .when(MaintenanceModePostAction.validate())
                .await(CLUSTER_MAINTENANCE_MODE)
                .when(MaintenanceModePostAction.disable())
                .await(STACK_AVAILABLE)
                .validate();
    }

    @AfterMethod(alwaysRun = true)
    public void tear(Object[] data) {
        MockedTestContext testContext = (MockedTestContext) data[0];
        testContext.cleanupTestContextEntity();
    }
}