package com.sequenceiq.cloudbreak.cloud.aws.connector.resource.upgrade.operation;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.cloud.aws.common.client.AmazonRdsClient;
import com.sequenceiq.cloudbreak.cloud.aws.connector.resource.AwsRdsStatusLookupService;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.exception.CloudConnectorException;
import com.sequenceiq.cloudbreak.cloud.model.DatabaseServer;
import com.sequenceiq.cloudbreak.cloud.model.DatabaseStack;
import com.sequenceiq.cloudbreak.common.database.TargetMajorVersion;
import com.sequenceiq.cloudbreak.common.database.Version;

import software.amazon.awssdk.services.rds.model.DBInstance;
import software.amazon.awssdk.services.rds.model.DBParameterGroupStatus;
import software.amazon.awssdk.services.rds.model.DescribeDbInstancesResponse;

@ExtendWith(MockitoExtension.class)
public class AwsRdsUpgradeValidatorProviderTest {

    private static final String TARGET_VERSION = "targetVersion";

    @Mock
    private AwsRdsStatusLookupService awsRdsStatusLookupService;

    @Mock
    private AwsRdsVersionOperations awsRdsVersionOperations;

    @Mock
    private AmazonRdsClient rdsClient;

    @InjectMocks
    private final AwsRdsUpgradeValidatorProvider underTest = new AwsRdsUpgradeValidatorProvider();

    @Test
    void testValidateRdsIsAvailableOrUpgrading() {
        RdsInfo rdsInfo = new RdsInfo(RdsState.AVAILABLE, Map.of(), new RdsEngineVersion("10"));

        assertDoesNotThrow(() ->
                underTest.validateRdsIsAvailableOrUpgrading(rdsInfo)
        );
    }

    @ParameterizedTest
    @EnumSource(value = RdsState.class, names = {"AVAILABLE", "UPGRADING"}, mode = EnumSource.Mode.EXCLUDE)
    void testValidateRdsIsAvailableOrUpgradingWhenStateNotOk(RdsState rdsState) {
        RdsInfo rdsInfo = new RdsInfo(rdsState, Map.of(), new RdsEngineVersion("10"));

        assertThrows(CloudConnectorException.class, () ->
                underTest.validateRdsIsAvailableOrUpgrading(rdsInfo)
        );
    }

    @Test
    void testValidateUpgradePresentForTargetMajorVersion() {
        Optional<RdsEngineVersion> upgradeTargetsForMajorVersion = Optional.of(new RdsEngineVersion("v1"));

        assertDoesNotThrow(() ->
                underTest.validateUpgradePresentForTargetMajorVersion(upgradeTargetsForMajorVersion)
        );
    }

    @Test
    void testValidateUpgradePresentForTargetMajorVersionWhenNoTargetVersionPresent() {
        Optional<RdsEngineVersion> upgradeTargetsForMajorVersion = Optional.empty();

        assertThrows(CloudConnectorException.class, () ->
                underTest.validateUpgradePresentForTargetMajorVersion(upgradeTargetsForMajorVersion)
        );
    }

    @Test
    void testClusterHasSingleVersion() {
        Set<String> dbVersions = Set.of("version");

        underTest.validateClusterHasASingleVersion(dbVersions);
    }

    @Test
    void testClusterHasSingleVersionWhenMultipleVersions() {
        Set<String> dbVersions = Set.of("v1", "v2");

        assertThrows(CloudConnectorException.class, () ->
                underTest.validateClusterHasASingleVersion(dbVersions)
        );
    }

    @Test
    void testClusterHasSingleVersionWhenVersionsEmpty() {
        Set<String> dbVersions = Set.of();

        assertThrows(CloudConnectorException.class, () ->
                underTest.validateClusterHasASingleVersion(dbVersions)
        );
    }

    @Test
    void validateCustomPropertiesAddedWhenNoCustomPropertiesAdded() {
        // GIVEN
        DatabaseStack stack = mock(DatabaseStack.class);
        DatabaseServer databaseServer = mock(DatabaseServer.class);
        when(stack.getDatabaseServer()).thenReturn(databaseServer);
        DescribeDbInstancesResponse describeDBInstancesResult = DescribeDbInstancesResponse.builder()
                .dbInstances(DBInstance.builder()
                        .dbParameterGroups(DBParameterGroupStatus.builder().dbParameterGroupName("default.test").build())
                        .build())
                .build();
        AuthenticatedContext authenticatedContext = mock(AuthenticatedContext.class);

        when(awsRdsStatusLookupService.getDescribeDBInstancesResult(authenticatedContext, stack)).thenReturn(describeDBInstancesResult);
        // WHEN and THEN
        assertDoesNotThrow(() -> underTest.validateCustomPropertiesAdded(authenticatedContext, stack));
    }

    @Test
    void validateCustomPropertiesAddedWhenCustomPropertiesAdded() {
        // GIVEN
        DatabaseStack stack = mock(DatabaseStack.class);
        DatabaseServer databaseServer = mock(DatabaseServer.class);
        when(stack.getDatabaseServer()).thenReturn(databaseServer);
        DescribeDbInstancesResponse describeDBInstancesResult = DescribeDbInstancesResponse.builder()
                .dbInstances(DBInstance.builder()
                        .dbParameterGroups(DBParameterGroupStatus.builder().dbParameterGroupName("custom.test").build())
                        .build())
                .build();
        AuthenticatedContext authenticatedContext = mock(AuthenticatedContext.class);

        when(awsRdsStatusLookupService.getDescribeDBInstancesResult(authenticatedContext, stack)).thenReturn(describeDBInstancesResult);
        // WHEN
        CloudConnectorException actualException = assertThrows(CloudConnectorException.class,
                () -> underTest.validateCustomPropertiesAdded(authenticatedContext, stack));
        // THEN
        assertTrue(actualException.getMessage().contains("custom.test"));
    }

    @Test
    void testGetHighestUpgradeVersion() {
        Version targetVersion = () -> TARGET_VERSION;
        RdsEngineVersion currentVersion = new RdsEngineVersion("currentVersion");
        Set<String> validUpgradeTargets = Set.of("v1", "v2");
        when(awsRdsVersionOperations.getAllUpgradeTargetVersions(rdsClient, currentVersion)).thenReturn(validUpgradeTargets);
        RdsEngineVersion highestUpgradeTargetForMajorVersion = new RdsEngineVersion("v1");
        when(awsRdsVersionOperations.getHighestUpgradeVersionForTargetMajorVersion(validUpgradeTargets, targetVersion))
                .thenReturn(Optional.of(highestUpgradeTargetForMajorVersion));

        RdsEngineVersion result = underTest.getHighestUpgradeTargetVersion(rdsClient, targetVersion, currentVersion);

        assertEquals(highestUpgradeTargetForMajorVersion, result);
    }

    @Test
    void testGetHighestUpgradeVersionWhenValidationThrows() {
        Version targetVersion = () -> TARGET_VERSION;
        RdsEngineVersion currentVersion = new RdsEngineVersion("currentVersion");
        Set<String> validUpgradeTargets = Set.of("v1", "v2");
        when(awsRdsVersionOperations.getAllUpgradeTargetVersions(rdsClient, currentVersion)).thenReturn(validUpgradeTargets);
        when(awsRdsVersionOperations.getHighestUpgradeVersionForTargetMajorVersion(validUpgradeTargets, targetVersion)).thenReturn(Optional.empty());

        assertThrows(CloudConnectorException.class, () ->
                underTest.getHighestUpgradeTargetVersion(rdsClient, targetVersion, currentVersion)
        );
    }

    @Test
    void testIsRdsMajorVersionSmallerThanTarget() {
        RdsInfo rdsInfo = new RdsInfo(RdsState.AVAILABLE, Map.of(), new RdsEngineVersion("11"));
        Version targetVersion = TargetMajorVersion.VERSION14;
        // Should return true since 11 < 14
        assertTrue(underTest.isRdsMajorVersionSmallerThanTarget(rdsInfo, targetVersion));

        rdsInfo = new RdsInfo(RdsState.AVAILABLE, Map.of(), new RdsEngineVersion("13.5"));
        // Should return true since 13.5 < 14
        assertTrue(underTest.isRdsMajorVersionSmallerThanTarget(rdsInfo, targetVersion));

        RdsInfo rdsInfoEqual = new RdsInfo(RdsState.AVAILABLE, Map.of(), new RdsEngineVersion("14"));
        // Should return false since 14 == 14
        assertFalse(underTest.isRdsMajorVersionSmallerThanTarget(rdsInfoEqual, targetVersion));

        RdsInfo rdsInfoGreater = new RdsInfo(RdsState.AVAILABLE, Map.of(), new RdsEngineVersion("15"));
        // Should return false since 15 > 14
        assertFalse(underTest.isRdsMajorVersionSmallerThanTarget(rdsInfoGreater, targetVersion));
    }

    @Test
    void validateCustomPropertiesAddedWhenSslEnforcementEnabled() {
        // GIVEN
        DatabaseStack stack = mock(DatabaseStack.class);
        DatabaseServer databaseServer = mock(DatabaseServer.class);
        when(stack.getDatabaseServer()).thenReturn(databaseServer);
        when(databaseServer.isUseSslEnforcement()).thenReturn(true);
        AuthenticatedContext authenticatedContext = mock(AuthenticatedContext.class);

        // WHEN and THEN
        assertDoesNotThrow(() -> underTest.validateCustomPropertiesAdded(authenticatedContext, stack));
        verify(awsRdsStatusLookupService, never()).getDescribeDBInstancesResult(authenticatedContext, stack);
    }

    @Test
    void validateCustomPropertiesAddedWhenDbInstancesListIsEmpty() {
        // GIVEN
        DatabaseStack stack = mock(DatabaseStack.class);
        DatabaseServer databaseServer = mock(DatabaseServer.class);
        when(stack.getDatabaseServer()).thenReturn(databaseServer);
        when(databaseServer.isUseSslEnforcement()).thenReturn(false);
        when(databaseServer.getServerId()).thenReturn("test-server-id");
        DescribeDbInstancesResponse describeDBInstancesResult = DescribeDbInstancesResponse.builder()
                .dbInstances(Collections.emptyList())
                .build();
        AuthenticatedContext authenticatedContext = mock(AuthenticatedContext.class);

        when(awsRdsStatusLookupService.getDescribeDBInstancesResult(authenticatedContext, stack)).thenReturn(describeDBInstancesResult);

        // WHEN and THEN
        assertDoesNotThrow(() -> underTest.validateCustomPropertiesAdded(authenticatedContext, stack));
        verify(awsRdsStatusLookupService).getDescribeDBInstancesResult(authenticatedContext, stack);
    }
}
