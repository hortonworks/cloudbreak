package com.sequenceiq.it.cloudbreak.testcase.e2e.freeipa;

import static com.sequenceiq.common.model.OsType.RHEL8;
import static com.sequenceiq.freeipa.rotation.FreeIpaSecretType.FREEIPA_ADMIN_PASSWORD;
import static com.sequenceiq.freeipa.rotation.FreeIpaSecretType.FREEIPA_USERSYNC_USER_PASSWORD;
import static com.sequenceiq.freeipa.rotation.FreeIpaSecretType.NGINX_CLUSTER_SSL_CERT_PRIVATE_KEY;
import static com.sequenceiq.freeipa.rotation.FreeIpaSecretType.SALT_BOOT_SECRETS;
import static com.sequenceiq.freeipa.rotation.FreeIpaSecretType.SALT_MASTER_KEY_PAIR;
import static com.sequenceiq.freeipa.rotation.FreeIpaSecretType.SALT_SIGN_KEY_PAIR;
import static com.sequenceiq.it.cloudbreak.context.RunningParameter.key;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

import org.apache.commons.lang3.tuple.Pair;
import org.testng.annotations.Test;

import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.Status;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.instance.InstanceGroupResponse;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.instance.InstanceMetaDataResponse;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.describe.DescribeFreeIpaResponse;
import com.sequenceiq.it.cloudbreak.client.FreeIpaTestClient;
import com.sequenceiq.it.cloudbreak.context.Description;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.freeipa.FreeIpaRotationTestDto;
import com.sequenceiq.it.cloudbreak.dto.freeipa.FreeIpaTestDto;
import com.sequenceiq.it.cloudbreak.exception.TestFailException;
import com.sequenceiq.it.cloudbreak.microservice.FreeIpaClient;
import com.sequenceiq.it.cloudbreak.testcase.e2e.AbstractE2ETest;
import com.sequenceiq.it.cloudbreak.util.ssh.action.SshJClientActions;

public class FreeIpaRotationTests extends AbstractE2ETest {

    private static final Status FREEIPA_AVAILABLE = Status.AVAILABLE;

    private static final String GET_PASSWORD_LAST_CHANGED_COMMAND = "export PASSWORD=$(sudo cat /srv/pillar/freeipa/init.sls | grep -i '\"password\"' | " +
            "awk -F'\"' '{print $4}'); kinit admin <<<$PASSWORD > /dev/null 2>&1; ipa user-show --all admin | grep krblastpwdchange: | awk -F': ' '{print $2}'";

    private static final String GET_PASSWORD_FROM_PILLAR_COMMAND = "sudo cat /srv/pillar/freeipa/init.sls | tail -n +2 | jq -r .freeipa.password";

    private static final String CHECK_DIRECTORY_MANAGER_PASSWORD_COMMAND = "export HOSTNAME=$(hostname -f); dsconf -D \"cn=Directory Manager\" -w \"%s\" " +
            "ldaps://$HOSTNAME config get";

    @Inject
    private FreeIpaTestClient freeIpaTestClient;

    @Inject
    private SshJClientActions sshJClientActions;

    @Override
    protected void setupTest(TestContext testContext) {
        super.setupTest(testContext);
    }

    @Test(dataProvider = TEST_CONTEXT)
    @Description(
            given = "there is a running cloudbreak",
            when = "a valid stack create request is sent with 2 FreeIPA instance " +
                    "AND do a FREEIPA_ADMIN_PASSWORD rotation on all cloud provider, " +
                    "after that {FREEIPA_SALT_BOOT_SECRETS, FREEIPA_USERSYNC_USER_PASSWORD} rotations on AWS only",
            then = "the stack should be available AND deletable and have 2 nodes AND the primary gateway should not change" +
                    " AND the rotations should happen")
    public void testFreeIpaRotation(TestContext testContext) {
        String freeIpa = resourcePropertyProvider().getName();
        Map<String, String> originalPasswordLastChangedMap = new HashMap<>();
        Map<String, String> originalPasswordsFromPillarMap = new HashMap<>();
        String cloudProvider = commonCloudProperties().getCloudProvider();

        FreeIpaRotationTestDto freeIpaRotationTestDto = testContext
                .given(freeIpa, FreeIpaTestDto.class)
                .withTelemetry("telemetry")
                .withOsType(RHEL8.getOs())
                .withFreeIpaHa(1, 2)
                .when(freeIpaTestClient.create(), key(freeIpa))
                .await(FREEIPA_AVAILABLE)
                .then((tc, testDto, client) -> {
                    Set<InstanceMetaDataResponse> instanceMetaDataResponses = getInstanceMetaDataResponses(testDto.getRequest().getEnvironmentCrn(), client);
                    getPasswordAndLastChangedFromPillar(originalPasswordLastChangedMap, originalPasswordsFromPillarMap, instanceMetaDataResponses);
                    checkDirectoryManagerPassword(instanceMetaDataResponses, originalPasswordsFromPillarMap);
                    return testDto;
                })
                .given(FreeIpaRotationTestDto.class)
                .withSecrets(List.of(FREEIPA_ADMIN_PASSWORD))
                .when(freeIpaTestClient.rotateSecret())
                .awaitForFlow()
                .then((tc, testDto, client) -> {
                    checkDirectoryManagerPasswordAfterRotation(originalPasswordLastChangedMap, originalPasswordsFromPillarMap, testDto, client);
                    return testDto;
                })
                .given(FreeIpaRotationTestDto.class)
                .withSecrets(List.of(SALT_BOOT_SECRETS,
                        SALT_SIGN_KEY_PAIR,
                        SALT_MASTER_KEY_PAIR,
                        FREEIPA_USERSYNC_USER_PASSWORD,
                        NGINX_CLUSTER_SSL_CERT_PRIVATE_KEY))
                .when(freeIpaTestClient.rotateSecret())
                .awaitForFlow();
        freeIpaRotationTestDto.validate();
    }

    private void getPasswordAndLastChangedFromPillar(Map<String, String> originalPasswordLastChangedMap,
            Map<String, String> originalPasswordsFromPillarMap, Set<InstanceMetaDataResponse> instanceMetaDataResponses) {
        originalPasswordLastChangedMap.putAll(getPasswordLastChanged(instanceMetaDataResponses));
        originalPasswordsFromPillarMap.putAll(getPasswordFromPillar(instanceMetaDataResponses));
        if (originalPasswordsFromPillarMap.values().stream().distinct().count() != 1) {
            throw new TestFailException("The passwords are not the same on the instances");
        }
    }

    private void checkDirectoryManagerPasswordAfterRotation(Map<String, String> originalPasswordLastChangedMap,
            Map<String, String> originalPasswordsFromPillarMap, FreeIpaRotationTestDto testDto, FreeIpaClient client) {
        Set<InstanceMetaDataResponse> instanceMetaDataResponses = getInstanceMetaDataResponses(testDto.getEnvironmentCrn(), client);
        Map<String, String> passwordFromPillar = getPasswordFromPillar(instanceMetaDataResponses);
        passwordFromPillar.forEach((key, value) -> {
            String originalPassword = originalPasswordsFromPillarMap.get(key);
            if (originalPassword.equals(value)) {
                throw new TestFailException("Freeipa admin password remained the same in pillar on " + key);
            }
        });
        Map<String, String> passwordLastChanged = getPasswordLastChanged(instanceMetaDataResponses);
        if (passwordLastChanged.values().size() != originalPasswordLastChangedMap.values().size()) {
            throw new TestFailException("The return size of the password last changed result is different");
        }
        passwordLastChanged.forEach((key, value) -> {
            String originalPasswordLastChanged = originalPasswordLastChangedMap.get(key);
            if (originalPasswordLastChanged.equals(value)) {
                throw new TestFailException("The password time for admin user is not different on " + key);
            }
        });
        checkDirectoryManagerPassword(instanceMetaDataResponses, passwordFromPillar);
    }

    private Map<String, String> getPasswordLastChanged(Set<InstanceMetaDataResponse> instanceMetaDataResponses) {
        Map<String, Pair<Integer, String>> sshCommandResponse = sshJClientActions.executeSshCommandOnHost(instanceMetaDataResponses,
                GET_PASSWORD_LAST_CHANGED_COMMAND, false);
        return sshCommandResponse.entrySet().stream()
                .collect(Collectors.toMap(Entry::getKey, sshCommandResult -> sshCommandResult.getValue().getValue()));
    }

    private Map<String, String> getPasswordFromPillar(Set<InstanceMetaDataResponse> instanceMetaDataResponses) {
        Map<String, Pair<Integer, String>> sshCommandResponse = sshJClientActions.executeSshCommandOnHost(instanceMetaDataResponses,
                GET_PASSWORD_FROM_PILLAR_COMMAND, false);
        return sshCommandResponse.entrySet().stream()
                .collect(Collectors.toMap(Entry::getKey, sshCommandResult -> sshCommandResult.getValue().getValue()));
    }

    private void checkDirectoryManagerPassword(Set<InstanceMetaDataResponse> instanceMetaDataResponses, Map<String, String> passwordsFromPillar) {
        String password = passwordsFromPillar.values().iterator().next().replaceAll("\\r\\n", "");
        String checkCommand = String.format(CHECK_DIRECTORY_MANAGER_PASSWORD_COMMAND, password);
        Map<String, Pair<Integer, String>> sshCommandResponse = sshJClientActions.executeSshCommandOnHost(instanceMetaDataResponses, checkCommand, false);
        Pair<Integer, String> response = sshCommandResponse.values().stream().findFirst().orElseThrow(() ->
                new TestFailException("We can't check directory manager password through SSH"));
        if (response.getValue().contains("Invalid credentials")) {
            throw new TestFailException("Credentials are invalid for directory manager password!");
        }
    }

    private Set<InstanceMetaDataResponse> getInstanceMetaDataResponses(String environmentCrn, FreeIpaClient<?> client) {
        DescribeFreeIpaResponse describeFreeIpaResponse = client.getDefaultClient().getFreeIpaV1Endpoint().describe(environmentCrn);
        return describeFreeIpaResponse.getInstanceGroups().stream()
                .map(InstanceGroupResponse::getMetaData)
                .flatMap(Set::stream)
                .collect(Collectors.toSet());
    }
}
