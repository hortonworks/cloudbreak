package com.sequenceiq.it.cloudbreak.util;

import java.time.LocalDate;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import jakarta.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.instancegroup.InstanceGroupV4Response;
import com.sequenceiq.it.cloudbreak.assertion.util.InstanceIPCollectorUtil;
import com.sequenceiq.it.cloudbreak.cloud.v4.CommonCloudProperties;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.distrox.DistroXTestDto;
import com.sequenceiq.it.cloudbreak.dto.freeipa.FreeIpaTestDto;
import com.sequenceiq.it.cloudbreak.dto.sdx.SdxInternalTestDto;
import com.sequenceiq.it.cloudbreak.exception.TestFailException;
import com.sequenceiq.it.cloudbreak.log.Log;
import com.sequenceiq.it.cloudbreak.microservice.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.microservice.SdxClient;
import com.sequenceiq.it.cloudbreak.util.ssh.SshCommandOutcome;
import com.sequenceiq.it.cloudbreak.util.ssh.SshCommandOutcomes;
import com.sequenceiq.it.cloudbreak.util.ssh.action.SshJClientActions;
import com.sequenceiq.it.cloudbreak.util.ssh.action.SshSaltPasswordActions;

@Component
public class SecretRotationCheckUtil {

    private static final Logger LOGGER = LoggerFactory.getLogger(SecretRotationCheckUtil.class);

    private static final String LDAP_CHECK_COMMAND = "export LDAP_BIND_PW=$(sudo grep \"LDAP_BIND_PW\" /etc/cloudera-scm-server/cm.settings | " +
            "awk '{print $3;}') && " +
            "export LDAP_BIND_DN=$(sudo grep \"LDAP_BIND_DN\" /etc/cloudera-scm-server/cm.settings | awk '{print $3;}') && " +
            "export LDAP_USER_SEARCH_BASE=$(sudo grep \"LDAP_USER_SEARCH_BASE\" /etc/cloudera-scm-server/cm.settings | awk '{print $3;}') && " +
            "export LDAP_URL=$(sudo grep \"LDAP_URL\" /etc/cloudera-scm-server/cm.settings | awk '{print $3;}') && " +
            "ldapsearch -LLL -H $LDAP_URL -D $LDAP_BIND_DN -w $LDAP_BIND_PW -b $LDAP_USER_SEARCH_BASE -z1";

    private static final LocalDate PAST_DATE = LocalDate.now().minusMonths(1);

    private static final AtomicReference<String> SHADOW_LINE_REFERENCE = new AtomicReference<>();

    @Inject
    private SshJClientActions sshJClientActions;

    @Inject
    private CommonCloudProperties commonCloudProperties;

    @Inject
    private SshSaltPasswordActions sshSaltPasswordActions;

    public SdxInternalTestDto checkLdapLogin(String datalakeCrn, SdxInternalTestDto testDto, SdxClient client) {
        checkLdapLogin(getInstanceGroupResponses(datalakeCrn, client, testDto.getTestContext()));
        return testDto;
    }

    public DistroXTestDto checkLdapLogin(TestContext testContext, DistroXTestDto testDto, CloudbreakClient client) {
        checkLdapLogin(getInstanceGroupResponses(testContext, testDto, client));
        return testDto;
    }

    public void checkSSHLoginWithNewKeys(String datalakeCrn, SdxClient client, TestContext testContext) {
        checkSSHLoginWithNewKeys(getInstanceGroupResponses(datalakeCrn, client, testContext));
    }

    public void checkSSHLoginWithNewKeys(TestContext testContext, DistroXTestDto testDto, CloudbreakClient client) {
        checkSSHLoginWithNewKeys(getInstanceGroupResponses(testContext, testDto, client));
    }

    private void checkLdapLogin(Collection<InstanceGroupV4Response> instanceGroupV4Responses) {
        SshCommandOutcomes sshCommandResponse = sshJClientActions.executeSshCommandOnPrimaryGateways(
                instanceGroupV4Responses, LDAP_CHECK_COMMAND, false).requireAllExecuted();
        boolean ldapSearchWasSuccessful = sshCommandResponse.stream()
                .map(SshCommandOutcome::returnCode)
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
            SshCommandOutcomes sshCommandResponse = sshJClientActions.executeSshCommandOnAllHosts(
                    instanceGroupV4Responses,
                    checkCommand, false,
                    commonCloudProperties.getRotationPrivateKeyFile()).requireAllExecuted();
            boolean keyChangeWasSuccessfulOnAllNodes = sshCommandResponse.stream()
                    .map(SshCommandOutcome::commandOutput)
                    .allMatch(value -> value.contains("SSH login check"));
            if (!keyChangeWasSuccessfulOnAllNodes) {
                throw new TestFailException(String.format("SSH login check was not successful on all nodes. Checks: %s", sshCommandResponse));
            }
        }
    }

    private Collection<InstanceGroupV4Response> getInstanceGroupResponses(String datalakeCrn, SdxClient client, TestContext testContext) {
        return client.getDefaultClient(testContext)
                .sdxEndpoint()
                .getDetailByCrn(datalakeCrn, Collections.emptySet())
                .getStackV4Response()
                .getInstanceGroups();
    }

    private Collection<InstanceGroupV4Response> getInstanceGroupResponses(TestContext testContext, DistroXTestDto testDto, CloudbreakClient client) {
        return client.getDefaultClient(testContext)
                .stackV4Endpoint()
                .get(client.getWorkspaceId(), testDto.getName(), Collections.emptySet(), testContext.getActingUserCrn().getAccountId())
                .getInstanceGroups();
    }

    public FreeIpaTestDto preSaltPasswordRotation(FreeIpaTestDto testDto) {
        List<String> ipAddresses = InstanceIPCollectorUtil.getAllInstanceIps(testDto, false);
        return preSaltPasswordRotation(testDto, ipAddresses);
    }

    public SdxInternalTestDto preSaltPasswordRotation(SdxInternalTestDto testDto) {
        List<String> ipAddresses = getSdxIpAddresses(testDto);
        return preSaltPasswordRotation(testDto, ipAddresses);
    }

    public DistroXTestDto preSaltPasswordRotation(DistroXTestDto testDto) {
        List<String> ipAddresses = getDistroXIpAddresses(testDto);
        return preSaltPasswordRotation(testDto, ipAddresses);
    }

    private <T> T preSaltPasswordRotation(T testDto, Collection<String> ipAddresses) {
        sshSaltPasswordActions.setPasswordChangeDate(ipAddresses, PAST_DATE);
        SHADOW_LINE_REFERENCE.set(sshSaltPasswordActions.getShadowLine(ipAddresses));
        return testDto;
    }

    public FreeIpaTestDto validateSaltPasswordRotation(FreeIpaTestDto testDto) {
        List<String> ipAddresses = InstanceIPCollectorUtil.getAllInstanceIps(testDto, false);
        return validateSaltPasswordRotation(testDto, ipAddresses);
    }

    public SdxInternalTestDto validateSaltPasswordRotation(SdxInternalTestDto testDto) {
        List<String> ipAddresses = getSdxIpAddresses(testDto);
        return validateSaltPasswordRotation(testDto, ipAddresses);
    }

    public DistroXTestDto validateSaltPasswordRotation(DistroXTestDto testDto) {
        List<String> ipAddresses = getDistroXIpAddresses(testDto);
        return validateSaltPasswordRotation(testDto, ipAddresses);
    }

    private <T> T validateSaltPasswordRotation(T testDto, Collection<String> ipAddresses) {
        String shadowLine = sshSaltPasswordActions.getShadowLine(ipAddresses);
        if (shadowLine.equals(SHADOW_LINE_REFERENCE.get())) {
            throw new TestFailException("Saltuser shadow line was not changed after password rotation");
        }
        SHADOW_LINE_REFERENCE.set("");

        LocalDate passwordChange = sshSaltPasswordActions.getPasswordChangeDate(ipAddresses);
        if (!passwordChange.isEqual(LocalDate.now())) {
            throw new TestFailException("Saltuser password change date was not modified to today after password rotation");
        }
        return testDto;
    }

    private List<String> getSdxIpAddresses(SdxInternalTestDto testDto) {
        return InstanceIPCollectorUtil.getGatewayIps(testDto.getResponse().getStackV4Response().getInstanceGroups(), false);
    }

    private List<String> getDistroXIpAddresses(DistroXTestDto testDto) {
        return InstanceIPCollectorUtil.getGatewayIps(testDto.getResponse().getInstanceGroups(), false);
    }
}
