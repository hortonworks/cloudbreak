package com.sequenceiq.it.cloudbreak.testcase.e2e.distrox;


import static com.sequenceiq.cloudbreak.rotation.CloudbreakSecretType.CLUSTER_CB_CM_ADMIN_PASSWORD;
import static com.sequenceiq.cloudbreak.rotation.CloudbreakSecretType.CLUSTER_CM_DB_PASSWORD;
import static com.sequenceiq.cloudbreak.rotation.CloudbreakSecretType.CLUSTER_CM_SERVICES_DB_PASSWORD;
import static com.sequenceiq.cloudbreak.rotation.CloudbreakSecretType.CLUSTER_MGMT_CM_ADMIN_PASSWORD;
import static com.sequenceiq.cloudbreak.rotation.CloudbreakSecretType.DATAHUB_CM_INTERMEDIATE_CA_CERT;
import static com.sequenceiq.cloudbreak.rotation.CloudbreakSecretType.DATAHUB_CM_SERVICE_SHARED_DB;
import static com.sequenceiq.cloudbreak.rotation.CloudbreakSecretType.DATAHUB_EXTERNAL_DATABASE_ROOT_PASSWORD;
import static com.sequenceiq.cloudbreak.rotation.CloudbreakSecretType.SALT_BOOT_SECRETS;
import static com.sequenceiq.cloudbreak.rotation.CloudbreakSecretType.USER_KEYPAIR;
import static com.sequenceiq.sdx.rotation.DatalakeSecretType.DATALAKE_CB_CM_ADMIN_PASSWORD;
import static com.sequenceiq.sdx.rotation.DatalakeSecretType.DATALAKE_CM_INTERMEDIATE_CA_CERT;
import static com.sequenceiq.sdx.rotation.DatalakeSecretType.DATALAKE_CM_SERVICE_SHARED_DB;
import static com.sequenceiq.sdx.rotation.DatalakeSecretType.DATALAKE_DATABASE_ROOT_PASSWORD;
import static com.sequenceiq.sdx.rotation.DatalakeSecretType.DATALAKE_MGMT_CM_ADMIN_PASSWORD;
import static com.sequenceiq.sdx.rotation.DatalakeSecretType.DATALAKE_SALT_BOOT_SECRETS;
import static com.sequenceiq.sdx.rotation.DatalakeSecretType.DATALAKE_USER_KEYPAIR;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.ITestContext;
import org.testng.annotations.Test;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.instancegroup.InstanceGroupV4Response;
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
import com.sequenceiq.it.cloudbreak.dto.sdx.SdxInternalTestDto;
import com.sequenceiq.it.cloudbreak.exception.TestFailException;
import com.sequenceiq.it.cloudbreak.log.Log;
import com.sequenceiq.it.cloudbreak.microservice.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.microservice.SdxClient;
import com.sequenceiq.it.cloudbreak.testcase.e2e.AbstractE2ETest;
import com.sequenceiq.it.cloudbreak.util.ssh.action.SshJClientActions;

public class DistroXSecretRotationTest extends AbstractE2ETest {
    private static final Logger LOGGER = LoggerFactory.getLogger(DistroXSecretRotationTest.class);

    @Inject
    private DistroXTestClient distroXTestClient;

    @Inject
    private SdxTestClient sdxTestClient;

    @Inject
    private FreeIpaTestClient freeIpaTestClient;

    @Inject
    private EnvironmentTestClient environmentTestClient;

    @Inject
    private SshJClientActions sshJClientActions;

    @Override
    protected void setupTest(TestContext testContext) {
        testContext.getCloudProvider().getCloudFunctionality().cloudStorageInitialize();
        createDefaultUser(testContext);
        createDefaultCredential(testContext);
        initializeDefaultBlueprints(testContext);
        createDefaultDatahubWithAutoTlsAndExternalDb(testContext);
    }

    @Test(dataProvider = TEST_CONTEXT)
    @Description(
            given = "there is a running default Distrox cluster",
            when = "secrets are getting rotated",
            then = "rotation should be successful and cluster should be available")
    public void testSecretRotation(TestContext testContext, ITestContext iTestContext) {
        testContext
                .given(SdxInternalTestDto.class)
                .when(sdxTestClient.rotateSecret(Set.of(
                        DATALAKE_SALT_BOOT_SECRETS,
                        DATALAKE_MGMT_CM_ADMIN_PASSWORD,
                        DATALAKE_CB_CM_ADMIN_PASSWORD,
                        DATALAKE_DATABASE_ROOT_PASSWORD)))
                .awaitForFlow()
                .given(DistroXTestDto.class)
                .when(distroXTestClient.rotateSecret(Set.of(
                        SALT_BOOT_SECRETS,
                        CLUSTER_MGMT_CM_ADMIN_PASSWORD,
                        CLUSTER_CB_CM_ADMIN_PASSWORD,
                        CLUSTER_CM_SERVICES_DB_PASSWORD,
                        CLUSTER_CM_DB_PASSWORD,
                        DATAHUB_EXTERNAL_DATABASE_ROOT_PASSWORD)))
                .awaitForFlow()
                .validate();
        testSSHKeyPairRotation(testContext);
    }

    @Test(dataProvider = TEST_CONTEXT)
    @Description(
            given = "there is a running default Distrox cluster",
            when = "CM shared DB multi secret are getting rotated",
            then = "rotation should be successful and clusters should be available")
    public void testCMSharedDbMultiSecretRotation(TestContext testContext, ITestContext iTestContext) {
        testContext
                .given(SdxInternalTestDto.class)
                .when(sdxTestClient.rotateSecret(Set.of(DATALAKE_CM_SERVICE_SHARED_DB)))
                .awaitForFlow()
                .given(DistroXTestDto.class)
                .when(distroXTestClient.rotateSecret(Set.of(DATAHUB_CM_SERVICE_SHARED_DB)))
                .awaitForFlow()
                .given(SdxInternalTestDto.class)
                .when(sdxTestClient.rotateSecret(Set.of(DATALAKE_CM_SERVICE_SHARED_DB)))
                .awaitForFlow()
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT)
    @Description(
            given = "there is a running default Distrox cluster",
            when = "root CA cert multi secret are getting rotated",
            then = "rotation should be successful and clusters should be available")
    public void testCacertMultiSecretRotation(TestContext testContext, ITestContext iTestContext) {
        testContext
                .given(FreeIpaRotationTestDto.class)
                .withSecrets(List.of(FreeIpaSecretType.FREEIPA_CA_CERT_RENEWAL.value()))
                .when(freeIpaTestClient.rotateSecret())
                .awaitForFlow()
                .given(SdxInternalTestDto.class)
                .when(sdxTestClient.rotateSecret(Set.of(DATALAKE_CM_INTERMEDIATE_CA_CERT)))
                .awaitForFlow()
                .given(DistroXTestDto.class)
                .when(distroXTestClient.rotateSecret(Set.of(DATAHUB_CM_INTERMEDIATE_CA_CERT)))
                .awaitForFlow()
                .given(FreeIpaRotationTestDto.class)
                .withSecrets(List.of(FreeIpaSecretType.FREEIPA_CA_CERT_RENEWAL.value()))
                .when(freeIpaTestClient.rotateSecret())
                .awaitForFlow()
                .validate();
    }

    private void testSSHKeyPairRotation(TestContext testContext) {
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
                .map(Map.Entry::getValue)
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