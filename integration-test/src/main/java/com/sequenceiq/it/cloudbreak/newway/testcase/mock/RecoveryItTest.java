package com.sequenceiq.it.cloudbreak.newway.testcase.mock;

import static com.sequenceiq.cloudbreak.common.type.HostMetadataState.HEALTHY;
import static com.sequenceiq.cloudbreak.common.type.HostMetadataState.UNHEALTHY;
import static com.sequenceiq.it.cloudbreak.newway.cloud.HostGroupType.WORKER;
import static com.sequenceiq.it.cloudbreak.newway.context.RunningParameter.key;

import javax.inject.Inject;

import org.testng.annotations.Test;

import com.sequenceiq.cloudbreak.common.type.HostMetadataState;
import com.sequenceiq.it.cloudbreak.newway.client.StackTestClient;
import com.sequenceiq.it.cloudbreak.newway.context.Description;
import com.sequenceiq.it.cloudbreak.newway.context.MockedTestContext;
import com.sequenceiq.it.cloudbreak.newway.dto.InstanceGroupTestDto;
import com.sequenceiq.it.cloudbreak.newway.dto.stack.StackTestDto;
import com.sequenceiq.it.cloudbreak.newway.testcase.AbstractIntegrationTest;
import com.sequenceiq.it.spark.StatefulRoute;

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
                .withHostGroup(WORKER)
                .withNodeCount(1)
                .given(stackName, StackTestDto.class)
                .withName(stackName)
                .replaceInstanceGroups(workerId)
                .when(stackTestClient.createV4(), key(stackName))
                .await(STACK_AVAILABLE, key(stackName))
                .when(stackTestClient.syncV4(), key(stackName))
                .await(STACK_FAILED, key(stackName))
                .validate();
    }

    private void mockAmbari(MockedTestContext testContext) {
        testContext.getModel().getAmbariMock().getDynamicRouteStack().clearPost(HOSTS);
        modifyStatusResponses(testContext, UNHEALTHY, 2);
        modifyStatusResponses(testContext, HEALTHY, 1);
    }

    private void modifyStatusResponses(MockedTestContext testContext, HostMetadataState state, int quantity) {
        for (int i = 0; i < quantity; i++) {
            testContext.getModel().getAmbariMock().getDynamicRouteStack().post(HOSTS, createHostResponseForAmbariWithStatus(state));
        }
    }

    private StatefulRoute createHostResponseForAmbariWithStatus(HostMetadataState overridedStatus) {
        return (request, response, model) -> {
            response.type("text/plain");
            response.status(200);
            /*List<Map<String, ?>> itemList = new ArrayList<>();
            for (Entry<String, CloudVmMetaDataStatus> stringCloudVmMetaDataStatusEntry : model.getInstanceMap().entrySet()) {
                CloudVmMetaDataStatus status = stringCloudVmMetaDataStatusEntry.getValue();
                if (InstanceStatus.STARTED == status.getCloudVmInstanceStatus().getStatus()) {
                    Hosts hosts = new Hosts(Collections.singletonList(HostNameUtil
                            .generateHostNameByIp(status.getMetaData().getPrivateIp())), overridedStatus.name());
                    itemList.add(Collections.singletonMap("Hosts", hosts));
                }
            }*/
            return "";
        };
    }

}