package com.sequenceiq.it.cloudbreak.testcase.mock;

import javax.inject.Inject;
import javax.ws.rs.BadRequestException;

import org.testng.annotations.Test;

import com.sequenceiq.environment.api.v1.environment.model.response.EnvironmentStatus;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.Status;
import com.sequenceiq.it.cloudbreak.assertion.freeipa.FreeIpaChildEnvironmentAssertion;
import com.sequenceiq.it.cloudbreak.client.EnvironmentTestClient;
import com.sequenceiq.it.cloudbreak.client.FreeIPATestClient;
import com.sequenceiq.it.cloudbreak.context.Description;
import com.sequenceiq.it.cloudbreak.context.MockedTestContext;
import com.sequenceiq.it.cloudbreak.context.RunningParameter;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.environment.EnvironmentTestDto;
import com.sequenceiq.it.cloudbreak.dto.freeipa.FreeIPAChildEnvironmentTestDto;
import com.sequenceiq.it.cloudbreak.dto.freeipa.FreeIPATestDto;
import com.sequenceiq.it.cloudbreak.testcase.AbstractIntegrationTest;

public class FreeIpaAttachChildEnvironmentTest extends AbstractIntegrationTest {

    @Inject
    private FreeIPATestClient freeIPATestClient;

    @Inject
    private EnvironmentTestClient environmentTestClient;

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
            then = "child environment uses the parent's freeipa")
    public void testAttachChildEnvironment(MockedTestContext testContext) {
        testContext
                .given(FreeIPATestDto.class)
                    .withCatalog(testContext.getImageCatalogMockServerSetup().getFreeIpaImageCatalogUrl())
                .when(freeIPATestClient.create())
                .await(Status.AVAILABLE)
                .given(FreeIPAChildEnvironmentTestDto.CHILD_ENVIRONMENT_KEY, EnvironmentTestDto.class)
                    .withNetwork()
                    .withCreateFreeIpa(Boolean.FALSE)
                .when(environmentTestClient.create())
                .await(EnvironmentStatus.AVAILABLE)
                .given(FreeIPAChildEnvironmentTestDto.class)
                .when(freeIPATestClient.attachChildEnvironment())
                .then(FreeIpaChildEnvironmentAssertion.validateChildFreeipa())
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    @Description(
            given = "child and parent environments are present with attached freeipas",
            when = "calling a freeipa delete on parent environment",
            then = "it should fail")
    public void testParentFreeIpaDeleteFailure(MockedTestContext testContext) {
        String key = resourcePropertyProvider().getName();
        testContext
                .given(FreeIPATestDto.class)
                    .withCatalog(testContext.getImageCatalogMockServerSetup().getFreeIpaImageCatalogUrl())
                .when(freeIPATestClient.create())
                .await(Status.AVAILABLE)
                .given(FreeIPAChildEnvironmentTestDto.CHILD_ENVIRONMENT_KEY, EnvironmentTestDto.class)
                    .withNetwork()
                    .withCreateFreeIpa(Boolean.FALSE)
                .when(environmentTestClient.create())
                .await(EnvironmentStatus.AVAILABLE)
                .given(FreeIPAChildEnvironmentTestDto.class)
                .when(freeIPATestClient.attachChildEnvironment())
                .given(FreeIPATestDto.class)
                .when(freeIPATestClient.delete(), RunningParameter.key(key))
                .expect(BadRequestException.class, RunningParameter.key(key))
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    @Description(
            given = "child and parent environments are present with attached freeipas",
            when = "calling a freeipa detach",
            and = "deleting parent freeipa",
            then = "it should succeed")
    public void testParentFreeIpaDeleteSuccess(MockedTestContext testContext) {
        testContext
                .given(FreeIPATestDto.class)
                    .withCatalog(testContext.getImageCatalogMockServerSetup().getFreeIpaImageCatalogUrl())
                .when(freeIPATestClient.create())
                .await(Status.AVAILABLE)
                .given(FreeIPAChildEnvironmentTestDto.CHILD_ENVIRONMENT_KEY, EnvironmentTestDto.class)
                    .withNetwork()
                    .withCreateFreeIpa(Boolean.FALSE)
                .when(environmentTestClient.create())
                .await(EnvironmentStatus.AVAILABLE)
                .given(FreeIPAChildEnvironmentTestDto.class)
                .when(freeIPATestClient.attachChildEnvironment())
                .when(freeIPATestClient.detachChildEnvironment())
                .given(FreeIPAChildEnvironmentTestDto.class)
                .then(FreeIpaChildEnvironmentAssertion.validateNoFreeipa())
                .given(FreeIPATestDto.class)
                .when(freeIPATestClient.delete())
                .validate();
    }
}
