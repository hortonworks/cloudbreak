package com.sequenceiq.it.cloudbreak.newway.testcase;

import static com.sequenceiq.cloudbreak.common.type.HostMetadataState.HEALTHY;
import static com.sequenceiq.cloudbreak.common.type.HostMetadataState.UNHEALTHY;
import static com.sequenceiq.it.cloudbreak.newway.cloud.HostGroupType.WORKER;

import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.sequenceiq.cloudbreak.common.type.HostMetadataState;
import com.sequenceiq.it.cloudbreak.newway.Stack;
import com.sequenceiq.it.cloudbreak.newway.action.stack.StackTestAction;
import com.sequenceiq.it.cloudbreak.newway.entity.stack.StackTestDto;
import com.sequenceiq.it.cloudbreak.newway.context.MockedTestContext;
import com.sequenceiq.it.cloudbreak.newway.entity.InstanceGroupEntity;
import com.sequenceiq.it.spark.StatefulRoute;

public class RecoveryItTest extends AbstractIntegrationTest {

    private static final String WORKER_ID = "ig";

    private static final String HOSTS = "/api/v1/hosts";

    @BeforeMethod
    public void beforeMethod(Object[] data) {
        MockedTestContext testContext = (MockedTestContext) data[0];
        minimalSetupForClusterCreation(testContext);
    }

    @AfterMethod(alwaysRun = true)
    public void tear(Object[] data) {
        ((MockedTestContext) data[0]).cleanupTestContextEntity();
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK, enabled = false)
    public void testWhenSyncTellsNodesAreUnhealthyThenClusterStatusHaveToChange(MockedTestContext testContext) {
        String stackName = getNameGenerator().getRandomNameForMock();
        mockAmbari(testContext);
        testContext
                .given(WORKER_ID, InstanceGroupEntity.class).withHostGroup(WORKER).withNodeCount(1)
                .given(StackTestDto.class).withName(stackName).replaceInstanceGroups(WORKER_ID)
                .when(Stack.postV4())
                .await(STACK_AVAILABLE)
                .when(StackTestAction::sync)
                .await(STACK_FAILED)
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