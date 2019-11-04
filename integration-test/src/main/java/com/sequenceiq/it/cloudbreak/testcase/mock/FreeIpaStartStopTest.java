package com.sequenceiq.it.cloudbreak.testcase.mock;

import static com.sequenceiq.it.cloudbreak.mock.ITResponse.MOCK_ROOT;
import static com.sequenceiq.it.cloudbreak.mock.model.SPIMock.START_INSTANCE;
import static com.sequenceiq.it.cloudbreak.mock.model.SPIMock.STOP_INSTANCE;

import javax.inject.Inject;

import org.testng.annotations.Test;

import com.sequenceiq.cloudbreak.exception.BadRequestException;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.Status;
import com.sequenceiq.it.cloudbreak.client.FreeIPATestClient;
import com.sequenceiq.it.cloudbreak.context.Description;
import com.sequenceiq.it.cloudbreak.context.MockedTestContext;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.freeipa.FreeIPATestDto;
import com.sequenceiq.it.cloudbreak.mock.ITResponse;
import com.sequenceiq.it.cloudbreak.mock.freeipa.FreeIpaRouteHandler;
import com.sequenceiq.it.cloudbreak.spark.DynamicRouteStack;
import com.sequenceiq.it.cloudbreak.testcase.AbstractIntegrationTest;

public class FreeIpaStartStopTest extends AbstractIntegrationTest {

    @Inject
    private FreeIPATestClient freeIPATestClient;

    @Inject
    private FreeIpaRouteHandler freeIpaRouteHandler;

    @Override
    protected void setupTest(TestContext testContext) {
        createDefaultUser(testContext);
        createDefaultCredential(testContext);
        createDefaultEnvironmentWithNetwork(testContext);
        createDefaultImageCatalog(testContext);
        initializeDefaultBlueprints(testContext);
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    @Description(
            given = "environment is present",
            when = "calling a freeipe start",
            then = "freeipa sould be available")
    public void testStopStartFreeIpa(MockedTestContext testContext) {
        DynamicRouteStack dynamicRouteStack = testContext.getModel().getClouderaManagerMock().getDynamicRouteStack();
        dynamicRouteStack.post(ITResponse.FREEIPA_ROOT + "/session/login_password", (request, response) -> {
            response.cookie("ipa_session", "dummysession");
            return "";
        });
        dynamicRouteStack.post(ITResponse.FREEIPA_ROOT + "/session/json", freeIpaRouteHandler);
        testContext
                .given(FreeIPATestDto.class).withCatalog(testContext.getImageCatalogMockServerSetup().getFreeIpaImageCatalogUrl())
                .when(freeIPATestClient.create())
                .await(Status.AVAILABLE)
                .when(freeIPATestClient.stop())
                .await(Status.STOPPED)
                .when(freeIPATestClient.start())
                .await(Status.AVAILABLE)
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    @Description(
            given = "environment is present",
            when = "calling a freeipe stop",
            then = "freeipa sould be failed stop")
    public void testFailedStopFreeIpa(MockedTestContext testContext) {
        DynamicRouteStack dynamicRouteStack = testContext.getModel().getClouderaManagerMock().getDynamicRouteStack();
        dynamicRouteStack.post(ITResponse.FREEIPA_ROOT + "/session/login_password", (request, response) -> {
            response.cookie("ipa_session", "dummysession");
            return "";
        });
        testContext.getModel().getSpiMock().getDynamicRouteStack().get(MOCK_ROOT + STOP_INSTANCE, (request, response) -> {
            throw new BadRequestException("Stop failed on mock instance");
        });
        dynamicRouteStack.post(ITResponse.FREEIPA_ROOT + "/session/json", freeIpaRouteHandler);
        testContext
                .given(FreeIPATestDto.class).withCatalog(testContext.getImageCatalogMockServerSetup().getFreeIpaImageCatalogUrl())
                .when(freeIPATestClient.create())
                .await(Status.AVAILABLE)
                .when(freeIPATestClient.stop())
                .await(Status.STOP_FAILED)
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    @Description(
            given = "environment is present",
            when = "calling a freeipe stop",
            then = "freeipa sould be failed start")
    public void testFailedStartFreeIpa(MockedTestContext testContext) {
        DynamicRouteStack dynamicRouteStack = testContext.getModel().getClouderaManagerMock().getDynamicRouteStack();
        dynamicRouteStack.post(ITResponse.FREEIPA_ROOT + "/session/login_password", (request, response) -> {
            response.cookie("ipa_session", "dummysession");
            return "";
        });
        testContext.getModel().getSpiMock().getDynamicRouteStack().get(MOCK_ROOT + START_INSTANCE, (request, response) -> {
            throw new BadRequestException("Start failed on mock instance");
        });
        dynamicRouteStack.post(ITResponse.FREEIPA_ROOT + "/session/json", freeIpaRouteHandler);
        testContext
                .given(FreeIPATestDto.class).withCatalog(testContext.getImageCatalogMockServerSetup().getFreeIpaImageCatalogUrl())
                .when(freeIPATestClient.create())
                .await(Status.AVAILABLE)
                .when(freeIPATestClient.stop())
                .await(Status.STOPPED)
                .when(freeIPATestClient.start())
                .await(Status.START_FAILED)
                .validate();
    }
}
