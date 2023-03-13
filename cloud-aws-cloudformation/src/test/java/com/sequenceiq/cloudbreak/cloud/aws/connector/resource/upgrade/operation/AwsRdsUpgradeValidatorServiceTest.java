package com.sequenceiq.cloudbreak.cloud.aws.connector.resource.upgrade.operation;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

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

import com.sequenceiq.cloudbreak.cloud.aws.connector.resource.AwsRdsStatusLookupService;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.exception.CloudConnectorException;
import com.sequenceiq.cloudbreak.cloud.model.DatabaseServer;
import com.sequenceiq.cloudbreak.cloud.model.DatabaseStack;

import software.amazon.awssdk.services.rds.model.DBInstance;
import software.amazon.awssdk.services.rds.model.DBParameterGroupStatus;
import software.amazon.awssdk.services.rds.model.DescribeDbInstancesResponse;

@ExtendWith(MockitoExtension.class)
public class AwsRdsUpgradeValidatorServiceTest {

    @Mock
    private AwsRdsStatusLookupService awsRdsStatusLookupService;

    @InjectMocks
    private final AwsRdsUpgradeValidatorService underTest = new AwsRdsUpgradeValidatorService();

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
}
