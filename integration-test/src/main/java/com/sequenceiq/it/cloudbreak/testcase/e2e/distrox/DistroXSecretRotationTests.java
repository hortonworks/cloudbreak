package com.sequenceiq.it.cloudbreak.testcase.e2e.distrox;

import static com.sequenceiq.cloudbreak.rotation.CloudbreakSecretType.USER_KEYPAIR;
import static com.sequenceiq.sdx.rotation.DatalakeSecretType.DATALAKE_USER_KEYPAIR;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.Test;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.instancegroup.InstanceGroupV4Response;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.it.cloudbreak.client.DistroXTestClient;
import com.sequenceiq.it.cloudbreak.client.EnvironmentTestClient;
import com.sequenceiq.it.cloudbreak.client.SdxTestClient;
import com.sequenceiq.it.cloudbreak.context.Description;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.distrox.DistroXTestDto;
import com.sequenceiq.it.cloudbreak.dto.environment.EnvironmentAuthenticationTestDto;
import com.sequenceiq.it.cloudbreak.dto.environment.EnvironmentTestDto;
import com.sequenceiq.it.cloudbreak.dto.sdx.SdxInternalTestDto;
import com.sequenceiq.it.cloudbreak.exception.TestFailException;
import com.sequenceiq.it.cloudbreak.log.Log;
import com.sequenceiq.it.cloudbreak.microservice.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.microservice.SdxClient;
import com.sequenceiq.it.cloudbreak.testcase.e2e.AbstractE2ETest;
import com.sequenceiq.it.cloudbreak.util.DistroxUtil;
import com.sequenceiq.it.cloudbreak.util.spot.UseSpotInstances;
import com.sequenceiq.it.cloudbreak.util.ssh.action.SshJClientActions;

public class DistroXSecretRotationTests extends AbstractE2ETest {
    private static final Logger LOGGER = LoggerFactory.getLogger(DistroXSecretRotationTests.class);

    @Inject
    private SdxTestClient sdxTestClient;

    @Inject
    private DistroXTestClient distroXTestClient;

    @Inject
    private DistroxUtil distroxUtil;

    @Inject
    private EnvironmentTestClient environmentTestClient;

    @Inject
    private SshJClientActions sshJClientActions;

    @Override
    protected void setupTest(TestContext testContext) {
        assertSupportedCloudPlatform(CloudPlatform.AWS);
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
            when = "SSH secrets are getting rotated if the required test parameters are present",
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
                    .given(SdxInternalTestDto.class)
                    .when(sdxTestClient.rotateSecret(Set.of(DATALAKE_USER_KEYPAIR)))
                    .awaitForFlow()
                    .then((tc, testDto, client) -> {
                        Collection<InstanceGroupV4Response> instanceGroupV4Responses = getInstanceGroupResponses(testDto.getCrn(), client);
                        checkSSHLoginWithNewKeys(instanceGroupV4Responses);
                        return testDto;
                    })
                    .given(DistroXTestDto.class)
                    .when(distroXTestClient.rotateSecret(Set.of(USER_KEYPAIR)))
                    .awaitForFlow()
                    .then((tc, testDto, client) -> {
                        Collection<InstanceGroupV4Response> instanceGroupV4Responses = getInstanceGroupResponses(tc, testDto, client);
                        checkSSHLoginWithNewKeys(instanceGroupV4Responses);
                        return testDto;
                    })
                    .validate();
        }
    }

    private void checkSSHLoginWithNewKeys(Collection<InstanceGroupV4Response> instanceGroupV4Responses) {
        String checkCommand = "echo \"SSH login check\"";
        Map<String, Pair<Integer, String>> sshCommandResponse =
                sshJClientActions.executeSshCommandOnAllHosts(
                        instanceGroupV4Responses,
                        checkCommand, false,
                        commonCloudProperties().getRotationPrivateKeyFile());
        boolean keyChangeWasSuccessfulOnAllNodes = sshCommandResponse.values().stream()
                .map(Entry::getValue)
                .allMatch(value -> value.contains("SSH login check"));
        if (!keyChangeWasSuccessfulOnAllNodes) {
            throw new TestFailException(String.format("SSH login check was not successful on all nodes. Checks: %s", sshCommandResponse));
        }
    }

    private Collection<InstanceGroupV4Response> getInstanceGroupResponses(String datalakeCrn, SdxClient client) {
        return client.getDefaultClient()
                .sdxEndpoint()
                .getDetailByCrn(datalakeCrn, Collections.emptySet())
                .getStackV4Response()
                .getInstanceGroups();
    }

    private Collection<InstanceGroupV4Response> getInstanceGroupResponses(TestContext testContext, DistroXTestDto testDto, CloudbreakClient client) {
        return client.getDefaultClient()
                .stackV4Endpoint()
                .get(client.getWorkspaceId(), testDto.getName(), Collections.emptySet(), testContext.getActingUserCrn().getAccountId())
                .getInstanceGroups();
    }
}
