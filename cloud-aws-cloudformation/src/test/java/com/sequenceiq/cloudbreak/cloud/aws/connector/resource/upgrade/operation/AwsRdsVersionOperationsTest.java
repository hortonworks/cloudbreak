package com.sequenceiq.cloudbreak.cloud.aws.connector.resource.upgrade.operation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.cloud.aws.common.client.AmazonRdsClient;
import com.sequenceiq.cloudbreak.cloud.exception.CloudConnectorException;
import com.sequenceiq.cloudbreak.cloud.model.DatabaseEngine;
import com.sequenceiq.cloudbreak.common.database.Version;

import software.amazon.awssdk.awscore.exception.AwsErrorDetails;
import software.amazon.awssdk.services.rds.model.DBEngineVersion;
import software.amazon.awssdk.services.rds.model.DescribeDbEngineVersionsRequest;
import software.amazon.awssdk.services.rds.model.DescribeDbEngineVersionsResponse;
import software.amazon.awssdk.services.rds.model.RdsException;
import software.amazon.awssdk.services.rds.model.UpgradeTarget;

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
        assertThrows(IllegalStateException.class, () ->
                underTest.getDBParameterGroupFamily(DatabaseEngine.POSTGRESQL, null)
        );
    }

    @Test
    void getDBParameterGroupFamilyTestWhenPgSqlAndVersionIsEmpty() {
        assertThrows(IllegalStateException.class, () ->
                underTest.getDBParameterGroupFamily(DatabaseEngine.POSTGRESQL, "")
        );
    }

    // Note: There is no easy way to test the "bad engine variant" case as enum classes are final and hard to mock.
    @Test
    void getDBParameterGroupFamilyTestWhenPgSqlAndBadVersionFormat() {
        assertThrows(IllegalStateException.class, () ->
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
        assertThrows(NumberFormatException.class, () ->
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
                {"version 18.1", "18.1"},
                {"version 123.1", "123.1"},
        };
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("unsupportedMajorVersionDataProvider")
    void getDBParameterGroupFamilyTestWhenPgSqlAndUnsupportedMajorVersion(String testCaseName, String version) {
        assertThrows(IllegalStateException.class, () ->
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
                {"version 14.1", "14.1", "postgres14"},
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
        DBEngineVersion dbEngineVersionV1 = DBEngineVersion.builder().validUpgradeTarget(
                UpgradeTarget.builder().engineVersion("1.1").build(),
                UpgradeTarget.builder().engineVersion("1.3").build()).build();
        DBEngineVersion dbEngineVersionV2 = DBEngineVersion.builder().validUpgradeTarget(
                UpgradeTarget.builder().engineVersion("2.1").build(),
                UpgradeTarget.builder().engineVersion("2.3").build()).build();
        DescribeDbEngineVersionsResponse describeDbEngineVersionsResponse = DescribeDbEngineVersionsResponse.builder()
                .dbEngineVersions(dbEngineVersionV1, dbEngineVersionV2).build();
        when(rdsClient.describeDBEngineVersions(any())).thenReturn(describeDbEngineVersionsResponse);

        Set<String> upgradeTargetVersions = underTest.getAllUpgradeTargetVersions(rdsClient, rdsEngineVersion);

        assertThat(upgradeTargetVersions).hasSize(4);
        assertThat(upgradeTargetVersions).containsOnly("1.1", "1.3", "2.1", "2.3");
        ArgumentCaptor<DescribeDbEngineVersionsRequest> rdsEngineVersionArgumentCaptor = ArgumentCaptor.forClass(DescribeDbEngineVersionsRequest.class);
        verify(rdsClient).describeDBEngineVersions(rdsEngineVersionArgumentCaptor.capture());
        DescribeDbEngineVersionsRequest describeDBEngineVersionsRequest = rdsEngineVersionArgumentCaptor.getValue();
        assertEquals("1.2", describeDBEngineVersionsRequest.engineVersion());
        assertEquals("postgres", describeDBEngineVersionsRequest.engine());
    }

    @Test
    void testGetAllUpgradeTargetsShouldThrowExceptionWhenDescribeDBEngineVersionsThrowsAccessDenied() {
        AmazonRdsClient rdsClient = mock(AmazonRdsClient.class);
        RdsEngineVersion rdsEngineVersion = new RdsEngineVersion("1.0");
        RdsException exception = (RdsException) RdsException.builder().awsErrorDetails(AwsErrorDetails.builder()
                .errorMessage("error").errorCode("AccessDenied").build()).build();
        when(rdsClient.describeDBEngineVersions(any())).thenThrow(exception);

        String message = assertThrows(CloudConnectorException.class, () -> underTest.getAllUpgradeTargetVersions(rdsClient, rdsEngineVersion)).getMessage();
        assertEquals("Could not query valid upgrade targets because user is not authorized to perform rds:DescribeDBEngineVersions action.", message);
    }

    @Test
    void testGetAllUpgradeTargetsWhenRdsClientThrowsOtherAmazonRDSException() {
        AmazonRdsClient rdsClient = mock(AmazonRdsClient.class);
        RdsEngineVersion rdsEngineVersion = new RdsEngineVersion("1.0");
        RdsException exception = (RdsException) RdsException.builder().awsErrorDetails(AwsErrorDetails.builder()
                .errorMessage("error").errorCode("Something").build()).build();
        when(rdsClient.describeDBEngineVersions(any())).thenThrow(exception);

        CloudConnectorException ex = assertThrows(CloudConnectorException.class, () ->
                underTest.getAllUpgradeTargetVersions(rdsClient, rdsEngineVersion));

        assertEquals("Exception occurred when querying valid upgrade targets: error " +
                "(Service: null, Status Code: 0, Request ID: null)", ex.getMessage());
    }

    @Test
    void testGetAllUpgradeTargetsWhenRdsClientThrowsOtherException() {
        AmazonRdsClient rdsClient = mock(AmazonRdsClient.class);
        RdsEngineVersion rdsEngineVersion = new RdsEngineVersion("1.0");
        RuntimeException exception = new RuntimeException("error");
        when(rdsClient.describeDBEngineVersions(any())).thenThrow(exception);

        CloudConnectorException ex = assertThrows(CloudConnectorException.class, () ->
                underTest.getAllUpgradeTargetVersions(rdsClient, rdsEngineVersion));

        assertEquals("Exception occurred when querying valid upgrade targets: error", ex.getMessage());
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
