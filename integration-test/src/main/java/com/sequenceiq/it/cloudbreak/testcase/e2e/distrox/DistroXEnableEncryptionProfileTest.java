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

public class DistroXEnableEncryptionProfileTest extends AbstractE2ETest {

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
            when = "encryption profile is enabled on the environment",
            then = "datalake and datahub use the same TLS version and ciphers from the custom encryption profile")
    public void testEnableEncryptionProfileInDistroX(TestContext testContext) {
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
                .withOneFreeIpaNode()
                .when(environmentTestClient.create())
                .awaitForCreationFlow()
                .given(FreeIpaUserSyncTestDto.class)
                .when(freeIpaTestClient.getLastSyncOperationStatus())
                .await(OperationState.COMPLETED)
                .given(EnvironmentTestDto.class)
                .when(environmentTestClient.describe())
                .given(SdxTestDto.class)
                .withCloudStorage()
                .withEnvironment()
                .withRuntimeVersion(VERSION_7_3_2)
                .when(sdxTestClient.create())
                .await(SdxClusterStatusResponse.RUNNING)
                .awaitForHealthyInstances()
                .given(DistroXTestDto.class)
                .withTemplate(commonClusterManagerProperties().getDataEngDistroXBlueprintName(VERSION_7_3_2))
                .withEnvironment()
                .when(distroXTestClient.create())
                .await(STACK_AVAILABLE)
                .awaitForHealthyInstances()
                .given(EnvironmentTestDto.class)
                .withEncryptionProfile(encryptionProfileName)
                .when(environmentTestClient.enableEncryptionProfileOnEnvironment())
                .awaitForFlow()
                .given(FreeIpaTestDto.class)
                .when(freeIpaTestClient.describe())
                .then((tc, testDto, client) -> encryptionProfileAssertion.assertTls13EncryptionProfile(testDto))
                .given(SdxTestDto.class)
                .await(SdxClusterStatusResponse.RUNNING)
                .awaitForHealthyInstances()
                .then((tc, testDto, client) -> encryptionProfileAssertion.assertTls13EncryptionProfile(testDto))
                .given(DistroXTestDto.class)
                .await(STACK_AVAILABLE)
                .awaitForHealthyInstances()
                .then((tc, testDto, client) -> encryptionProfileAssertion.assertTls13EncryptionProfile(testDto))
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT)
    @UseSpotInstances
    @Description(
            given = "there is a running cloudbreak",
            when = "encryption profile is enabled on the datalake and a different one on the datahub",
            then = "datalake and datahub use different TLS version and ciphers from the custom encryption profiles")
    public void testEnableDifferentEncryptionProfilesInSdxAndDistroX(TestContext testContext) {
        String envEncryptionProfileName = "encryption-profile-env" + UUID.randomUUID();
        String dlEncryptionProfileName = "encryption-profile-dl" + UUID.randomUUID();
        String distroXEncryptionProfileName = "encryption-profile-dx" + UUID.randomUUID();

        testContext
                .given(CredentialTestDto.class)
                .when(credentialTestClient.create())
                .given(envEncryptionProfileName, EncryptionProfileTestDto.class)
                .withName(envEncryptionProfileName)
                .withTlsVersions(Set.of(TlsVersion.TLS_1_2))
                .withCipherSuites(List.of("TLS_ECDHE_RSA_WITH_AES_256_GCM_SHA384", "TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256"))
                .when(encryptionProfileTestClient.create())
                .given(dlEncryptionProfileName, EncryptionProfileTestDto.class)
                .withName(dlEncryptionProfileName)
                .withTlsVersions(Set.of(TlsVersion.TLS_1_3))
                .withCipherSuites(List.of("TLS_AES_256_GCM_SHA384"))
                .when(encryptionProfileTestClient.create())
                .given(distroXEncryptionProfileName, EncryptionProfileTestDto.class)
                .withName(distroXEncryptionProfileName)
                .withTlsVersions(Set.of(TlsVersion.TLS_1_2, TlsVersion.TLS_1_3))
                .withCipherSuites(List.of("TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256", "TLS_AES_256_GCM_SHA384"))
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
                .withOneFreeIpaNode()
                // TODO Uncomment after CB-34283 is fixed.
                // .withEncryptionProfile(envEncryptionProfileName)
                .when(environmentTestClient.create())
                .awaitForCreationFlow()
                .given(FreeIpaUserSyncTestDto.class)
                .when(freeIpaTestClient.getLastSyncOperationStatus())
                .await(OperationState.COMPLETED)
                .given(FreeIpaTestDto.class)
                .when(freeIpaTestClient.describe())
                // TODO Uncomment after CB-34283 is fixed.
                // .then((tc, testDto, client) -> encryptionProfileAssertion.assertTls12EncryptionProfile(testDto))
                .given(EnvironmentTestDto.class)
                .when(environmentTestClient.describe())
                .given(SdxTestDto.class)
                .withCloudStorage()
                .withEnvironment()
                .withRuntimeVersion(VERSION_7_3_2)
                .when(sdxTestClient.create())
                .await(SdxClusterStatusResponse.RUNNING)
                .awaitForHealthyInstances()
                // TODO Uncomment after CB-34283 is fixed.
                // .then((tc, testDto, client) -> encryptionProfileAssertion.assertTls12EncryptionProfile(testDto))
                .given(DistroXTestDto.class)
                .withTemplate(commonClusterManagerProperties().getDataEngDistroXBlueprintName(VERSION_7_3_2))
                .withEnvironment()
                .when(distroXTestClient.create())
                .await(STACK_AVAILABLE)
                .awaitForHealthyInstances()
                // TODO Uncomment after CB-34283 is fixed.
                // .then((tc, testDto, client) -> encryptionProfileAssertion.assertTls12EncryptionProfile(testDto))
                .given(SdxTestDto.class)
                .withEncryptionProfile(dlEncryptionProfileName)
                .when(sdxTestClient.enableEncryptionProfileOnDatalake())
                .awaitForFlow()
                .await(SdxClusterStatusResponse.RUNNING)
                .awaitForHealthyInstances()
                .then((tc, testDto, client) -> encryptionProfileAssertion.assertTls13EncryptionProfile(testDto))
                .given(DistroXTestDto.class)
                .withEncryptionProfile(distroXEncryptionProfileName)
                .when(distroXTestClient.updateSslConfigurations())
                .awaitForFlow()
                .await(STACK_AVAILABLE)
                .awaitForHealthyInstances()
                .then((tc, testDto, client) -> encryptionProfileAssertion.assertTls12Tls13EncryptionProfile(testDto))
                .validate();
    }
}
