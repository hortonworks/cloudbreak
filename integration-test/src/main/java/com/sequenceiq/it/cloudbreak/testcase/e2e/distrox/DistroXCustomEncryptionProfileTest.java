package com.sequenceiq.it.cloudbreak.testcase.e2e.distrox;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import jakarta.inject.Inject;

import org.testng.annotations.Test;

import com.sequenceiq.common.api.encryptionprofile.TlsVersion;
import com.sequenceiq.freeipa.api.v1.operation.model.OperationState;
import com.sequenceiq.it.cloudbreak.assertion.stack.EncryptionProfileAssertion;
import com.sequenceiq.it.cloudbreak.client.CredentialTestClient;
import com.sequenceiq.it.cloudbreak.client.DistroXTestClient;
import com.sequenceiq.it.cloudbreak.client.EncryptionProfileTestClient;
import com.sequenceiq.it.cloudbreak.client.EnvironmentTestClient;
import com.sequenceiq.it.cloudbreak.client.FreeIpaTestClient;
import com.sequenceiq.it.cloudbreak.client.SdxTestClient;
import com.sequenceiq.it.cloudbreak.context.Description;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.credential.CredentialTestDto;
import com.sequenceiq.it.cloudbreak.dto.distrox.DistroXTestDto;
import com.sequenceiq.it.cloudbreak.dto.environment.EncryptionProfileTestDto;
import com.sequenceiq.it.cloudbreak.dto.environment.EnvironmentNetworkTestDto;
import com.sequenceiq.it.cloudbreak.dto.environment.EnvironmentTestDto;
import com.sequenceiq.it.cloudbreak.dto.freeipa.FreeIpaTestDto;
import com.sequenceiq.it.cloudbreak.dto.freeipa.FreeIpaUserSyncTestDto;
import com.sequenceiq.it.cloudbreak.dto.sdx.SdxTestDto;
import com.sequenceiq.it.cloudbreak.dto.telemetry.TelemetryTestDto;
import com.sequenceiq.it.cloudbreak.testcase.e2e.AbstractE2ETest;
import com.sequenceiq.it.cloudbreak.util.spot.UseSpotInstances;
import com.sequenceiq.sdx.api.model.SdxClusterStatusResponse;

public class DistroXCustomEncryptionProfileTest extends AbstractE2ETest {

    private static final String VERSION_7_3_2 = "7.3.2";

    @Inject
    private DistroXTestClient distroXTestClient;

    @Inject
    private EncryptionProfileTestClient encryptionProfileTestClient;

    @Inject
    private EnvironmentTestClient environmentTestClient;

    @Inject
    private SdxTestClient sdxTestClient;

    @Inject
    private CredentialTestClient credentialTestClient;

    @Inject
    private FreeIpaTestClient freeIpaTestClient;

    @Inject
    private EncryptionProfileAssertion encryptionProfileAssertion;

    @Override
    protected void setupTest(TestContext testContext) {
        createDefaultUser(testContext);
    }

    @Test(dataProvider = TEST_CONTEXT)
    @UseSpotInstances
    @Description(
            given = "there is a running cloudbreak",
            when = "a custom encryption profile is used to create the environment",
            then = "datalake and datahub uses the same TLS version from the custom encryption profile")
    public void testCreateDistroXWithCustomEncryptionProfile(TestContext testContext) {
        String encryptionProfileName = "encryption-profile-" + UUID.randomUUID();
        testContext
                .given(CredentialTestDto.class)
                .when(credentialTestClient.create())
                .given(encryptionProfileName, EncryptionProfileTestDto.class)
                .withName(encryptionProfileName)
                .withTlsVersions(Set.of(TlsVersion.TLS_1_3))
                .withCipherSuites(List.of("TLS_AES_256_GCM_SHA384"))
                .when(encryptionProfileTestClient.create())
                .given(EnvironmentNetworkTestDto.class)
                .given("telemetry", TelemetryTestDto.class)
                .withLogging()
                .withReportClusterLogs()
                .given(EnvironmentTestDto.class)
                .withNetwork()
                .withTelemetry("telemetry")
                .withTunnel(testContext.getTunnel())
                .withCreateFreeIpa(Boolean.TRUE)
                .withEncryptionProfile(encryptionProfileName)
                .when(environmentTestClient.create())
                .awaitForCreationFlow()
                .given(FreeIpaUserSyncTestDto.class)
                .when(freeIpaTestClient.getLastSyncOperationStatus())
                .await(OperationState.COMPLETED)
                .given(FreeIpaTestDto.class)
                .when(freeIpaTestClient.describe())
                .then((tc, testDto, client) -> encryptionProfileAssertion.assertTls13EncryptionProfile(testDto))
                .given(EnvironmentTestDto.class)
                .when(environmentTestClient.describe())
                .given(SdxTestDto.class)
                .withCloudStorage()
                .withEnvironment()
                .withRuntimeVersion(VERSION_7_3_2)
                .when(sdxTestClient.create())
                .await(SdxClusterStatusResponse.RUNNING)
                .awaitForHealthyInstances()
                .then((tc, testDto, client) -> encryptionProfileAssertion.assertTls13EncryptionProfile(testDto))
                .given(DistroXTestDto.class)
                .withTemplate(commonClusterManagerProperties().getDataEngDistroXBlueprintName(VERSION_7_3_2))
                .withEnvironment()
                .when(distroXTestClient.create())
                .await(STACK_AVAILABLE)
                .awaitForHealthyInstances()
                .then((tc, testDto, client) -> encryptionProfileAssertion.assertTls13EncryptionProfile(testDto))
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT)
    @UseSpotInstances
    @Description(
            given = "there is a running cloudbreak",
            when = "a default encryption profile is used to create the environment",
            then = "datalake and datahub uses the same TLS version from the default encryption profile with TLSv1.2 and TLSv1.3")
    public void testCreateDistroXWithDefaultEncryptionProfile(TestContext testContext) {
        testContext
                .given(CredentialTestDto.class)
                .when(credentialTestClient.create())
                .given(EnvironmentNetworkTestDto.class)
                .given("telemetry", TelemetryTestDto.class)
                .withLogging()
                .withReportClusterLogs()
                .given(EnvironmentTestDto.class)
                .withNetwork()
                .withTelemetry("telemetry")
                .withTunnel(testContext.getTunnel())
                .withCreateFreeIpa(Boolean.TRUE)
                .withEncryptionProfile(testContext.getTestContext().getCloudProvider().defaultEncryptionProfile())
                .when(environmentTestClient.create())
                .awaitForCreationFlow()
                .given(FreeIpaUserSyncTestDto.class)
                .when(freeIpaTestClient.getLastSyncOperationStatus())
                .await(OperationState.COMPLETED)
                .given(FreeIpaTestDto.class)
                .when(freeIpaTestClient.describe())
                .then((tc, testDto, client) -> encryptionProfileAssertion.assertDefaultEncryptionProfile(testDto))
                .given(EnvironmentTestDto.class)
                .when(environmentTestClient.describe())
                .given(SdxTestDto.class)
                .withCloudStorage()
                .withEnvironment()
                .withRuntimeVersion(VERSION_7_3_2)
                .when(sdxTestClient.create())
                .await(SdxClusterStatusResponse.RUNNING)
                .awaitForHealthyInstances()
                .then((tc, testDto, client) -> encryptionProfileAssertion.assertDefaultEncryptionProfile(testDto))
                .given(DistroXTestDto.class)
                .withTemplate(commonClusterManagerProperties().getDataEngDistroXBlueprintName(VERSION_7_3_2))
                .withEnvironment()
                .when(distroXTestClient.create())
                .await(STACK_AVAILABLE)
                .awaitForHealthyInstances()
                .then((tc, testDto, client) -> encryptionProfileAssertion.assertDefaultEncryptionProfile(testDto))
                .validate();
    }
}
