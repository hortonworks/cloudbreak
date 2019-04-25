package com.sequenceiq.it.cloudbreak.newway.testcase.mock;


import static com.sequenceiq.it.cloudbreak.newway.context.RunningParameter.key;
import static com.sequenceiq.it.cloudbreak.newway.mock.model.AmbariMock.BLUEPRINTS;
import static com.sequenceiq.it.spark.ITResponse.AMBARI_API_ROOT;
import static com.sequenceiq.it.spark.ITResponse.MOCK_ROOT;
import static com.sequenceiq.it.spark.ITResponse.SALT_API_ROOT;
import static com.sequenceiq.it.spark.ITResponse.SALT_BOOT_ROOT;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpMethod;
import org.testng.annotations.Test;

import com.sequenceiq.it.cloudbreak.newway.action.v4.stack.StackScalePostAction;
import com.sequenceiq.it.cloudbreak.newway.assertion.MockVerification;
import com.sequenceiq.it.cloudbreak.newway.client.StackTestClient;
import com.sequenceiq.it.cloudbreak.newway.context.Description;
import com.sequenceiq.it.cloudbreak.newway.context.MockedTestContext;
import com.sequenceiq.it.cloudbreak.newway.context.TestContext;
import com.sequenceiq.it.cloudbreak.newway.dto.stack.StackTestDto;
import com.sequenceiq.it.cloudbreak.newway.testcase.AbstractIntegrationTest;

import spark.Route;

public class UpscaleTest extends AbstractIntegrationTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(UpscaleTest.class);

    @Inject
    private StackTestClient stackTestClient;

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    @Description(
            given = "a stack with stack scale",
            when = "upscale to 15 after it downscale to 6",
            then = "stack is running")
    public void testStackScaling(TestContext testContext) {
        String stackName = resourcePropertyProvider().getName();

        testContext
                .given(stackName, StackTestDto.class)
                .when(stackTestClient.createV4(), key(stackName))
                .await(STACK_AVAILABLE, key(stackName))
                .when(StackScalePostAction.valid().withDesiredCount(15), key(stackName))
                .await(STACK_AVAILABLE, key(stackName))
                .when(StackScalePostAction.valid().withDesiredCount(6), key(stackName))
                .await(STACK_AVAILABLE, key(stackName))
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    @Description(
            given = "a stack with upscale",
            when = "upscale to 15",
            then = "stack is running")
    public void testUpscale(MockedTestContext testContext) {
        String clusterName = resourcePropertyProvider().getName();
        int originalWorkedCount = 1;
        int desiredWorkedCount = 15;
        int addedNodes = desiredWorkedCount - originalWorkedCount;
        testContext.given(StackTestDto.class)
                .withName(clusterName)
                .withGatewayPort(testContext.getSparkServer().getPort())
                .when(stackTestClient.createV4())
                .await(STACK_AVAILABLE)
                .when(StackScalePostAction.valid().withDesiredCount(desiredWorkedCount))
                .await(StackTestDto.class, STACK_AVAILABLE)
                .then(MockVerification.verify(HttpMethod.POST, "/api/v1/blueprints/"))
                .then(MockVerification.verify(HttpMethod.POST, MOCK_ROOT + "/cloud_instance_statuses").exactTimes(1))
                .then(MockVerification.verify(HttpMethod.POST, MOCK_ROOT + "/cloud_metadata_statuses")
                        .bodyContains("CREATE_REQUESTED", addedNodes).exactTimes(1))
                .then(MockVerification.verify(HttpMethod.GET, SALT_BOOT_ROOT + "/health").atLeast(1))
                .then(MockVerification.verify(HttpMethod.POST, SALT_BOOT_ROOT + "/salt/action/distribute").atLeast(1))
                .then(MockVerification.verify(HttpMethod.POST, SALT_API_ROOT + "/run").bodyContains("fun=network.ipaddrs").atLeast(1))
                .then(MockVerification.verify(HttpMethod.POST, SALT_API_ROOT + "/run").bodyContains("arg=roles&arg=ambari_server").atLeast(2))
                .then(MockVerification.verify(HttpMethod.POST, SALT_API_ROOT + "/run").bodyContains("fun=saltutil.sync_all").atLeast(1))
                .then(MockVerification.verify(HttpMethod.POST, SALT_API_ROOT + "/run").bodyContains("fun=mine.update").atLeast(1))
                .then(MockVerification.verify(HttpMethod.POST, SALT_API_ROOT + "/run").bodyContains("fun=state.highstate").atLeast(2))
                .then(MockVerification.verify(HttpMethod.POST, SALT_API_ROOT + "/run").bodyContains("fun=grains.remove").exactTimes(4))
                .then(MockVerification.verify(HttpMethod.POST, SALT_BOOT_ROOT + "/hostname/distribute")
                        .bodyRegexp("^.*\\[([\"0-9\\.]+([,]{0,1})){" + addedNodes + "}\\].*").exactTimes(2))
                .then(MockVerification.verify(HttpMethod.GET, AMBARI_API_ROOT + "/hosts").atLeast(1))
                .then(MockVerification.verify(HttpMethod.GET, AMBARI_API_ROOT + "/clusters").exactTimes(22))
                .then(MockVerification.verify(HttpMethod.GET, AMBARI_API_ROOT + "/clusters/" + clusterName).atLeast(1))
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    @Description(
            given = "a stack",
            when = "ambari is failing",
            then = "stack state is failed")
    public void testAmbariFailure(MockedTestContext testContext) {
        String stackName = resourcePropertyProvider().getName();
        mockAmbariBlueprintFail(testContext);
        testContext
                .given(stackName, StackTestDto.class)
                .when(stackTestClient.createV4(), key(stackName))
                .await(STACK_FAILED, key(stackName))
                .then(MockVerification.verify(HttpMethod.POST, "/api/v1/blueprints/").atLeast(1), key(stackName))
                .validate();
    }

    private void mockAmbariBlueprintFail(MockedTestContext testContext) {
        Route customResponse = (request, response) -> {
            response.type("text/plain");
            response.status(400);
            response.body("Bad blueprint format");
            return "";
        };
        testContext.getModel().getAmbariMock().getDynamicRouteStack().clearPost(BLUEPRINTS);
        testContext.getModel().getAmbariMock().getDynamicRouteStack().post(BLUEPRINTS, customResponse);
    }

}
