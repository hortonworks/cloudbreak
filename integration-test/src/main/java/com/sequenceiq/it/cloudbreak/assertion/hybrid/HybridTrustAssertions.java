package com.sequenceiq.it.cloudbreak.assertion.hybrid;

import java.util.List;
import java.util.Locale;

import jakarta.inject.Inject;

import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.instancegroup.instancemetadata.InstanceMetaDataV4Response;
import com.sequenceiq.common.api.type.InstanceGroupType;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.describe.TrustResponse;
import com.sequenceiq.it.cloudbreak.assertion.Assertion;
import com.sequenceiq.it.cloudbreak.cloud.v4.CommonCloudProperties;
import com.sequenceiq.it.cloudbreak.config.TrustProperties;
import com.sequenceiq.it.cloudbreak.dto.distrox.DistroXTestDto;
import com.sequenceiq.it.cloudbreak.dto.freeipa.FreeIpaTestDto;
import com.sequenceiq.it.cloudbreak.exception.TestFailException;
import com.sequenceiq.it.cloudbreak.microservice.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.microservice.FreeIpaClient;
import com.sequenceiq.it.cloudbreak.util.ssh.action.ActiveDirectorySshJClientActions;
import com.sequenceiq.it.cloudbreak.util.ssh.action.SshJClientActions;

@Component
public class HybridTrustAssertions {

    private static final Logger LOGGER = LoggerFactory.getLogger(HybridTrustAssertions.class);

    private static final String AD_TRUST_VALIDATE_COMMAND = "nltest /dsgetdc:%s /force || echo: && echo " + ActiveDirectorySshJClientActions.FAILURE_TOKEN;

    private static final String FREEIPA_TRUST_VALIDATE_COMMAND = """
            export PW=$(sudo grep -v "^#" /srv/pillar/freeipa/init.sls | jq -r '.freeipa.password');
            kinit admin <<<$PW > /dev/null 2>&1;
            KRB5_TRACE=/dev/stdout kvno ldap/%s@%s
            """;

    private static final String DISTROX_TRUST_VALIDATE_COMMAND = """
            echo Password123! | KRB5_TRACE=/dev/stdout kinit -V fakemockuser0;
            HADOOP_OPTS="-Dsun.security.krb5.debug=true" hdfs dfs -ls hdfs://%s
            """;

    @Inject
    private SshJClientActions sshJClientActions;

    @Inject
    private ActiveDirectorySshJClientActions activeDirectorySshJClientActions;

    @Inject
    private CommonCloudProperties commonCloudProperties;

    @Inject
    private TrustProperties trustProperties;

    public Assertion<FreeIpaTestDto, FreeIpaClient> validateTrustOnFreeIpa() {
        return (testContext, testDto, client) -> {
            List<String> freeIpaIps = testDto.getAllInstanceIps();
            TrustResponse trust = testDto.getResponse().getTrust();
            String command = String.format(FREEIPA_TRUST_VALIDATE_COMMAND, trust.getFqdn(), trust.getRealm().toUpperCase(Locale.ROOT));
            executeFreeIpaCommand(freeIpaIps, command);
            return testDto;
        };
    }

    private void executeFreeIpaCommand(List<String> freeIpaIps, String command) {
        freeIpaIps.forEach(freeIpaIp -> {
            Pair<Integer, String> result = sshJClientActions.executeSshCommand(freeIpaIp, command);
            if (result.getKey() != 0) {
                throw new TestFailException("The command execution of '" + command + "' failed with: " + result.getValue());
            }
        });
    }

    public Assertion<FreeIpaTestDto, FreeIpaClient> validateTrustOnActiveDirectory() {
        return (testContext, testDto, client) ->  {
            String realm = testDto.getResponse().getFreeIpa().getDomain().toLowerCase(Locale.ROOT);
            String command = String.format(AD_TRUST_VALIDATE_COMMAND, realm);
            activeDirectorySshJClientActions.executeActiveDirectoryCommands(testDto.getName() + "-validate", command, true);
            return testDto;
        };
    }

    public Assertion<DistroXTestDto, CloudbreakClient> validateTrustOnDistroX() {
        return (testContext, testDto, client) ->  {
            String hdfsPath = trustProperties.getHdfsPath();
            InstanceMetaDataV4Response gatewayInstance = testDto.getResponse().getInstanceGroups().stream()
                    .filter(ig -> ig.getType().equals(InstanceGroupType.GATEWAY))
                    .flatMap(ig -> ig.getMetadata().stream())
                    .findFirst()
                    .orElseThrow(() -> new TestFailException("The DistroX has no GATEWAY"));
            Pair<Integer, String> result =
                    sshJClientActions.executeSshCommand(gatewayInstance.getPrivateIp(), DISTROX_TRUST_VALIDATE_COMMAND.formatted(hdfsPath));
            if (result.getKey() != 0) {
                throw new TestFailException("Failed to list HDFS of %s with error: %s".formatted(hdfsPath, result.getValue()));
            }
            return testDto;
        };
    }
}
