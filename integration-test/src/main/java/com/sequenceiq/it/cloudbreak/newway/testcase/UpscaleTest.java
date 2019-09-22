package com.sequenceiq.it.cloudbreak.newway.testcase;


import static com.sequenceiq.it.cloudbreak.newway.mock.model.AmbariMock.BLUEPRINTS;
import static com.sequenceiq.it.spark.ITResponse.AMBARI_API_ROOT;
import static com.sequenceiq.it.spark.ITResponse.MOCK_ROOT;
import static com.sequenceiq.it.spark.ITResponse.SALT_API_ROOT;
import static com.sequenceiq.it.spark.ITResponse.SALT_BOOT_ROOT;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpMethod;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.sequenceiq.it.cloudbreak.newway.Stack;
import com.sequenceiq.it.cloudbreak.newway.StackEntity;
import com.sequenceiq.it.cloudbreak.newway.action.StackPostAction;
import com.sequenceiq.it.cloudbreak.newway.action.StackScalePostAction;
import com.sequenceiq.it.cloudbreak.newway.assertion.MockVerification;
import com.sequenceiq.it.cloudbreak.newway.context.TestContext;

import spark.Route;

public class UpscaleTest extends AbstractIntegrationTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(UpscaleTest.class);

    @BeforeMethod
    public void beforeMethod(Object[] data) {
        TestContext testContext = (TestContext) data[0];
        LOGGER.info("All routes added: {}", testContext.getSparkServer().getSparkService().getPaths());
        createDefaultUser(testContext);
        createDefaultCredential(testContext);
        createDefaultImageCatalog(testContext);
    }

    @AfterMethod(alwaysRun = true)
    public void tear(Object[] data) {
        TestContext testContext = (TestContext) data[0];
        testContext.cleanupTestContextEntity();
    }

    @Test(dataProvider = "testContext")
    public void testStackScaling(TestContext testContext) {
        // GIVEN
        testContext.given(Stack.class)
                .when(new StackPostAction())
                .await(STACK_AVAILABLE)
                .when(StackScalePostAction.valid().withDesiredCount(15))
                .await(STACK_AVAILABLE)
                .when(StackScalePostAction.valid().withDesiredCount(6))
                .await(STACK_AVAILABLE)
                .validate();
    }

    @Test(dataProvider = "testContext")
    public void testUpscale(TestContext testContext) {
        String clusterName = getNameGenerator().getRandomNameForMock();
        int originalWorkedCount = 1;
        int desiredWorkedCount = 15;
        int addedNodes = desiredWorkedCount - originalWorkedCount;
        testContext.given(StackEntity.class).withName(clusterName).withGatewayPort(testContext.getSparkServer().getPort())
                .when(Stack.postV2())
                .await(STACK_AVAILABLE)
                .when(StackScalePostAction.valid().withDesiredCount(desiredWorkedCount))
                .await(StackEntity.class, STACK_AVAILABLE)
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
                .then(MockVerification.verify(HttpMethod.GET, AMBARI_API_ROOT + "/clusters").exactTimes(9))
                .then(MockVerification.verify(HttpMethod.GET, AMBARI_API_ROOT + "/clusters/" + clusterName).atLeast(1))
                .validate();
    }

    @Test(dataProvider = "testContext")
    public void testAmbariFailure(TestContext testContext) {
        mockAmbariBlueprintFail(testContext);
        testContext.given(Stack.class)
                .when(Stack.postV2())
                .await(STACK_FAILED)
                .then(MockVerification.verify(HttpMethod.POST, "/api/v1/blueprints/").atLeast(1))
                .validate();
    }

    private void mockAmbariBlueprintFail(TestContext testContext) {
        Route customResponse2 = (request, response) -> {
            response.type("text/plain");
            response.status(400);
            response.body("Bad blueprint format");
            return "";
        };
        testContext.getModel().getAmbariMock().getDynamicRouteStack().clearPost(BLUEPRINTS);
        testContext.getModel().getAmbariMock().getDynamicRouteStack().post(BLUEPRINTS, customResponse2);
    }

}
