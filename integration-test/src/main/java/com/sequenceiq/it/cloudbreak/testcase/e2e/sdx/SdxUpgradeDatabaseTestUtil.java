package com.sequenceiq.it.cloudbreak.testcase.e2e.sdx;

import java.util.List;
import java.util.Map;

import jakarta.inject.Inject;

import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.sdx.SdxTestDto;
import com.sequenceiq.it.cloudbreak.exception.TestFailException;
import com.sequenceiq.it.cloudbreak.util.ssh.action.SshJClientActions;

@Component
public class SdxUpgradeDatabaseTestUtil {
    private static final Logger LOGGER = LoggerFactory.getLogger(SdxUpgradeDatabaseTestUtil.class);

    private static final String EMBEDDED_DBSERVER_VERSION_CMD = "sudo sh -c 'psql -U postgres -c \"show server_version\" | grep -o \"[0-9]*\\.\"'";

    private static final String DBSERVER_VERSION_CMD_SSL = "sudo sh -c '. activate_salt_env; \\\n" +
            "DBUSER=$(salt-call pillar.get \"postgres:clouderamanager:remote_admin\" --output json 2>/dev/null | jq -r .local); \\\n" +
            "DBHOST=$(salt-call pillar.get \"postgres:clouderamanager:remote_db_url\" --output json 2>/dev/null | jq -r .local); \\\n" +
            "DBPORT=$(salt-call pillar.get \"postgres:clouderamanager:remote_db_port\" --output json 2>/dev/null | jq -r .local); \\\n" +
            "export PGPASSWORD=$(salt-call pillar.get \"postgres:clouderamanager:remote_admin_pw\" --output json 2>/dev/null | jq -r .local); \\\n" +
            "export PGSSLROOTCERT=$(salt-call pillar.get \"postgres_root_certs:ssl_certs_file_path\" --output json 2>/dev/null | jq -r .local); \\\n" +
            "export PGSSLMODE=verify-full; \\\n" +
            "psql -h \"$DBHOST\" -p \"$DBPORT\" -U \"$DBUSER\" -d postgres -c \"show server_version\" | grep -o \"[0-9]*\\.\"'";

    private static final String DBSERVER_VERSION_CMD = "sudo sh -c '. activate_salt_env; \\\n" +
            "DBUSER=$(salt-call pillar.get \"postgres:clouderamanager:remote_admin\" --output json 2>/dev/null | jq -r .local); \\\n" +
            "DBHOST=$(salt-call pillar.get \"postgres:clouderamanager:remote_db_url\" --output json 2>/dev/null | jq -r .local); \\\n" +
            "DBPORT=$(salt-call pillar.get \"postgres:clouderamanager:remote_db_port\" --output json 2>/dev/null | jq -r .local); \\\n" +
            "export PGPASSWORD=$(salt-call pillar.get \"postgres:clouderamanager:remote_admin_pw\" --output json 2>/dev/null | jq -r .local); \\\n" +
            "psql -h \"$DBHOST\" -p \"$DBPORT\" -U \"$DBUSER\" -d postgres -c \"show server_version\" | grep -o \"[0-9]*\\.\"'";

    @Inject
    private SshJClientActions sshJClientActions;

    public SdxTestDto checkEmbeddedDatabaseVersionFromMasterNode(TestContext tc, SdxTestDto testDto) {
        return checkDatabaseVersionFromMasterNode(tc, testDto, EMBEDDED_DBSERVER_VERSION_CMD, testDto.getResponse().getDatabaseEngineVersion());
    }

    public SdxTestDto checkDbEngineVersionIsUpdated(String targetDatabaseMajorVersion, SdxTestDto testDto) {
        String actualDatabaseMajorVersion = testDto.getResponse().getDatabaseEngineVersion();
        if (!targetDatabaseMajorVersion.equals(actualDatabaseMajorVersion)) {
            String errorMsg = String.format("Datalake's database engine version is wrong: %s, expected: %s",
                    actualDatabaseMajorVersion, targetDatabaseMajorVersion);
            LOGGER.error(errorMsg);
            throw new TestFailException(errorMsg);
        }
        return testDto;
    }

    public SdxTestDto checkCloudProviderDatabaseVersionFromMasterNode(String targetDatabaseMajorVersion, TestContext tc, SdxTestDto testDto) {
        String command = tc.getCloudProvider().isExternalDatabaseSslEnforcementSupported() ? DBSERVER_VERSION_CMD_SSL : DBSERVER_VERSION_CMD;
        return checkDatabaseVersionFromMasterNode(tc, testDto, command, targetDatabaseMajorVersion);
    }

    private SdxTestDto checkDatabaseVersionFromMasterNode(TestContext tc, SdxTestDto testDto, String checkCmd, String targetDatabaseMajorVersion) {
        Map<String, Pair<Integer, String>> dbVersions = sshJClientActions.executeSshCommandOnPrimaryGateways(
                testDto.getResponse().getStackV4Response().getInstanceGroups(), checkCmd, false);
        List<Pair<Integer, String>> wrongVersions = dbVersions.values().stream()
                .filter(ssh -> !ssh.getValue().startsWith(targetDatabaseMajorVersion))
                .toList();
        if (!wrongVersions.isEmpty()) {
            String errorMsg = String.format("Datalake's database engine version check is wrong on primary gateways: %s, expected: %s",
                    dbVersions, targetDatabaseMajorVersion);
            LOGGER.error(errorMsg);
            throw new TestFailException(errorMsg);
        }
        return testDto;
    }
}
