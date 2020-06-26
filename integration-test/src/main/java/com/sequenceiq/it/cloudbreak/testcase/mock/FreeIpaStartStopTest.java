package com.sequenceiq.it.cloudbreak.testcase.mock;

import java.util.Collections;

import javax.inject.Inject;

import org.testng.annotations.Test;

import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.Status;
import com.sequenceiq.it.cloudbreak.client.FreeIPATestClient;
import com.sequenceiq.it.cloudbreak.context.Description;
import com.sequenceiq.it.cloudbreak.context.MockedTestContext;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.freeipa.FreeIPATestDto;
import com.sequenceiq.it.cloudbreak.mock.ITResponse;
import com.sequenceiq.it.cloudbreak.mock.freeipa.FreeIpaRouteHandler;
import com.sequenceiq.it.cloudbreak.mock.freeipa.ServerConnCheckFreeipaRpcResponse;
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
        getFreeIpaRouteHandler().updateResponse("server_conncheck", new ServerConnCheckFreeipaRpcResponse());
        testContext
                .given(FreeIPATestDto.class).withCatalog(testContext.getImageCatalogMockServerSetup().getFreeIpaImageCatalogUrl())
                .when(freeIPATestClient.create())
                .await(Status.AVAILABLE);
        getFreeIpaRouteHandler().updateResponse("server_conncheck", new ServerConnCheckFreeipaRpcResponse(false, Collections.emptyList()));
        testContext.given(FreeIPATestDto.class)
                .when(freeIPATestClient.stop())
                .await(Status.STOPPED);
        getFreeIpaRouteHandler().updateResponse("server_conncheck", new ServerConnCheckFreeipaRpcResponse());
        testContext.given(FreeIPATestDto.class)
                .when(freeIPATestClient.start())
                .await(Status.AVAILABLE)
                .validate();
    }
}
