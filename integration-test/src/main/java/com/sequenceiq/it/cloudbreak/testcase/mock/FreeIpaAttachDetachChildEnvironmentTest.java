package com.sequenceiq.it.cloudbreak.testcase.mock;

import jakarta.inject.Inject;
import jakarta.ws.rs.BadRequestException;

import org.testng.annotations.Test;

import com.sequenceiq.environment.api.v1.environment.model.response.EnvironmentStatus;
import com.sequenceiq.it.cloudbreak.assertion.freeipa.FreeIpaChildEnvironmentAssertion;
import com.sequenceiq.it.cloudbreak.client.EnvironmentTestClient;
import com.sequenceiq.it.cloudbreak.client.FreeIpaTestClient;
import com.sequenceiq.it.cloudbreak.context.Description;
import com.sequenceiq.it.cloudbreak.context.MockedTestContext;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.environment.EnvironmentTestDto;
import com.sequenceiq.it.cloudbreak.dto.freeipa.FreeIpaChildEnvironmentTestDto;
import com.sequenceiq.it.cloudbreak.dto.freeipa.FreeIpaTestDto;

public class FreeIpaAttachDetachChildEnvironmentTest extends AbstractMockTest {

    @Inject
    private FreeIpaTestClient freeIpaTestClient;

    @Inject
    private EnvironmentTestClient environmentTestClient;

    protected void setupTest(TestContext testContext) {
        createDefaultUser(testContext);
        createDefaultCredential(testContext);
        createDefaultEnvironment(testContext);
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
                .given(FreeIpaTestDto.class)
                .when(freeIpaTestClient.create())
                .awaitForCreationFlow()
                .given(FreeIpaChildEnvironmentTestDto.CHILD_ENVIRONMENT_KEY, EnvironmentTestDto.class)
                    .withNetwork()
                    .withCreateFreeIpa(Boolean.FALSE)
                .when(environmentTestClient.create())
                .await(EnvironmentStatus.AVAILABLE)
                .given(FreeIpaChildEnvironmentTestDto.class)
                .when(freeIpaTestClient.attachChildEnvironment())
                .then(FreeIpaChildEnvironmentAssertion.validateChildFreeipa())
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    @Description(
            given = "child and parent environments are present with attached freeipas",
            when = "calling a freeipa delete on parent environment",
            then = "it should fail")
    public void testParentFreeIpaDeleteFailure(MockedTestContext testContext) {
        testContext
                .given(FreeIpaTestDto.class)
                .when(freeIpaTestClient.create())
                .awaitForCreationFlow()
                .given(FreeIpaChildEnvironmentTestDto.CHILD_ENVIRONMENT_KEY, EnvironmentTestDto.class)
                    .withNetwork()
                    .withCreateFreeIpa(Boolean.FALSE)
                .when(environmentTestClient.create())
                .await(EnvironmentStatus.AVAILABLE)
                .given(FreeIpaChildEnvironmentTestDto.class)
                .when(freeIpaTestClient.attachChildEnvironment())
                .given(FreeIpaTestDto.class)
                .whenException(freeIpaTestClient.delete(), BadRequestException.class)
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
                .given(FreeIpaTestDto.class)
                .when(freeIpaTestClient.create())
                .awaitForCreationFlow()
                .given(FreeIpaChildEnvironmentTestDto.CHILD_ENVIRONMENT_KEY, EnvironmentTestDto.class)
                    .withNetwork()
                    .withCreateFreeIpa(Boolean.FALSE)
                .when(environmentTestClient.create())
                .await(EnvironmentStatus.AVAILABLE)
                .given(FreeIpaChildEnvironmentTestDto.class)
                .when(freeIpaTestClient.attachChildEnvironment())
                .when(freeIpaTestClient.detachChildEnvironment())
                .given(FreeIpaChildEnvironmentTestDto.class)
                .then(FreeIpaChildEnvironmentAssertion.validateNoFreeipa())
                .given(FreeIpaTestDto.class)
                .when(freeIpaTestClient.delete())
                .validate();
    }
}
