package com.sequenceiq.it.cloudbreak.newway.testcase;

import static com.sequenceiq.cloudbreak.cloud.model.InstanceStatus.STARTED;
import static com.sequenceiq.cloudbreak.cloud.model.InstanceStatus.STOPPED;
import static com.sequenceiq.it.cloudbreak.newway.Mock.gson;
import static com.sequenceiq.it.spark.ITResponse.MOCK_ROOT;
import static java.lang.String.format;
import static javax.ws.rs.core.MediaType.TEXT_PLAIN;
import static javax.ws.rs.core.Response.Status.OK;

import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status;
import com.sequenceiq.it.cloudbreak.newway.Stack;
import com.sequenceiq.it.cloudbreak.newway.StackEntity;
import com.sequenceiq.it.cloudbreak.newway.context.TestContext;
import com.sequenceiq.it.cloudbreak.newway.mock.model.SPIMock;
import com.sequenceiq.it.spark.StatefulRoute;
import com.sequenceiq.it.spark.spi.CloudVmInstanceStatuses;

import spark.Route;

public class ClusterStopTest extends AbstractIntegrationTest {

    private static final String TEST_CONTEXT = "testContext";

    private static final String CLOUD_INSTANCE_STATUSES = MOCK_ROOT + SPIMock.CLOUD_INSTANCE_STATUSES;

    @BeforeMethod
    public void beforeMethod(Object[] data) {
        TestContext testContext = (TestContext) data[0];
        createDefaultUser(testContext);
        createDefaultCredential(testContext);
        createDefaultImageCatalog(testContext);
        initializeDefaultBlueprints(testContext);
    }

    @AfterMethod(alwaysRun = true)
    public void tear(Object[] data) {
        ((TestContext) data[0]).cleanupTestContextEntity();
    }

    @Test(dataProvider = TEST_CONTEXT)
    public void testClusterStop(TestContext testContext) {
        String clusterName = getNameGenerator().getRandomNameForMock();
        mockAmbari(testContext, clusterName);
        mockSpi(testContext);
        testContext
                .given(StackEntity.class).valid().withName(clusterName)
                .when(Stack.postV2())
                .await(Status.AVAILABLE)
                .when(Stack.stopV2())
                .await(Status.STOPPED)
                .validate();
    }

    private void mockAmbari(TestContext testContext, String clusterName) {
        Route passAmbari = (request, response) -> {
            response.type(TEXT_PLAIN);
            response.status(OK.getStatusCode());
            response.body(format("{\"href\":\"%s\",\"Requests\":{\"id\":12,\"status\":\"Accepted\"}}", request.url()));
            return "";
        };

        testContext.getModel().getAmbariMock().getDynamicRouteStack().clearPost(format("/api/v1/clusters/%s/*", clusterName));
        testContext.getModel().getAmbariMock().getDynamicRouteStack().put(format("/api/v1/clusters/%s/*", clusterName), passAmbari);
    }

    private void mockSpi(TestContext testContext) {
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