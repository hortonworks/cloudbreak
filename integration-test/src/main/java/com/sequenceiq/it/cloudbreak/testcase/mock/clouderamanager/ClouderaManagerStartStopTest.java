package com.sequenceiq.it.cloudbreak.testcase.mock.clouderamanager;

import static com.sequenceiq.cloudbreak.cloud.model.InstanceStatus.STARTED;
import static com.sequenceiq.cloudbreak.cloud.model.InstanceStatus.STOPPED;
import static com.sequenceiq.it.cloudbreak.context.RunningParameter.key;
import static javax.ws.rs.core.MediaType.TEXT_PLAIN;
import static javax.ws.rs.core.Response.Status.OK;

import javax.inject.Inject;

import org.testng.annotations.Test;

import com.sequenceiq.it.cloudbreak.Mock;
import com.sequenceiq.it.cloudbreak.client.BlueprintTestClient;
import com.sequenceiq.it.cloudbreak.client.StackTestClient;
import com.sequenceiq.it.cloudbreak.context.Description;
import com.sequenceiq.it.cloudbreak.context.MockedTestContext;
import com.sequenceiq.it.cloudbreak.dto.ClouderaManagerTestDto;
import com.sequenceiq.it.cloudbreak.dto.ClusterTestDto;
import com.sequenceiq.it.cloudbreak.dto.blueprint.BlueprintTestDto;
import com.sequenceiq.it.cloudbreak.dto.stack.StackTestDto;
import com.sequenceiq.it.cloudbreak.mock.ITResponse;
import com.sequenceiq.it.cloudbreak.mock.model.SPIMock;
import com.sequenceiq.it.cloudbreak.mock.spi.CloudVmInstanceStatuses;
import com.sequenceiq.it.cloudbreak.spark.StatefulRoute;

public class ClouderaManagerStartStopTest extends AbstractClouderaManagerTest {

    private static final String CLOUD_INSTANCE_STATUSES = ITResponse.MOCK_ROOT + SPIMock.CLOUD_INSTANCE_STATUSES;

    @Inject
    private BlueprintTestClient blueprintTestClient;

    @Inject
    private StackTestClient stackTestClient;

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    @Description(
            given = "a Cloudera Manager cluster",
            when = "the cluster is stoppend and started",
            then = "the cluster should be available")
    public void createRegularClouderaManagerClusterThenWaitForAvailableThenStopThenStartThenWaitForAvailableThenNoExceptionOccurs(
            MockedTestContext testContext) {
        mockSpi(testContext);
        String name = testContext.get(BlueprintTestDto.class).getRequest().getName();
        String cm = resourcePropertyProvider().getName();
        String cmcluster = resourcePropertyProvider().getName();
        String stack = resourcePropertyProvider().getName();

        testContext
                .given(cm, ClouderaManagerTestDto.class)
                .given(cmcluster, ClusterTestDto.class)
                .withValidateBlueprint(Boolean.FALSE)
                .withBlueprintName(name)
                .withClouderaManager(cm)
                .given(stack, StackTestDto.class)
                .withCluster(cmcluster)
                .when(stackTestClient.createV4(), key(stack))
                .await(STACK_AVAILABLE, key(stack))
                .when(stackTestClient.stopV4(), key(stack))
                .await(STACK_STOPPED, key(stack))
                .when(stackTestClient.startV4(), key(stack))
                .await(STACK_AVAILABLE, key(stack))
                .validate();
    }

    private void mockSpi(MockedTestContext testContext) {
        StatefulRoute okState = (request, response, model) -> {
            String resultJson = Mock.gson().toJson(new CloudVmInstanceStatuses(model.getInstanceMap()).createCloudVmInstanceStatuses());
            response.body(resultJson);
            response.type(TEXT_PLAIN);
            response.status(OK.getStatusCode());
            return "";
        };

        StatefulRoute stoppedStateSpi = (request, response, model) -> {
            String resultJson = Mock.gson().toJson(new CloudVmInstanceStatuses(model.getInstanceMap()).createCloudVmInstanceStatuses());
            response.body(resultJson.replaceAll(STARTED.name(), STOPPED.name()));
            response.type(TEXT_PLAIN);
            response.status(OK.getStatusCode());
            return "";
        };

        StatefulRoute startedStateSpi = (request, response, model) -> {
            String resultJson = Mock.gson().toJson(new CloudVmInstanceStatuses(model.getInstanceMap()).createCloudVmInstanceStatuses());
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

    @Override
    protected BlueprintTestClient blueprintTestClient() {
        return blueprintTestClient;
    }
}
