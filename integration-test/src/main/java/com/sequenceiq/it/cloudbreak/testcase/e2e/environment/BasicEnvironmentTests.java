package com.sequenceiq.it.cloudbreak.testcase.e2e.environment;

import javax.inject.Inject;

import org.testng.annotations.Test;

import com.sequenceiq.environment.api.v1.environment.model.response.EnvironmentStatus;
import com.sequenceiq.it.cloudbreak.client.CredentialTestClient;
import com.sequenceiq.it.cloudbreak.client.EnvironmentTestClient;
import com.sequenceiq.it.cloudbreak.context.Description;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.credential.CredentialTestDto;
import com.sequenceiq.it.cloudbreak.dto.environment.EnvironmentTestDto;
import com.sequenceiq.it.cloudbreak.dto.telemetry.TelemetryTestDto;
import com.sequenceiq.it.cloudbreak.testcase.e2e.AbstractE2ETest;

public class BasicEnvironmentTests extends AbstractE2ETest {

    @Inject
    private EnvironmentTestClient environmentTestClient;

    @Inject
    private CredentialTestClient credentialTestClient;

    @Override
    protected void setupTest(TestContext testContext) {
        createDefaultUser(testContext);
        initializeDefaultBlueprints(testContext);
    }

    @Test(dataProvider = TEST_CONTEXT)
    @Description(
            given = "there is a running cloudbreak",
            when = "valid create environment request is sent",
            then = "environment should be created")
    public void testCreateEnvironment(TestContext testContext) {
        testContext
                .given(CredentialTestDto.class)
                .when(credentialTestClient.create())
                .given("telemetry", TelemetryTestDto.class)
                .withLogging()
                .given(EnvironmentTestDto.class)
                .withNetwork()
                .withTelemetry("telemetry")
                .withCreateFreeIpa(Boolean.TRUE)
                .when(environmentTestClient.create())
                .await(EnvironmentStatus.AVAILABLE)
                .then((tc, testDto, cc) -> environmentTestClient.describe().action(tc, testDto, cc))
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT)
    @Description(
            given = "there is an available environment",
            when = "a delete request is sent for the environment",
            then = "the environment should be deleted")
    public void testForceDeleteEnvironment(TestContext testContext) {
        testContext
                .given(CredentialTestDto.class)
                .when(credentialTestClient.create())
                .given("telemetry", TelemetryTestDto.class)
                .withLogging()
                .given(EnvironmentTestDto.class)
                .withNetwork()
                .withTelemetry("telemetry")
                .withCreateFreeIpa(Boolean.TRUE)
                .when(environmentTestClient.create())
                .await(EnvironmentStatus.AVAILABLE)
                .then((tc, testDto, cc) -> environmentTestClient.describe().action(tc, testDto, cc))
                .when(environmentTestClient.deleteForced())
                .validate();
    }
}
