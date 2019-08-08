package com.sequenceiq.it.cloudbreak.testcase.mock.cluster;

import static com.sequenceiq.cloudbreak.cloud.model.InstanceStatus.STARTED;
import static com.sequenceiq.cloudbreak.cloud.model.InstanceStatus.STOPPED;
import static java.lang.String.format;
import static javax.ws.rs.core.MediaType.TEXT_PLAIN;
import static javax.ws.rs.core.Response.Status.OK;

import javax.inject.Inject;

import org.testng.annotations.Test;

import com.sequenceiq.it.cloudbreak.Mock;
import com.sequenceiq.it.cloudbreak.client.StackTestClient;
import com.sequenceiq.it.cloudbreak.context.Description;
import com.sequenceiq.it.cloudbreak.context.MockedTestContext;
import com.sequenceiq.it.cloudbreak.context.RunningParameter;
import com.sequenceiq.it.cloudbreak.dto.stack.StackTestDto;
import com.sequenceiq.it.cloudbreak.mock.ITResponse;
import com.sequenceiq.it.cloudbreak.mock.model.SPIMock;
import com.sequenceiq.it.cloudbreak.mock.spi.CloudVmInstanceStatuses;
import com.sequenceiq.it.cloudbreak.testcase.AbstractIntegrationTest;
import com.sequenceiq.it.cloudbreak.spark.StatefulRoute;

import spark.Route;

public class AmbariPasswordUpdateTest extends AbstractIntegrationTest {

    private static final String CLOUD_INSTANCE_STATUSES = ITResponse.MOCK_ROOT + SPIMock.CLOUD_INSTANCE_STATUSES;

    @Inject
    private StackTestClient stackTestClient;

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    @Description(
            given = "a stack with an Ambari cluster",
            when = "password of the cluster is modified",
            then = "the cluster should still be available")
    public void createAmbariClusterAndModifyThePasswordOnItThenNoExceptionOccursTheStackIsAvailable(MockedTestContext testContext) {
        String clusterName = resourcePropertyProvider().getName();
        String generatedKey = resourcePropertyProvider().getName();

        mockAmbari(testContext, clusterName);
        mockSpi(testContext);
        testContext
                .given(generatedKey, StackTestDto.class)
                .valid()
                .withName(clusterName)
                .when(stackTestClient.createV4(), RunningParameter.key(generatedKey))
                .await(STACK_AVAILABLE, RunningParameter.key(generatedKey))
                .when(stackTestClient.modifyAmbariPasswordV4(), RunningParameter.key(generatedKey))
                .await(STACK_AVAILABLE, RunningParameter.key(generatedKey))
                .validate();
    }

    private void mockAmbari(MockedTestContext testContext, String clusterName) {
        Route passAmbari = (request, response) -> {
            response.type(TEXT_PLAIN);
            response.status(OK.getStatusCode());
            response.body(format("{\"href\":\"%s\",\"Requests\":{\"id\":12,\"status\":\"Accepted\"}}", request.url()));
            return "";
        };

        testContext.getModel().getAmbariMock().getDynamicRouteStack().clearPost(format("/api/v1/clusters/%s/*", clusterName));
        testContext.getModel().getAmbariMock().getDynamicRouteStack().put(format("/api/v1/clusters/%s/*", clusterName), passAmbari);
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

        testContext.getModel().getSpiMock().getDynamicRouteStack().clearPost(CLOUD_INSTANCE_STATUSES);
        testContext.getModel().getSpiMock().getDynamicRouteStack().post(CLOUD_INSTANCE_STATUSES, okState);
        testContext.getModel().getSpiMock().getDynamicRouteStack().post(CLOUD_INSTANCE_STATUSES, stoppedStateSpi);
    }
}
