package com.sequenceiq.it.cloudbreak.testcase.e2e.sdx;

import java.util.List;
import java.util.Map;

import jakarta.inject.Inject;

import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.instancegroup.InstanceGroupV4Response;
import com.sequenceiq.cloudbreak.common.database.TargetMajorVersion;
import com.sequenceiq.it.cloudbreak.cloud.v4.CommonClusterManagerProperties;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.AbstractSdxTestDto;
import com.sequenceiq.it.cloudbreak.dto.CloudbreakTestDto;
import com.sequenceiq.it.cloudbreak.exception.TestFailException;
import com.sequenceiq.it.cloudbreak.util.ssh.action.SshJClientActions;
import com.sequenceiq.sdx.api.model.SdxClusterDetailResponse;
import com.sequenceiq.sdx.api.model.SdxDatabaseAvailabilityType;

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

    @Inject
    private CommonClusterManagerProperties commonClusterManagerProperties;

    public <T extends AbstractSdxTestDto<?, SdxClusterDetailResponse, ?>> T checkDbEngineVersionIsUpdated(String targetDatabaseMajorVersion, T testDto) {
        String actualDatabaseMajorVersion = testDto.getResponse().getDatabaseEngineVersion();
        if (!targetDatabaseMajorVersion.equals(actualDatabaseMajorVersion)) {
            String errorMsg = String.format("Datalake's database engine version is wrong: %s, expected: %s",
                    actualDatabaseMajorVersion, targetDatabaseMajorVersion);
            LOGGER.error(errorMsg);
            throw new TestFailException(errorMsg);
        }
        return testDto;
    }

    public <T extends AbstractSdxTestDto<?, SdxClusterDetailResponse, ?>> T checkCloudProviderDatabaseVersionFromPrimaryGateway(
            String databaseMajorVersion, TestContext tc, T testDto) {
        SdxDatabaseAvailabilityType availabilityType = testDto.getResponse().getSdxDatabaseResponse().getAvailabilityType();
        List<InstanceGroupV4Response> instanceGroups = testDto.getResponse().getStackV4Response().getInstanceGroups();
        return SdxDatabaseAvailabilityType.hasExternalDatabase(availabilityType)
                ? checkExternalDatabaseVersionFromPrimaryGateway(tc, testDto, instanceGroups, databaseMajorVersion)
                : checkEmbeddedDatabaseVersionFromPrimaryGateway(tc, testDto, instanceGroups, databaseMajorVersion);
    }

    private <T extends CloudbreakTestDto> T checkEmbeddedDatabaseVersionFromPrimaryGateway(
            TestContext tc, T testDto, List<InstanceGroupV4Response> instanceGroups, String databaseMajorVersion) {
        Map<String, Boolean> serviceStatusesByName = Map.of(
                "postgresql-10", "10".equals(databaseMajorVersion),
                "postgresql-11", "11".equals(databaseMajorVersion),
                "postgresql-14", "14".equals(databaseMajorVersion)
        );

        sshJClientActions.checkSystemctlServiceStatusOnPrimaryGateway(testDto, instanceGroups, serviceStatusesByName);
        return checkDatabaseVersionFromPrimaryGateway(testDto, instanceGroups, databaseMajorVersion, EMBEDDED_DBSERVER_VERSION_CMD);
    }

    private <T extends CloudbreakTestDto> T checkExternalDatabaseVersionFromPrimaryGateway(TestContext tc, T testDto,
            List<InstanceGroupV4Response> instanceGroups, String databaseMajorVersion) {
        String command = tc.getCloudProvider().isExternalDatabaseSslEnforcementSupported() ? DBSERVER_VERSION_CMD_SSL : DBSERVER_VERSION_CMD;
        return checkDatabaseVersionFromPrimaryGateway(testDto, instanceGroups, databaseMajorVersion, command);
    }

    private <T extends CloudbreakTestDto> T checkDatabaseVersionFromPrimaryGateway(T testDto, List<InstanceGroupV4Response> instanceGroups,
            String databaseMajorVersion, String command) {
        Map<String, Pair<Integer, String>> dbVersions = sshJClientActions.executeSshCommandOnPrimaryGateways(instanceGroups, command, false);
        List<Pair<Integer, String>> wrongVersions = dbVersions.values().stream()
                .filter(ssh -> !ssh.getValue().startsWith(databaseMajorVersion))
                .toList();
        if (!wrongVersions.isEmpty()) {
            String errorMsg = String.format("Datalake's database engine version check is wrong on primary gateways: %s, expected: %s",
                    dbVersions, databaseMajorVersion);
            LOGGER.error(errorMsg);
            throw new TestFailException(errorMsg);
        }
        return testDto;
    }

    public String getOriginalDatabaseMajorVersion() {
        return commonClusterManagerProperties.getUpgradeDatabaseServer().getOriginalDatabaseMajorVersion();
    }

    public TargetMajorVersion getTargetMajorVersion() {
        String versionString = commonClusterManagerProperties.getUpgradeDatabaseServer().getTargetDatabaseMajorVersion();
        return parseTargetMajorVersion(versionString);
    }

    private TargetMajorVersion parseTargetMajorVersion(String versionString) {
        try {
            return TargetMajorVersion.valueOf("VERSION" + versionString);
        } catch (Exception exception) {
            LOGGER.warn("Failed to parse given versionString {}, falling back to default", versionString, exception);
            return TargetMajorVersion.VERSION11;
        }
    }
}
