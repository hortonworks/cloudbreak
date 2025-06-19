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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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

import com.dyngr.exception.PollerStoppedException;
import com.sequenceiq.cloudbreak.cloud.aws.common.client.AmazonRdsClient;
import com.sequenceiq.cloudbreak.cloud.exception.CloudConnectorException;

import software.amazon.awssdk.services.rds.model.DescribeDbInstancesRequest;
import software.amazon.awssdk.services.rds.model.DescribeDbInstancesResponse;
import software.amazon.awssdk.services.rds.model.ModifyDbInstanceRequest;

@ExtendWith(MockitoExtension.class)
public class AwsRdsUpgradeOperationsTest {

    private static final String DB_INSTANCE_IDENTIFIER = "dbInstanceIdentifier";

    private static final String DB_PARAMETER_GROUP_NAME = "dbParameterGroupName";

    private static final String TARGET_VERSION = "targetVersion";

    @Mock
    private AwsRdsUpgradeWaitOperations awsRdsUpgradeWaitOperations;

    @Mock
    private AwsRdsUpgradeValidatorProvider awsRdsUpgradeValidatorProvider;

    @Mock
    private AwsRdsCustomParameterSupplier awsRdsCustomParameterSupplier;

    @InjectMocks
    private AwsRdsUpgradeOperations underTest;

    @Mock
    private AmazonRdsClient rdsClient;

    @Test
    void testDescribeRds() {
        DescribeDbInstancesResponse describeDbInstancesResponse = DescribeDbInstancesResponse.builder().build();
        when(rdsClient.describeDBInstances(any())).thenReturn(describeDbInstancesResponse);

        DescribeDbInstancesResponse result = underTest.describeRds(rdsClient, DB_INSTANCE_IDENTIFIER);

        assertEquals(result, describeDbInstancesResponse);
        ArgumentCaptor<DescribeDbInstancesRequest> describeDBInstancesRequestArgumentCaptor = ArgumentCaptor.forClass(DescribeDbInstancesRequest.class);
        verify(rdsClient).describeDBInstances(describeDBInstancesRequestArgumentCaptor.capture());
        DescribeDbInstancesRequest request = describeDBInstancesRequestArgumentCaptor.getValue();
        assertEquals(DB_INSTANCE_IDENTIFIER, request.dbInstanceIdentifier());
    }

    @Test
    void testUpgradeRds() {
        RdsEngineVersion targetVersion = new RdsEngineVersion(TARGET_VERSION);

        underTest.upgradeRds(rdsClient, targetVersion, DB_INSTANCE_IDENTIFIER, DB_PARAMETER_GROUP_NAME);

        ArgumentCaptor<ModifyDbInstanceRequest> modifyRequestCaptor = ArgumentCaptor.forClass(ModifyDbInstanceRequest.class);
        verify(rdsClient).modifyDBInstance(modifyRequestCaptor.capture());
        ModifyDbInstanceRequest firedRequest = modifyRequestCaptor.getValue();
        assertEquals(DB_INSTANCE_IDENTIFIER, firedRequest.dbInstanceIdentifier());
        assertEquals(TARGET_VERSION, firedRequest.engineVersion());
        assertTrue(firedRequest.allowMajorVersionUpgrade());
        assertTrue(firedRequest.applyImmediately());
        assertEquals(DB_PARAMETER_GROUP_NAME, firedRequest.dbParameterGroupName());
    }

    @Test
    void testUpgradeRdsWithoutDbParameterGroupName() {
        RdsEngineVersion targetVersion = new RdsEngineVersion(TARGET_VERSION);

        underTest.upgradeRds(rdsClient, targetVersion, DB_INSTANCE_IDENTIFIER, null);

        ArgumentCaptor<ModifyDbInstanceRequest> modifyRequestCaptor = ArgumentCaptor.forClass(ModifyDbInstanceRequest.class);
        verify(rdsClient).modifyDBInstance(modifyRequestCaptor.capture());
        ModifyDbInstanceRequest firedRequest = modifyRequestCaptor.getValue();
        assertEquals(DB_INSTANCE_IDENTIFIER, firedRequest.dbInstanceIdentifier());
        assertEquals(TARGET_VERSION, firedRequest.engineVersion());
        assertTrue(firedRequest.allowMajorVersionUpgrade());
        assertTrue(firedRequest.applyImmediately());
        assertNull(firedRequest.dbParameterGroupName());
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
        underTest.waitForRdsUpgrade(rdsClient, DB_INSTANCE_IDENTIFIER);

        InOrder inOrder = Mockito.inOrder(awsRdsUpgradeWaitOperations);
        ArgumentCaptor<DescribeDbInstancesRequest> describeDBInstancesRequestCaptor = ArgumentCaptor.forClass(DescribeDbInstancesRequest.class);
        inOrder.verify(awsRdsUpgradeWaitOperations).waitUntilUpgradeStarts(eq(rdsClient), describeDBInstancesRequestCaptor.capture());
        inOrder.verify(awsRdsUpgradeWaitOperations).waitUntilUpgradeFinishes(eq(rdsClient), describeDBInstancesRequestCaptor.capture());

        Set<String> dbInstanceIdentifiers = describeDBInstancesRequestCaptor.getAllValues().stream()
                .map(DescribeDbInstancesRequest::dbInstanceIdentifier)
                .collect(Collectors.toSet());
        assertThat(dbInstanceIdentifiers, hasSize(1));
        assertThat(dbInstanceIdentifiers, hasItem(DB_INSTANCE_IDENTIFIER));
    }

    @Test
    void testWaitForUpgradeWhenWaitingOnUpgradeStartTimesOut() {
        doThrow(PollerStoppedException.class).when(awsRdsUpgradeWaitOperations).waitUntilUpgradeStarts(eq(rdsClient), any());

        Assertions.assertThrows(PollerStoppedException.class, () ->
                underTest.waitForRdsUpgrade(rdsClient, DB_INSTANCE_IDENTIFIER)
        );

        verify(awsRdsUpgradeWaitOperations, never()).waitUntilUpgradeFinishes(any(), any());
    }
}
