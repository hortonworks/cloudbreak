package com.sequenceiq.it.cloudbreak.util;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;

import jakarta.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.instancegroup.InstanceGroupV4Response;
import com.sequenceiq.it.cloudbreak.cloud.v4.CommonCloudProperties;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.distrox.DistroXTestDto;
import com.sequenceiq.it.cloudbreak.exception.TestFailException;
import com.sequenceiq.it.cloudbreak.log.Log;
import com.sequenceiq.it.cloudbreak.microservice.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.microservice.SdxClient;
import com.sequenceiq.it.cloudbreak.util.ssh.action.SshJClientActions;

@Component
public class SecretRotationCheckUtil {

    private static final Logger LOGGER = LoggerFactory.getLogger(SecretRotationCheckUtil.class);

    private static final String LDAP_CHECK_COMMAND = "export LDAP_BIND_PW=$(sudo grep \"LDAP_BIND_PW\" /etc/cloudera-scm-server/cm.settings | " +
            "awk '{print $3;}') && " +
            "export LDAP_BIND_DN=$(sudo grep \"LDAP_BIND_DN\" /etc/cloudera-scm-server/cm.settings | awk '{print $3;}') && " +
            "export LDAP_USER_SEARCH_BASE=$(sudo grep \"LDAP_USER_SEARCH_BASE\" /etc/cloudera-scm-server/cm.settings | awk '{print $3;}') && " +
            "export LDAP_URL=$(sudo grep \"LDAP_URL\" /etc/cloudera-scm-server/cm.settings | awk '{print $3;}') && " +
            "ldapsearch -LLL -H $LDAP_URL -D $LDAP_BIND_DN -w $LDAP_BIND_PW -b $LDAP_USER_SEARCH_BASE -z1";

    @Inject
    private SshJClientActions sshJClientActions;

    @Inject
    private CommonCloudProperties commonCloudProperties;

    public void checkLdapLogin(String datalakeCrn, SdxClient client) {
        checkLdapLogin(getInstanceGroupResponses(datalakeCrn, client));
    }

    public void checkLdapLogin(TestContext testContext, DistroXTestDto testDto, CloudbreakClient client) {
        checkLdapLogin(getInstanceGroupResponses(testContext, testDto, client));
    }

    public void checkSSHLoginWithNewKeys(String datalakeCrn, SdxClient client) {
        checkSSHLoginWithNewKeys(getInstanceGroupResponses(datalakeCrn, client));
    }

    public void checkSSHLoginWithNewKeys(TestContext testContext, DistroXTestDto testDto, CloudbreakClient client) {
        checkSSHLoginWithNewKeys(getInstanceGroupResponses(testContext, testDto, client));
    }

    private void checkLdapLogin(Collection<InstanceGroupV4Response> instanceGroupV4Responses) {
        Map<String, Pair<Integer, String>> sshCommandResponse = sshJClientActions.executeSshCommandOnPrimaryGateways(instanceGroupV4Responses,
                LDAP_CHECK_COMMAND, false);
        boolean ldapSearchWasSuccessful = sshCommandResponse.values().stream()
                .map(Map.Entry::getKey)
                .allMatch(exitCode -> exitCode.equals(0) || exitCode.equals(4));
        if (!ldapSearchWasSuccessful) {
            throw new TestFailException(String.format("LDAP login check with new password was not successful on all gateway nodes. " +
                    "Checks: %s", sshCommandResponse));
        }
    }

    private void checkSSHLoginWithNewKeys(Collection<InstanceGroupV4Response> instanceGroupV4Responses) {
        if (StringUtils.isBlank(commonCloudProperties.getRotationSshPublicKey()) ||
                StringUtils.isBlank(commonCloudProperties.getRotationPrivateKeyFile())) {
            Log.log(LOGGER, "Checking SSH key rotation skipped because parameters are not found " +
                    "(integrationtest.rotationSshPublicKey, integrationtest.rotationPrivateKeyFile).");
        } else {
            String checkCommand = "echo \"SSH login check\"";
            Map<String, Pair<Integer, String>> sshCommandResponse =
                    sshJClientActions.executeSshCommandOnAllHosts(
                            instanceGroupV4Responses,
                            checkCommand, false,
                            commonCloudProperties.getRotationPrivateKeyFile());
            boolean keyChangeWasSuccessfulOnAllNodes = sshCommandResponse.values().stream()
                    .map(Map.Entry::getValue)
                    .allMatch(value -> value.contains("SSH login check"));
            if (!keyChangeWasSuccessfulOnAllNodes) {
                throw new TestFailException(String.format("SSH login check was not successful on all nodes. Checks: %s", sshCommandResponse));
            }
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
