package com.sequenceiq.it.cloudbreak.testcase.e2e.gov;

import java.util.List;
import java.util.Set;

import jakarta.inject.Inject;

import org.testng.annotations.Test;

import com.sequenceiq.cloudbreak.rotation.CloudbreakSecretType;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.AvailabilityType;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.Status;
import com.sequenceiq.freeipa.rotation.FreeIpaSecretType;
import com.sequenceiq.it.cloudbreak.assertion.encryption.SecretEncryptionAssertions;
import com.sequenceiq.it.cloudbreak.context.Description;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.distrox.DistroXTestDto;
import com.sequenceiq.it.cloudbreak.dto.freeipa.FreeIpaDownscaleTestDto;
import com.sequenceiq.it.cloudbreak.dto.freeipa.FreeIpaRotationTestDto;
import com.sequenceiq.it.cloudbreak.dto.freeipa.FreeIpaTestDto;
import com.sequenceiq.it.cloudbreak.dto.freeipa.FreeIpaUpscaleTestDto;
import com.sequenceiq.it.cloudbreak.dto.sdx.SdxInternalTestDto;
import com.sequenceiq.it.cloudbreak.util.spot.UseSpotInstances;
import com.sequenceiq.sdx.api.model.SdxClusterStatusResponse;
import com.sequenceiq.sdx.rotation.DatalakeSecretType;

public class SecretEncryptionTests extends PreconditionGovTest {

    @Inject
    private SecretEncryptionAssertions secretEncryptionAssertions;

    @Test(dataProvider = TEST_CONTEXT)
    @Description(
            given = "a running cloudbreak",
            when = "a valid environment request is sent with CCM2 and FreeIpa " +
                    "AND the stack is stopped " +
                    "AND the stack is started " +
                    "AND the stack is upscaled to HA " +
                    "AND the stack is downscaled to TWO_NODE_BASED",
            then = "the stack should be available AND deletable"
    )
    public void testCreateStopStartUpscaleDownscaleFreeIpaWithSecretEncryption(TestContext testContext) {
        createEnvironmentWithFreeIpa(testContext);

        testContext
                .given(FreeIpaTestDto.class)
                .then(secretEncryptionAssertions::validate)

                .when(getFreeIpaTestClient().stop())
                .await(Status.STOPPED)
                .when(getFreeIpaTestClient().start())
                .await(Status.AVAILABLE)
                .awaitForHealthyInstances()
                .then(secretEncryptionAssertions::validate)

                .given(FreeIpaUpscaleTestDto.class)
                .withAvailabilityType(AvailabilityType.HA)
                .when(getFreeIpaTestClient().upscale())
                .await(Status.AVAILABLE)
                .given(FreeIpaTestDto.class)
                .awaitForHealthyInstances()
                .then(secretEncryptionAssertions::validate)

                .given(FreeIpaDownscaleTestDto.class)
                .withAvailabilityType(AvailabilityType.TWO_NODE_BASED)
                .when(getFreeIpaTestClient().downscale())
                .await(Status.AVAILABLE)
                .given(FreeIpaTestDto.class)
                .awaitForHealthyInstances()
                .then(secretEncryptionAssertions::validate)

                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT)
    @UseSpotInstances
    @Description(
            given = "an environment which has secret encryption and CCMv2 enabled and has a FreeIpa, a Data Lake and a Data Hub",
            when = "we rotate the rotate the STACK_ENCRYPTION_KEYS and LUKS_VOLUME_PASSPHRASEs of each stack",
            then = "the rotations should be successful and each stack should be in a healthy state"
    )
    public void testSecretRotationWithSecretEncryption(TestContext testContext) {
        createEnvironmentWithFreeIpa(testContext);
        testContext
                .given(FreeIpaTestDto.class)
                .then(secretEncryptionAssertions::validate)
                .given(FreeIpaRotationTestDto.class)
                .withSecrets(List.of(FreeIpaSecretType.STACK_ENCRYPTION_KEYS, FreeIpaSecretType.LUKS_VOLUME_PASSPHRASE))
                .when(getFreeIpaTestClient().rotateSecret())
                .awaitForFlow()
                .given(FreeIpaTestDto.class)
                .await(Status.AVAILABLE)
                .awaitForHealthyInstances()
                .then(secretEncryptionAssertions::validate)
                .validate();

        createDatalake(testContext);
        testContext
                .given(SdxInternalTestDto.class)
                .then(secretEncryptionAssertions::validate)
                .when(getSdxTestClient().rotateSecret(Set.of(DatalakeSecretType.STACK_ENCRYPTION_KEYS, DatalakeSecretType.LUKS_VOLUME_PASSPHRASE)))
                .await(SdxClusterStatusResponse.RUNNING)
                .awaitForHealthyInstances()
                .then(secretEncryptionAssertions::validate)
                .validate();

        createDefaultDatahubForExistingDatalake(testContext);
        testContext
                .given(DistroXTestDto.class)
                .then(secretEncryptionAssertions::validate)
                .when(getDistroXTestClient().rotateSecret(Set.of(CloudbreakSecretType.STACK_ENCRYPTION_KEYS, CloudbreakSecretType.LUKS_VOLUME_PASSPHRASE)))
                .await(STACK_AVAILABLE)
                .awaitForHealthyInstances()
                .then(secretEncryptionAssertions::validate)
                .validate();
    }
}
