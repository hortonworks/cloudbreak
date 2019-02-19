package com.sequenceiq.it.cloudbreak.newway.testcase;

import static com.sequenceiq.cloudbreak.cloud.model.InstanceStatus.STARTED;
import static com.sequenceiq.cloudbreak.cloud.model.InstanceStatus.STOPPED;
import static com.sequenceiq.it.cloudbreak.newway.Mock.gson;
import static com.sequenceiq.it.spark.ITResponse.MOCK_ROOT;
import static javax.ws.rs.core.MediaType.TEXT_PLAIN;
import static javax.ws.rs.core.Response.Status.OK;

import org.testng.annotations.Test;

import com.sequenceiq.it.cloudbreak.newway.Stack;
import com.sequenceiq.it.cloudbreak.newway.StackEntity;
import com.sequenceiq.it.cloudbreak.newway.context.MockedTestContext;
import com.sequenceiq.it.cloudbreak.newway.entity.AmbariEntity;
import com.sequenceiq.it.cloudbreak.newway.entity.ClusterEntity;
import com.sequenceiq.it.cloudbreak.newway.entity.clusterdefinition.ClusterDefinitionEntity;
import com.sequenceiq.it.cloudbreak.newway.mock.model.SPIMock;
import com.sequenceiq.it.spark.StatefulRoute;
import com.sequenceiq.it.spark.spi.CloudVmInstanceStatuses;

public class ClouderaManagerStartStopTest extends AbstractClouderaManagerTest {

    private static final String CLOUD_INSTANCE_STATUSES = MOCK_ROOT + SPIMock.CLOUD_INSTANCE_STATUSES;

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    public void testCreateNewRegularCluster(MockedTestContext testContext) {
        mockSpi(testContext);
        String name = testContext.get(ClusterDefinitionEntity.class).getRequest().getName();
        testContext
                .given("cm", AmbariEntity.class).withClusterDefinitionName(name).withValidateClusterDefinition(Boolean.FALSE)
                .given("cmcluster", ClusterEntity.class).withAmbari("cm")
                .given(StackEntity.class).withCluster("cmcluster")
                .when(Stack.postV4())
                .await(STACK_AVAILABLE)
                .when(Stack.stop())
                .await(STACK_STOPPED)
                .when(Stack.start())
                .await(STACK_AVAILABLE)
                .validate();
    }

    private void mockSpi(MockedTestContext testContext) {
        StatefulRoute okState = (request, response, model) -> {
            String resultJson = gson().toJson(new CloudVmInstanceStatuses(model.getInstanceMap()).createCloudVmInstanceStatuses());
            response.body(resultJson);
            response.type(TEXT_PLAIN);
            response.status(OK.getStatusCode());
            return "";
        };

        StatefulRoute stoppedStateSpi = (request, response, model) -> {
            String resultJson = gson().toJson(new CloudVmInstanceStatuses(model.getInstanceMap()).createCloudVmInstanceStatuses());
            response.body(resultJson.replaceAll(STARTED.name(), STOPPED.name()));
            response.type(TEXT_PLAIN);
            response.status(OK.getStatusCode());
            return "";
        };

        StatefulRoute startedStateSpi = (request, response, model) -> {
            String resultJson = gson().toJson(new CloudVmInstanceStatuses(model.getInstanceMap()).createCloudVmInstanceStatuses());
            response.body(resultJson.replaceAll(STOPPED.name(), STARTED.name()));
            response.type(TEXT_PLAIN);
            response.status(OK.getStatusCode());
            return "";
        };

        testContext.getModel().getSpiMock().getDynamicRouteStack().clearPost(CLOUD_INSTANCE_STATUSES);
        testContext.getModel().getSpiMock().getDynamicRouteStack().post(CLOUD_INSTANCE_STATUSES, okState);
        testContext.getModel().getSpiMock().getDynamicRouteStack().post(CLOUD_INSTANCE_STATUSES, startedStateSpi);
        testContext.getModel().getSpiMock().getDynamicRouteStack().post(CLOUD_INSTANCE_STATUSES, stoppedStateSpi);
    }
}
