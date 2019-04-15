package com.sequenceiq.it.cloudbreak.newway.testcase.mock;

import java.util.Map;

import javax.inject.Inject;

import org.testng.annotations.Test;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status;
import com.sequenceiq.it.cloudbreak.newway.client.StackTestClient;
import com.sequenceiq.it.cloudbreak.newway.context.Description;
import com.sequenceiq.it.cloudbreak.newway.context.TestContext;
import com.sequenceiq.it.cloudbreak.newway.dto.StackRepositoryTestDto;
import com.sequenceiq.it.cloudbreak.newway.dto.stack.StackTestDto;
import com.sequenceiq.it.cloudbreak.newway.testcase.AbstractIntegrationTest;
import com.sequenceiq.it.cloudbreak.newway.v4.MaintenanceModePostAction;
import com.sequenceiq.it.cloudbreak.newway.v4.UpdateStackDataAction;

public class MaintenanceModeTest extends AbstractIntegrationTest {

    private static final Map<String, Status> CLUSTER_MAINTENANCE_MODE = Map.of("status", Status.AVAILABLE, "clusterStatus", Status.MAINTENANCE_MODE_ENABLED);

    @Inject
    private StackTestClient stackTestClient;

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    @Description(
            given = "a valid cluster request",
            when = "calling create cluster and then modelling a maintenance mode on it",
            then = "cluster should be available after the whole process")
    public void testMaintenanceMode(TestContext testContext) {
        testContext
                .given(StackTestDto.class)
                .when(stackTestClient.createV4())
                .await(STACK_AVAILABLE)
                .given(StackTestDto.class)
                .when(MaintenanceModePostAction.enable())
                .await(CLUSTER_MAINTENANCE_MODE)
                .given(StackTestDto.class)
                .when(stackTestClient.changeImage())
                .await(CLUSTER_MAINTENANCE_MODE)
                .when(stackTestClient.syncV4())
                .await(CLUSTER_MAINTENANCE_MODE)

                .given(StackRepositoryTestDto.class)
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
}