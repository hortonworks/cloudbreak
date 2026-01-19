package com.sequenceiq.it.cloudbreak.testcase.e2e.distrox;

import java.util.List;
import java.util.Set;

import jakarta.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.Test;

import com.sequenceiq.cloudbreak.rotation.CloudbreakSecretType;
import com.sequenceiq.freeipa.rotation.FreeIpaSecretType;
import com.sequenceiq.it.cloudbreak.client.DistroXTestClient;
import com.sequenceiq.it.cloudbreak.client.EnvironmentTestClient;
import com.sequenceiq.it.cloudbreak.client.FreeIpaTestClient;
import com.sequenceiq.it.cloudbreak.client.SdxTestClient;
import com.sequenceiq.it.cloudbreak.context.Description;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.distrox.DistroXTestDto;
import com.sequenceiq.it.cloudbreak.dto.environment.EnvironmentAuthenticationTestDto;
import com.sequenceiq.it.cloudbreak.dto.environment.EnvironmentTestDto;
import com.sequenceiq.it.cloudbreak.dto.freeipa.FreeIpaRotationTestDto;
import com.sequenceiq.it.cloudbreak.dto.freeipa.FreeIpaTestDto;
import com.sequenceiq.it.cloudbreak.dto.sdx.SdxInternalTestDto;
import com.sequenceiq.it.cloudbreak.log.Log;
import com.sequenceiq.it.cloudbreak.testcase.e2e.AbstractE2ETest;
import com.sequenceiq.it.cloudbreak.util.SecretRotationCheckUtil;
import com.sequenceiq.it.cloudbreak.util.spot.UseSpotInstances;
import com.sequenceiq.sdx.rotation.DatalakeSecretType;

public class DistroXSecretRotationTests extends AbstractE2ETest {
    private static final Logger LOGGER = LoggerFactory.getLogger(DistroXSecretRotationTests.class);

    @Inject
    private SdxTestClient sdxTestClient;

    @Inject
    private DistroXTestClient distroXTestClient;

    @Inject
    private EnvironmentTestClient environmentTestClient;

    @Inject
    private FreeIpaTestClient freeIpaTestClient;

    @Inject
    private SecretRotationCheckUtil secretRotationCheckUtil;

    @Override
    protected void setupTest(TestContext testContext) {
        testContext.getCloudProvider().getCloudFunctionality().cloudStorageInitialize();
        createDefaultUser(testContext);
        createDefaultCredential(testContext);
        initializeDefaultBlueprints(testContext);
        createDefaultDatahubWithAutoTlsAndExternalDb(testContext);
    }

    @Test(dataProvider = TEST_CONTEXT)
    @UseSpotInstances
    @Description(
            given = "there is an environment with DistroX in available state",
            when = "keypair secrets are getting rotated if the required test parameters are present",
            then = "rotation should be successful, the cluster should be available"
    )
    public void testSSHKeyPairRotation(TestContext testContext) {
        if (StringUtils.isBlank(commonCloudProperties().getRotationSshPublicKey()) ||
                StringUtils.isBlank(commonCloudProperties().getRotationPrivateKeyFile())) {
            Log.log(LOGGER, "SSH key rotation skipped because parameters are not found " +
                    "(integrationtest.rotationSshPublicKey, integrationtest.rotationPrivateKeyFile).");
        } else {
            testContext
                    .given(EnvironmentAuthenticationTestDto.class)
                    .withPublicKey(commonCloudProperties().getRotationSshPublicKey())
                    .given(EnvironmentTestDto.class)
                    .when(environmentTestClient.changeAuthentication())
                    .given(FreeIpaTestDto.class)
                    .given(FreeIpaRotationTestDto.class)
                        .withSecrets(List.of(FreeIpaSecretType.USER_KEYPAIR))
                    .when(freeIpaTestClient.rotateSecret())
                    .awaitForFlow()
                    .given(SdxInternalTestDto.class)
                    .when(sdxTestClient.rotateSecret(Set.of(DatalakeSecretType.USER_KEYPAIR)))
                    .awaitForFlow()
                    .then((tc, testDto, client) -> {
                        secretRotationCheckUtil.checkSSHLoginWithNewKeys(testDto.getCrn(), client, tc);
                        return testDto;
                    })
                    .given(DistroXTestDto.class)
                    .when(distroXTestClient.rotateSecret(Set.of(CloudbreakSecretType.USER_KEYPAIR)))
                    .awaitForFlow()
                    .then((tc, testDto, client) -> {
                        secretRotationCheckUtil.checkSSHLoginWithNewKeys(tc, testDto, client);
                        return testDto;
                    })
                    .validate();
        }
    }
}
