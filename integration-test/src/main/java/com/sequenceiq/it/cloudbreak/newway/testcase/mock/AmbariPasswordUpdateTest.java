package com.sequenceiq.it.cloudbreak.newway.testcase.mock;

import static com.sequenceiq.cloudbreak.cloud.model.InstanceStatus.STARTED;
import static com.sequenceiq.cloudbreak.cloud.model.InstanceStatus.STOPPED;
import static com.sequenceiq.it.cloudbreak.newway.Mock.gson;
import static com.sequenceiq.it.cloudbreak.newway.context.RunningParameter.key;
import static com.sequenceiq.it.spark.ITResponse.MOCK_ROOT;
import static java.lang.String.format;
import static javax.ws.rs.core.MediaType.TEXT_PLAIN;
import static javax.ws.rs.core.Response.Status.OK;

import javax.inject.Inject;

import org.testng.annotations.Test;

import com.sequenceiq.it.cloudbreak.newway.client.StackTestClient;
import com.sequenceiq.it.cloudbreak.newway.context.Description;
import com.sequenceiq.it.cloudbreak.newway.context.MockedTestContext;
import com.sequenceiq.it.cloudbreak.newway.dto.stack.StackTestDto;
import com.sequenceiq.it.cloudbreak.newway.mock.model.SPIMock;
import com.sequenceiq.it.cloudbreak.newway.testcase.AbstractIntegrationTest;
import com.sequenceiq.it.spark.StatefulRoute;
import com.sequenceiq.it.spark.spi.CloudVmInstanceStatuses;

import spark.Route;

public class AmbariPasswordUpdateTest extends AbstractIntegrationTest {

    private static final String CLOUD_INSTANCE_STATUSES = MOCK_ROOT + SPIMock.CLOUD_INSTANCE_STATUSES;

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
                .when(stackTestClient.createV4(), key(generatedKey))
                .await(STACK_AVAILABLE, key(generatedKey))
                .when(stackTestClient.modifyAmbariPasswordV4(), key(generatedKey))
                .await(STACK_AVAILABLE, key(generatedKey))
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

        testContext.getModel().getSpiMock().getDynamicRouteStack().clearPost(CLOUD_INSTANCE_STATUSES);
        testContext.getModel().getSpiMock().getDynamicRouteStack().post(CLOUD_INSTANCE_STATUSES, okState);
        testContext.getModel().getSpiMock().getDynamicRouteStack().post(CLOUD_INSTANCE_STATUSES, stoppedStateSpi);
    }
}
