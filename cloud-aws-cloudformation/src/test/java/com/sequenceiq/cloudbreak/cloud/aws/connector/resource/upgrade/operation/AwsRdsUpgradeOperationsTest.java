package com.sequenceiq.cloudbreak.cloud.aws.connector.resource.upgrade.operation;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import com.amazonaws.services.rds.model.DescribeDBInstancesRequest;
import com.amazonaws.services.rds.model.DescribeDBInstancesResult;
import com.amazonaws.services.rds.model.ModifyDBInstanceRequest;
import com.dyngr.exception.PollerStoppedException;
import com.sequenceiq.cloudbreak.cloud.aws.common.client.AmazonRdsClient;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.exception.CloudConnectorException;
import com.sequenceiq.cloudbreak.common.database.Version;

@ExtendWith(MockitoExtension.class)
public class AwsRdsUpgradeOperationsTest {

    private static final String DB_INSTANCE_IDENTIFIER = "dbInstanceIdentifier";

    private static final String DB_PARAMETER_GROUP_NAME = "dbParameterGroupName";

    private static final String TARGET_VERSION = "targetVersion";

    @Mock
    private AwsRdsVersionOperations awsRdsVersionOperations;

    @Mock
    private AwsRdsUpgradeWaitOperations awsRdsUpgradeWaitOperations;

    @Mock
    private AwsRdsUpgradeValidatorService awsRdsUpgradeValidatorService;

    @Mock
    private AwsRdsCustomParameterSupplier awsRdsCustomParameterSupplier;

    @InjectMocks
    private AwsRdsUpgradeOperations underTest;

    @Mock
    private AmazonRdsClient rdsClient;

    @Test
    void testDescribeRds() {
        DescribeDBInstancesResult describeDBInstancesResult = new DescribeDBInstancesResult();
        when(rdsClient.describeDBInstances(any())).thenReturn(describeDBInstancesResult);

        DescribeDBInstancesResult result = underTest.describeRds(rdsClient, DB_INSTANCE_IDENTIFIER);

        assertEquals(result, describeDBInstancesResult);
        ArgumentCaptor<DescribeDBInstancesRequest> describeDBInstancesRequestArgumentCaptor = ArgumentCaptor.forClass(DescribeDBInstancesRequest.class);
        verify(rdsClient).describeDBInstances(describeDBInstancesRequestArgumentCaptor.capture());
        DescribeDBInstancesRequest request = describeDBInstancesRequestArgumentCaptor.getValue();
        assertEquals(DB_INSTANCE_IDENTIFIER, request.getDBInstanceIdentifier());
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
        verify(awsRdsUpgradeValidatorService).validateUpgradePresentForTargetMajorVersion(Optional.of(highestUpgradeTargetForMajorVersion));
    }

    @Test
    void testGetHighestUpgradeVersionWhenValidationThrows() {
        Version targetVersion = () -> TARGET_VERSION;
        RdsEngineVersion currentVersion = new RdsEngineVersion("currentVersion");
        Set<String> validUpgradeTargets = Set.of("v1", "v2");
        when(awsRdsVersionOperations.getAllUpgradeTargetVersions(rdsClient, currentVersion)).thenReturn(validUpgradeTargets);
        when(awsRdsVersionOperations.getHighestUpgradeVersionForTargetMajorVersion(validUpgradeTargets, targetVersion)).thenReturn(Optional.empty());
        doThrow(CloudConnectorException.class).when(awsRdsUpgradeValidatorService)
                .validateUpgradePresentForTargetMajorVersion(Optional.empty());

        Assertions.assertThrows(CloudConnectorException.class, () ->
                underTest.getHighestUpgradeTargetVersion(rdsClient, targetVersion, currentVersion)
        );
    }

    @Test
    void testUpgradeRds() {
        RdsEngineVersion targetVersion = new RdsEngineVersion(TARGET_VERSION);

        underTest.upgradeRds(rdsClient, targetVersion, DB_INSTANCE_IDENTIFIER, DB_PARAMETER_GROUP_NAME);

        ArgumentCaptor<ModifyDBInstanceRequest> modifyRequestCaptor = ArgumentCaptor.forClass(ModifyDBInstanceRequest.class);
        verify(rdsClient).modifyDBInstance(modifyRequestCaptor.capture());
        ModifyDBInstanceRequest firedRequest = modifyRequestCaptor.getValue();
        assertEquals(DB_INSTANCE_IDENTIFIER, firedRequest.getDBInstanceIdentifier());
        assertEquals(TARGET_VERSION, firedRequest.getEngineVersion());
        assertTrue(firedRequest.getAllowMajorVersionUpgrade());
        assertTrue(firedRequest.getApplyImmediately());
        assertEquals(DB_PARAMETER_GROUP_NAME, firedRequest.getDBParameterGroupName());
    }

    @Test
    void testUpgradeRdsWithoutDbParameterGroupName() {
        RdsEngineVersion targetVersion = new RdsEngineVersion(TARGET_VERSION);

        underTest.upgradeRds(rdsClient, targetVersion, DB_INSTANCE_IDENTIFIER, null);

        ArgumentCaptor<ModifyDBInstanceRequest> modifyRequestCaptor = ArgumentCaptor.forClass(ModifyDBInstanceRequest.class);
        verify(rdsClient).modifyDBInstance(modifyRequestCaptor.capture());
        ModifyDBInstanceRequest firedRequest = modifyRequestCaptor.getValue();
        assertEquals(DB_INSTANCE_IDENTIFIER, firedRequest.getDBInstanceIdentifier());
        assertEquals(TARGET_VERSION, firedRequest.getEngineVersion());
        assertTrue(firedRequest.getAllowMajorVersionUpgrade());
        assertTrue(firedRequest.getApplyImmediately());
        assertNull(firedRequest.getDBParameterGroupName());
    }

    @Test
    void testUpgradeRdsWhenExceptionThenThrows() {
        RdsEngineVersion targetVersion = new RdsEngineVersion(TARGET_VERSION);
        when(rdsClient.modifyDBInstance(any())).thenThrow(new RuntimeException("myException"));

        org.assertj.core.api.Assertions.assertThatCode(() -> underTest.upgradeRds(rdsClient, targetVersion, DB_INSTANCE_IDENTIFIER, DB_PARAMETER_GROUP_NAME))
                .isInstanceOf(CloudConnectorException.class)
                .hasMessageContaining("myException");
    }

    @Test
    void testWaitForUpgrade() {
        AuthenticatedContext ac = mock(AuthenticatedContext.class);

        underTest.waitForRdsUpgrade(ac, rdsClient, DB_INSTANCE_IDENTIFIER);

        InOrder inOrder = Mockito.inOrder(awsRdsUpgradeWaitOperations);
        ArgumentCaptor<DescribeDBInstancesRequest> describeDBInstancesRequestCaptor = ArgumentCaptor.forClass(DescribeDBInstancesRequest.class);
        inOrder.verify(awsRdsUpgradeWaitOperations).waitUntilUpgradeStarts(eq(rdsClient), describeDBInstancesRequestCaptor.capture());
        inOrder.verify(awsRdsUpgradeWaitOperations).waitUntilUpgradeFinishes(eq(ac), eq(rdsClient), describeDBInstancesRequestCaptor.capture());

        Set<String> dbInstanceIdentifiers = describeDBInstancesRequestCaptor.getAllValues().stream()
                .map(DescribeDBInstancesRequest::getDBInstanceIdentifier)
                .collect(Collectors.toSet());
        assertThat(dbInstanceIdentifiers, hasSize(1));
        assertThat(dbInstanceIdentifiers, hasItem(DB_INSTANCE_IDENTIFIER));
    }

    @Test
    void testWaitForUpgradeWhenWaitingOnUpgradeStartTimesOut() {
        AuthenticatedContext ac = mock(AuthenticatedContext.class);
        doThrow(PollerStoppedException.class).when(awsRdsUpgradeWaitOperations).waitUntilUpgradeStarts(eq(rdsClient), any());

        Assertions.assertThrows(PollerStoppedException.class, () ->
                underTest.waitForRdsUpgrade(ac, rdsClient, DB_INSTANCE_IDENTIFIER)
        );

        verify(awsRdsUpgradeWaitOperations, never()).waitUntilUpgradeFinishes(any(), any(), any());
    }
}
