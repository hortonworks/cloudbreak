package com.sequenceiq.cloudbreak.cloud.aws.connector.resource.upgrade.operation;


import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;

import com.amazonaws.services.rds.model.DBEngineVersion;
import com.amazonaws.services.rds.model.DescribeDBEngineVersionsRequest;
import com.amazonaws.services.rds.model.DescribeDBEngineVersionsResult;
import com.amazonaws.services.rds.model.UpgradeTarget;
import com.sequenceiq.cloudbreak.cloud.aws.common.client.AmazonRdsClient;
import com.sequenceiq.cloudbreak.cloud.exception.CloudConnectorException;
import com.sequenceiq.cloudbreak.cloud.model.DatabaseEngine;
import com.sequenceiq.cloudbreak.common.database.Version;

@ExtendWith(MockitoExtension.class)
public class AwsRdsVersionOperationsTest {

    private final AwsRdsVersionOperations underTest = new AwsRdsVersionOperations();

    @Test
    void testGetHighestUpgradeVersion() {
        Set<RdsEngineVersion> validUpgradeVersions = Set.of(new RdsEngineVersion("1.1"), new RdsEngineVersion("1.2"));

        RdsEngineVersion selectedTargetVersion = underTest.getHighestUpgradeTargetVersion(validUpgradeVersions);

        assertEquals("1.2", selectedTargetVersion.getVersion());
    }

    @Test
    void getDBParameterGroupFamilyTestWhenPgSqlAndVersionIsNull() {
        Assertions.assertThrows(IllegalStateException.class, () ->
                underTest.getDBParameterGroupFamily(DatabaseEngine.POSTGRESQL, null)
        );
    }

    @Test
    void getDBParameterGroupFamilyTestWhenPgSqlAndVersionIsEmpty() {
        Assertions.assertThrows(IllegalStateException.class, () ->
                underTest.getDBParameterGroupFamily(DatabaseEngine.POSTGRESQL, "")
        );
    }

    // Note: There is no easy way to test the "bad engine variant" case as enum classes are final and hard to mock.
    @Test
    void getDBParameterGroupFamilyTestWhenPgSqlAndBadVersionFormat() {
        Assertions.assertThrows(IllegalStateException.class, () ->
                underTest.getDBParameterGroupFamily(DatabaseEngine.POSTGRESQL, "latest")
        );
    }

    @Test
    void getDBParameterGroupFamilyTestWhenPgSqlAndMissingMinorVersion() {
        String dbFamily = underTest.getDBParameterGroupFamily(DatabaseEngine.POSTGRESQL, "10");

        assertEquals("postgres10", dbFamily);
    }

    @Test
    void getDBParameterGroupFamilyTestWhenPgSqlAndMajorVersionNumericOverflow() {
        Assertions.assertThrows(NumberFormatException.class, () ->
                underTest.getDBParameterGroupFamily(DatabaseEngine.POSTGRESQL, "12345678901234567890.1")
        );
    }

    static Object[][] unsupportedMajorVersionDataProvider() {
        return new Object[][]{
                // testCaseName version
                {"version 0.1", "0.1"},
                {"version 1.1", "1.1"},
                {"version 2.1", "2.1"},
                {"version 3.1", "3.1"},
                {"version 4.1", "4.1"},
                {"version 5.1", "5.1"},
                {"version 6.1", "6.1"},
                {"version 7.1", "7.1"},
                {"version 8.1", "8.1"},
                {"version 123.1", "123.1"},
        };
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("unsupportedMajorVersionDataProvider")
    void getDBParameterGroupFamilyTestWhenPgSqlAndUnsupportedMajorVersion(String testCaseName, String version) {
        Assertions.assertThrows(IllegalStateException.class, () ->
                underTest.getDBParameterGroupFamily(DatabaseEngine.POSTGRESQL, version)
        );
    }

    static Object[][] validVersionDataProvider() {
        return new Object[][]{
                // testCaseName version familyExpected
                {"version 9.5", "9.5", "postgres9.5"},
                {"version 9.6", "9.6", "postgres9.6"},
                {"version 10.1", "10.1", "postgres10"},
                {"version 11.1", "11.1", "postgres11"},
                {"version 12.1", "12.1", "postgres12"},
                {"version 13.1", "13.1", "postgres13"},
        };
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("validVersionDataProvider")
    void getDBParameterGroupFamilyTestWhenPgSqlAndValidVersion(String testCaseName, String version, String familyExpected) {
        String dbFamily = underTest.getDBParameterGroupFamily(DatabaseEngine.POSTGRESQL, version);

        assertEquals(familyExpected, dbFamily);
    }

    @Test
    void testGetAllUpgradeTargets() {
        AmazonRdsClient rdsClient = mock(AmazonRdsClient.class);
        RdsEngineVersion rdsEngineVersion = new RdsEngineVersion("1.2");
        DBEngineVersion dbEngineVersionV1 = new DBEngineVersion().withValidUpgradeTarget(
                new UpgradeTarget().withEngineVersion("1.1"),
                new UpgradeTarget().withEngineVersion("1.3"));
        DBEngineVersion dbEngineVersionV2 = new DBEngineVersion().withValidUpgradeTarget(
                new UpgradeTarget().withEngineVersion("2.1"),
                new UpgradeTarget().withEngineVersion("2.3"));
        DescribeDBEngineVersionsResult describeDBEngineVersionsResult = new DescribeDBEngineVersionsResult()
                .withDBEngineVersions(dbEngineVersionV1, dbEngineVersionV2);
        when(rdsClient.describeDBEngineVersions(any())).thenReturn(describeDBEngineVersionsResult);

        Set<String> upgradeTargetVersions = underTest.getAllUpgradeTargetVersions(rdsClient, rdsEngineVersion);

        assertThat(upgradeTargetVersions).hasSize(4);
        assertThat(upgradeTargetVersions).containsOnly("1.1", "1.3", "2.1", "2.3");
        ArgumentCaptor<DescribeDBEngineVersionsRequest> rdsEngineVersionArgumentCaptor = ArgumentCaptor.forClass(DescribeDBEngineVersionsRequest.class);
        verify(rdsClient).describeDBEngineVersions(rdsEngineVersionArgumentCaptor.capture());
        DescribeDBEngineVersionsRequest describeDBEngineVersionsRequest = rdsEngineVersionArgumentCaptor.getValue();
        assertEquals("1.2", describeDBEngineVersionsRequest.getEngineVersion());
        assertEquals("postgres", describeDBEngineVersionsRequest.getEngine());
    }

    @Test
    void testGetAllUpgradeTargetsWhenRdsClientThrows() {
        AmazonRdsClient rdsClient = mock(AmazonRdsClient.class);
        RdsEngineVersion rdsEngineVersion = new RdsEngineVersion("1.0");
        when(rdsClient.describeDBEngineVersions(any())).thenThrow(RuntimeException.class);

        Assertions.assertThrows(CloudConnectorException.class, () ->
                underTest.getAllUpgradeTargetVersions(rdsClient, rdsEngineVersion)
        );
    }

    @Test
    void testGetUpgradeVersionsForTargetMajor() {
        Set<String> validUpgradeTargetVersions = Set.of("1.1", "1.2", "2.1", "2.2");
        Version targetVersion = () -> "2";

        Optional<RdsEngineVersion> validTargetVersions = underTest.getHighestUpgradeVersionForTargetMajorVersion(validUpgradeTargetVersions, targetVersion);

        assertTrue(validTargetVersions.isPresent());
        assertEquals("2.2", validTargetVersions.get().getVersion());
    }
}
