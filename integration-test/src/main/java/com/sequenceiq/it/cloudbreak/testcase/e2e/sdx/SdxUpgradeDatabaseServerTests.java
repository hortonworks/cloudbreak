package com.sequenceiq.it.cloudbreak.testcase.e2e.sdx;

import static com.sequenceiq.it.cloudbreak.context.RunningParameter.key;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.Test;

import com.sequenceiq.cloudbreak.common.database.TargetMajorVersion;
import com.sequenceiq.it.cloudbreak.client.SdxTestClient;
import com.sequenceiq.it.cloudbreak.cloud.v4.CommonClusterManagerProperties;
import com.sequenceiq.it.cloudbreak.context.Description;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.sdx.SdxTestDto;
import com.sequenceiq.it.cloudbreak.dto.sdx.SdxUpgradeDatabaseServerTestDto;
import com.sequenceiq.it.cloudbreak.exception.TestFailException;
import com.sequenceiq.it.cloudbreak.util.spot.UseSpotInstances;
import com.sequenceiq.it.cloudbreak.util.ssh.action.SshJClientActions;
import com.sequenceiq.sdx.api.model.SdxClusterStatusResponse;
import com.sequenceiq.sdx.api.model.SdxDatabaseAvailabilityType;
import com.sequenceiq.sdx.api.model.SdxDatabaseRequest;

public class SdxUpgradeDatabaseServerTests extends PreconditionSdxE2ETest {
    private static final Logger LOGGER = LoggerFactory.getLogger(SdxUpgradeDatabaseServerTests.class);

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
    private SdxTestClient sdxTestClient;

    @Inject
    private CommonClusterManagerProperties commonClusterManagerProperties;

    @Inject
    private SshJClientActions sshJClientActions;

    @Test(dataProvider = TEST_CONTEXT)
    @UseSpotInstances
    @Description(
            given = "there is a running Cloudbreak, and an SDX cluster in available state",
            when = "upgrade called on the SDX cluster",
            then = "SDX upgrade database server should be successful, the cluster should be up and running"
    )
    public void testSDXDatabaseUpgrade(TestContext testContext) {
        String sdx = resourcePropertyProvider().getName();

        String originalDatabaseMajorVersion = commonClusterManagerProperties.getUpgradeDatabaseServer().getOriginalDatabaseMajorVersion();
        String targetDatabaseMajorVersion = commonClusterManagerProperties.getUpgradeDatabaseServer().getTargetDatabaseMajorVersion();
        SdxDatabaseRequest sdxDatabaseRequest = new SdxDatabaseRequest();
        sdxDatabaseRequest.setAvailabilityType(SdxDatabaseAvailabilityType.NON_HA);
        sdxDatabaseRequest.setDatabaseEngineVersion(originalDatabaseMajorVersion);

        testContext
                .given(sdx, SdxTestDto.class)
                .withCloudStorage()
                .withExternalDatabase(sdxDatabaseRequest)
                .when(sdxTestClient.create(), key(sdx))
                .await(SdxClusterStatusResponse.RUNNING, key(sdx))
                .awaitForHealthyInstances()
                .given(SdxUpgradeDatabaseServerTestDto.class)
                .withTargetMajorVersion(fromVersionString(targetDatabaseMajorVersion))
                .given(sdx, SdxTestDto.class)
                .when(sdxTestClient.upgradeDatabaseServer(), key(sdx))
                .await(SdxClusterStatusResponse.DATALAKE_UPGRADE_DATABASE_SERVER_IN_PROGRESS, key(sdx).withWaitForFlow(Boolean.FALSE))
                .await(SdxClusterStatusResponse.RUNNING, key(sdx))
                .awaitForHealthyInstances()
                .then((tc, testDto, client) -> checkDbEngineVersionIsUpdated(targetDatabaseMajorVersion, testDto))
                .then((tc, testDto, client) -> checkCloudProviderDatabaseVersionFromMasterNode(targetDatabaseMajorVersion, tc, testDto))
                .validate();
    }

    private TargetMajorVersion fromVersionString(String versionString) {
        try {
            return TargetMajorVersion.valueOf("VERSION_" + versionString);
        } catch (Exception exception) {
            return TargetMajorVersion.VERSION_11;
        }
    }

    private SdxTestDto checkDbEngineVersionIsUpdated(String targetDatabaseMajorVersion, SdxTestDto testDto) {
        String actualDatabaseMajorVersion = testDto.getResponse().getDatabaseEngineVersion();
        if (!targetDatabaseMajorVersion.equals(actualDatabaseMajorVersion)) {
            String errorMsg = String.format("Datalake's database engine version is wrong: %s, expected: %s",
                    actualDatabaseMajorVersion, targetDatabaseMajorVersion);
            LOGGER.error(errorMsg);
            throw new TestFailException(errorMsg);
        }
        return testDto;
    }

    private SdxTestDto checkCloudProviderDatabaseVersionFromMasterNode(String targetDatabaseMajorVersion, TestContext tc, SdxTestDto testDto) {
        String command = tc.getCloudProvider().isExternalDatabaseSslEnforcementSupported() ? DBSERVER_VERSION_CMD_SSL : DBSERVER_VERSION_CMD;
        Map<String, Pair<Integer, String>> dbVersions = sshJClientActions.executeSshCommandOnPrimaryGateways(
                testDto.getResponse().getStackV4Response().getInstanceGroups(), command, false);
        List<Pair<Integer, String>> wrongVersions = dbVersions.values().stream()
                .filter(ssh -> !ssh.getValue().startsWith(targetDatabaseMajorVersion))
                .collect(Collectors.toList());
        if (!wrongVersions.isEmpty()) {
            String errorMsg = String.format("Datalake's database engine version check is wrong on primary gateways: %s, expected: %s",
                    dbVersions, targetDatabaseMajorVersion);
            LOGGER.error(errorMsg);
            throw new TestFailException(errorMsg);
        }
        return testDto;
    }
}
