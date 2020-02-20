package com.sequenceiq.it.cloudbreak.testcase.mock;

import javax.inject.Inject;

import org.testng.annotations.Test;

import com.sequenceiq.environment.api.v1.environment.model.response.EnvironmentStatus;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.Status;
import com.sequenceiq.it.cloudbreak.assertion.freeipa.FreeIpaChildEnvironmentAssertion;
import com.sequenceiq.it.cloudbreak.client.CredentialTestClient;
import com.sequenceiq.it.cloudbreak.client.EnvironmentTestClient;
import com.sequenceiq.it.cloudbreak.client.FreeIPATestClient;
import com.sequenceiq.it.cloudbreak.context.Description;
import com.sequenceiq.it.cloudbreak.context.MockedTestContext;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.environment.EnvironmentTestDto;
import com.sequenceiq.it.cloudbreak.dto.freeipa.FreeIPAChildEnvironmentTestDto;
import com.sequenceiq.it.cloudbreak.dto.freeipa.FreeIPATestDto;
import com.sequenceiq.it.cloudbreak.mock.ITResponse;
import com.sequenceiq.it.cloudbreak.mock.freeipa.FreeIpaRouteHandler;
import com.sequenceiq.it.cloudbreak.spark.DynamicRouteStack;
import com.sequenceiq.it.cloudbreak.testcase.AbstractIntegrationTest;

public class FreeIpaAttachChildEnvironmentTest extends AbstractIntegrationTest {

    @Inject
    private FreeIPATestClient freeIPATestClient;

    @Inject
    private CredentialTestClient credentialTestClient;

    @Inject
    private EnvironmentTestClient environmentTestClient;

    @Inject
    private FreeIpaRouteHandler freeIpaRouteHandler;

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
            when = "calling a freeipa attach child environment",
            then = "freeipa should be available with kerberos and ldap config")
    public void testAttachChildEnvironment(MockedTestContext testContext) {
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
                .given(FreeIPAChildEnvironmentTestDto.CHILD_ENVIRONMENT_KEY, EnvironmentTestDto.class).withCreateFreeIpa(false)
                .when(environmentTestClient.create())
                .await(EnvironmentStatus.AVAILABLE)
                .given(FreeIPAChildEnvironmentTestDto.class)
                .when(freeIPATestClient.attachChildEnvironment())
                .then(FreeIpaChildEnvironmentAssertion.validate())
                .validate();
    }
}
