package com.sequenceiq.cloudbreak.cloud.aws.connector.resource.upgrade;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import org.assertj.core.data.MapEntry;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.cloud.aws.common.client.AmazonRdsClient;
import com.sequenceiq.cloudbreak.cloud.aws.connector.resource.AwsRdsParameterGroupService;
import com.sequenceiq.cloudbreak.cloud.aws.connector.resource.upgrade.operation.AwsRdsUpgradeOperations;
import com.sequenceiq.cloudbreak.cloud.aws.connector.resource.upgrade.operation.AwsRdsUpgradeValidatorProvider;
import com.sequenceiq.cloudbreak.cloud.aws.connector.resource.upgrade.operation.RdsEngineVersion;
import com.sequenceiq.cloudbreak.cloud.aws.connector.resource.upgrade.operation.RdsInfo;
import com.sequenceiq.cloudbreak.cloud.aws.connector.resource.upgrade.operation.RdsInstanceStatusesToRdsStateConverter;
import com.sequenceiq.cloudbreak.cloud.aws.connector.resource.upgrade.operation.RdsState;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.exception.CloudConnectorException;
import com.sequenceiq.cloudbreak.cloud.model.AvailabilityZone;
import com.sequenceiq.cloudbreak.cloud.model.DatabaseServer;
import com.sequenceiq.cloudbreak.cloud.model.Location;
import com.sequenceiq.cloudbreak.common.database.Version;

import software.amazon.awssdk.services.rds.model.DBInstance;
import software.amazon.awssdk.services.rds.model.DescribeDbInstancesResponse;

@ExtendWith(MockitoExtension.class)
public class AwsRdsUpgradeStepsTest {

    private static final String DB_INSTANCE_IDENTIFIER = "dbInstanceIdentifier";

    private static final String INSTANCE_ARN = "instanceArn";

    private static final String STATUS_AVAILABLE = "available";

    private static final String ENGINE_VERSION = "engineVersion";

    private static final String CURRENT_VERSION = "currentVersion";

    private static final String TARGET_VERSION = "targetVersion";

    private static final String DB_PARAMETER_GROUP_NAME = "dbParameterGroupName";

    @Mock
    private AwsRdsUpgradeOperations awsRdsUpgradeOperations;

    @Mock
    private RdsInstanceStatusesToRdsStateConverter rdsInstanceStatusesToRdsStateConverter;

    @Mock
    private AwsRdsUpgradeValidatorProvider awsRdsUpgradeValidatorProvider;

    @InjectMocks
    private AwsRdsUpgradeSteps underTest;

    @Mock
    private AmazonRdsClient rdsClient;

    @Mock
    private AuthenticatedContext ac;

    @Mock
    private DatabaseServer databaseServer;

    @Mock
    private AwsRdsParameterGroupService awsRdsParameterGroupService;

    private final Version targetMajorVersion = () -> "majorVersion";

    @Test
    void testGetRdsInfo() {
        DescribeDbInstancesResponse describeDbInstancesResponse = getDescribeDBInstancesResult(true);
        when(awsRdsUpgradeOperations.describeRds(rdsClient, DB_INSTANCE_IDENTIFIER)).thenReturn(describeDbInstancesResponse);
        when(rdsInstanceStatusesToRdsStateConverter.convert(any())).thenReturn(RdsState.AVAILABLE);

        RdsInfo rdsInfo = underTest.getRdsInfo(rdsClient, DB_INSTANCE_IDENTIFIER);

        assertEquals(RdsState.AVAILABLE, rdsInfo.getRdsState());
        assertThat(rdsInfo.getDbArnToInstanceStatuses()).containsExactly(MapEntry.entry(INSTANCE_ARN, STATUS_AVAILABLE));
        assertThat(rdsInfo.getRdsEngineVersion().getVersion()).isEqualTo(ENGINE_VERSION);
    }

    @Test
    void testGetRdsInfoWhenValidationThrows() {
        DescribeDbInstancesResponse describeDbInstancesResponse = getDescribeDBInstancesResult(false);
        when(awsRdsUpgradeOperations.describeRds(rdsClient, DB_INSTANCE_IDENTIFIER)).thenReturn(describeDbInstancesResponse);
        doThrow(CloudConnectorException.class).when(awsRdsUpgradeValidatorProvider).validateClusterHasASingleVersion(any());

        assertThrows(CloudConnectorException.class, () ->
                underTest.getRdsInfo(rdsClient, DB_INSTANCE_IDENTIFIER)
        );
    }

    @Test
    void testUpgradeRds() {
        RdsEngineVersion currentVersion = new RdsEngineVersion(CURRENT_VERSION);
        RdsEngineVersion targetVersion = new RdsEngineVersion(TARGET_VERSION);
        AuthenticatedContext ac = mock(AuthenticatedContext.class);

        RdsInfo rdsInfo = new RdsInfo(RdsState.AVAILABLE, null, currentVersion);
        when(databaseServer.getServerId()).thenReturn(DB_INSTANCE_IDENTIFIER);
        when(databaseServer.isUseSslEnforcement()).thenReturn(true);
        when(awsRdsUpgradeValidatorProvider.getHighestUpgradeTargetVersion(rdsClient, targetMajorVersion, currentVersion))
                .thenReturn(targetVersion);
        when(awsRdsParameterGroupService.createParameterGroupWithCustomSettings(ac, rdsClient, databaseServer, targetVersion))
                .thenReturn(DB_PARAMETER_GROUP_NAME);
        CloudContext cloudContext = mock(CloudContext.class);
        when(ac.getCloudContext()).thenReturn(cloudContext);
        Location location = mock(Location.class);
        when(cloudContext.getLocation()).thenReturn(location);
        when(location.getAvailabilityZone()).thenReturn(AvailabilityZone.availabilityZone("az"));

        underTest.upgradeRds(ac, rdsClient, databaseServer, rdsInfo, targetMajorVersion);

        verify(awsRdsUpgradeOperations).upgradeRds(rdsClient, targetVersion, DB_INSTANCE_IDENTIFIER, DB_PARAMETER_GROUP_NAME);
    }

    @Test
    void testUpgradeRdsWithoutEnforceSsl() {
        RdsEngineVersion currentVersion = new RdsEngineVersion(CURRENT_VERSION);
        RdsEngineVersion targetVersion = new RdsEngineVersion(TARGET_VERSION);
        RdsInfo rdsInfo = new RdsInfo(RdsState.AVAILABLE, null, currentVersion);
        when(databaseServer.getServerId()).thenReturn(DB_INSTANCE_IDENTIFIER);
        when(awsRdsUpgradeValidatorProvider.getHighestUpgradeTargetVersion(rdsClient, targetMajorVersion, currentVersion)).thenReturn(targetVersion);

        underTest.upgradeRds(null, rdsClient, databaseServer, rdsInfo, targetMajorVersion);

        verify(awsRdsParameterGroupService, never()).createParameterGroupWithCustomSettings(ac, rdsClient, databaseServer, targetVersion);
        verify(awsRdsUpgradeOperations).upgradeRds(rdsClient, targetVersion, DB_INSTANCE_IDENTIFIER, null);
    }

    @Test
    void testUpgradeRdsWhenGetHighestUpgradeThrowsThenNoUpgrade() {
        RdsEngineVersion currentVersion = new RdsEngineVersion(CURRENT_VERSION);
        RdsInfo rdsInfo = new RdsInfo(RdsState.AVAILABLE, null, currentVersion);
        when(awsRdsUpgradeValidatorProvider.getHighestUpgradeTargetVersion(rdsClient, targetMajorVersion, currentVersion))
                .thenThrow(CloudConnectorException.class);

        assertThrows(CloudConnectorException.class, () ->
                underTest.upgradeRds(null, rdsClient, databaseServer, rdsInfo, targetMajorVersion)
        );

        verify(awsRdsParameterGroupService, never()).createParameterGroupWithCustomSettings(any(), any(), any(), any());
        verify(awsRdsUpgradeOperations, never()).upgradeRds(any(), any(), anyString(), any());
    }

    @Test
    void testUpgradeRdsWhenCreateParameterGroupThrowsThenNoUpgrade() {
        RdsEngineVersion currentVersion = new RdsEngineVersion(CURRENT_VERSION);
        RdsEngineVersion targetVersion = new RdsEngineVersion(TARGET_VERSION);
        RdsInfo rdsInfo = new RdsInfo(RdsState.AVAILABLE, null, currentVersion);
        when(databaseServer.isUseSslEnforcement()).thenReturn(true);
        when(awsRdsUpgradeValidatorProvider.getHighestUpgradeTargetVersion(rdsClient, targetMajorVersion, currentVersion)).thenReturn(targetVersion);
        when(awsRdsParameterGroupService.createParameterGroupWithCustomSettings(ac, rdsClient, databaseServer, targetVersion))
                .thenThrow(RuntimeException.class);

        assertThrows(RuntimeException.class, () ->
                underTest.upgradeRds(null, rdsClient, databaseServer, rdsInfo, targetMajorVersion)
        );

        verify(awsRdsUpgradeOperations, never()).upgradeRds(any(), any(), anyString(), any());
    }

    @Test
    void testWaitForUpgrade() {
        underTest.waitForUpgrade(rdsClient, databaseServer);

        verify(awsRdsUpgradeOperations).waitForRdsUpgrade(rdsClient, databaseServer.getServerId());
    }

    private DescribeDbInstancesResponse getDescribeDBInstancesResult(boolean containsResult) {
        List<DBInstance> dbInstaces = new ArrayList<>();
        if (containsResult) {
            DBInstance dbInstance = DBInstance.builder()
                    .dbInstanceStatus(STATUS_AVAILABLE)
                    .dbInstanceArn(INSTANCE_ARN)
                    .engineVersion(ENGINE_VERSION)
                    .build();
            dbInstaces.add(dbInstance);
        }
        return DescribeDbInstancesResponse.builder().dbInstances(dbInstaces).build();
    }

}
