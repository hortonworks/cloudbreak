package com.sequenceiq.cloudbreak.cloud.aws.connector.resource.upgrade.operation;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.core.Is.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.amazonaws.services.rds.model.DBEngineVersion;
import com.amazonaws.services.rds.model.DBInstance;
import com.amazonaws.services.rds.model.DescribeDBEngineVersionsResult;
import com.amazonaws.services.rds.model.DescribeDBInstancesRequest;
import com.amazonaws.services.rds.model.DescribeDBInstancesResult;
import com.amazonaws.services.rds.model.ModifyDBInstanceRequest;
import com.amazonaws.services.rds.model.Parameter;
import com.amazonaws.services.rds.model.UpgradeTarget;
import com.amazonaws.waiters.Waiter;
import com.dyngr.exception.PollerException;
import com.sequenceiq.cloudbreak.cloud.aws.common.client.AmazonRdsClient;
import com.sequenceiq.cloudbreak.cloud.aws.scheduler.CustomAmazonWaiterProvider;
import com.sequenceiq.cloudbreak.cloud.aws.util.poller.upgrade.UpgradeStartPoller;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.exception.CloudConnectorException;
import com.sequenceiq.cloudbreak.cloud.model.DatabaseEngine;
import com.sequenceiq.cloudbreak.cloud.model.DatabaseServer;

import reactor.fn.tuple.Tuple2;

@ExtendWith(MockitoExtension.class)
public class AwsRdsUpgradeOperationsTest {

    private static final String ENGINE_VERSION_1_2 = "1.2";

    private static final String ENGINE_VERSION_1_3 = "1.3";

    private static final String DB_INSTANCE_IDENTIFIER = "dbInstanceIdentifier";

    private static final String DB_PARAMETER_GROUP_NAME = "dbParameterGroupName";

    private static final String DB_SERVER_ID = "dbServerId";

    private static final String PARAMETER_GROUP_FAMILY = "parameterGroupFamily";

    @Mock
    private CustomAmazonWaiterProvider customAmazonWaiterProvider;

    @Mock
    private UpgradeStartPoller upgradeStartPoller;

    @Mock
    private AwsRdsVersionOperations awsRdsVersionOperations;

    @InjectMocks
    private AwsRdsUpgradeOperations underTest;

    @Mock
    private AmazonRdsClient rdsClient;

    @Test
    void testGetCurrentDbEngineVersions() {
        when(rdsClient.describeDBInstances(any())).thenReturn(setupDescribeDbInstancesResult(ENGINE_VERSION_1_2));

        String currentDbEngineVersion = underTest.getCurrentDbEngineVersion(rdsClient, DB_INSTANCE_IDENTIFIER);

        assertEquals(ENGINE_VERSION_1_2, currentDbEngineVersion);
    }

    @Test
    void testGetCurrentDbEngineVersionsWhenNoResultThenException() {
        when(rdsClient.describeDBInstances(any())).thenReturn(setupDescribeDbInstancesResult());

        Assertions.assertThrows(CloudConnectorException.class, () ->
                underTest.getCurrentDbEngineVersion(rdsClient, DB_INSTANCE_IDENTIFIER)
        );

    }

    @Test
    void testGetCurrentDbEngineVersionsWhenMultipleResultsThenException() {
        when(rdsClient.describeDBInstances(any())).thenReturn(setupDescribeDbInstancesResult(ENGINE_VERSION_1_2, ENGINE_VERSION_1_3));

        Assertions.assertThrows(CloudConnectorException.class, () ->
                underTest.getCurrentDbEngineVersion(rdsClient, DB_INSTANCE_IDENTIFIER)
        );

    }

    @Test
    void testGetUpgradeTargetVersions() {
        when(rdsClient.describeDBEngineVersions(any())).thenReturn(setupDescribeDbEngineVersionResults(Tuple2.of(ENGINE_VERSION_1_2, Set.of("2.1", "2.2"))));

        Set<String> upgradeTargetVersionList = underTest.getUpgradeTargetVersions(rdsClient, ENGINE_VERSION_1_2);

        assertThat(upgradeTargetVersionList, hasSize(2));
        assertThat(upgradeTargetVersionList, contains("2.1", "2.2"));
    }

    @Test
    void testGetUpgradeTargetVersionsWhenCurrentVersionIsNotPresentThenReturnsEmptySet() {
        when(rdsClient.describeDBEngineVersions(any())).thenReturn(setupDescribeDbEngineVersionResults(Tuple2.of(ENGINE_VERSION_1_3, Set.of("2.1"))));

        Set<String> upgradeTargetVersionList = underTest.getUpgradeTargetVersions(rdsClient, ENGINE_VERSION_1_2);

        assertThat(upgradeTargetVersionList, is(empty()));
    }

    @Test
    void testGetUpgradeTargetVersionsWhenMultipleMatchesThenReturnsAll() {
        when(rdsClient.describeDBEngineVersions(any())).thenReturn(setupDescribeDbEngineVersionResults(
                        Tuple2.of(ENGINE_VERSION_1_2, Set.of("2.1")),
                        Tuple2.of(ENGINE_VERSION_1_2, Set.of("2.2"))
                )
        );

        Set<String> upgradeTargetVersionList = underTest.getUpgradeTargetVersions(rdsClient, ENGINE_VERSION_1_2);

        assertThat(upgradeTargetVersionList, hasSize(2));
        assertThat(upgradeTargetVersionList, contains("2.1", "2.2"));
    }

    @Test
    void testGetUpgradeTargetVersionsWhenExceptionThenThrows() {
        when(rdsClient.describeDBEngineVersions(any())).thenThrow(new RuntimeException("MyException"));

        Assertions.assertThrows(CloudConnectorException.class, () ->
                underTest.getUpgradeTargetVersions(rdsClient, ENGINE_VERSION_1_2)
        );
    }

    @Test
    void testUpgradeRds() {
        underTest.upgradeRds(rdsClient, "2.2", DB_INSTANCE_IDENTIFIER, DB_PARAMETER_GROUP_NAME);

        ArgumentCaptor<ModifyDBInstanceRequest> modifyRequestCaptor = ArgumentCaptor.forClass(ModifyDBInstanceRequest.class);
        verify(rdsClient).modifyDBInstance(modifyRequestCaptor.capture());
        ModifyDBInstanceRequest firedRequest = modifyRequestCaptor.getValue();
        assertEquals(DB_INSTANCE_IDENTIFIER, firedRequest.getDBInstanceIdentifier());
        assertEquals("2.2", firedRequest.getEngineVersion());
        assertTrue(firedRequest.getAllowMajorVersionUpgrade());
        assertTrue(firedRequest.getApplyImmediately());
    }

    @Test
    void testUpgradeRdsWhenExceptionThenThrows() {
        when(rdsClient.modifyDBInstance(any())).thenThrow(new RuntimeException("myException"));

        Assertions.assertThrows(CloudConnectorException.class, () ->
                underTest.upgradeRds(rdsClient, "2.2", DB_INSTANCE_IDENTIFIER, DB_PARAMETER_GROUP_NAME)
        );
    }

    @Test
    void testWaitForUpgrade() {
        AuthenticatedContext ac = setupAuthenticatedContext();
        Waiter<DescribeDBInstancesRequest> awsWaiter = mock(Waiter.class);
        when(customAmazonWaiterProvider.getDbInstanceModifyWaiter(rdsClient)).thenReturn(awsWaiter);

        underTest.waitForRdsUpgrade(ac, rdsClient, DB_INSTANCE_IDENTIFIER);

        verify(upgradeStartPoller).waitForUpgradeToStart(any());
        verify(awsWaiter).run(any());
    }

    @Test
    void testWaitForUpgradeWhenWaitForUpgradeTimesOut() {
        AuthenticatedContext ac = mock(AuthenticatedContext.class);
        doThrow(new PollerException("myException")).when(upgradeStartPoller).waitForUpgradeToStart(any());

        Assertions.assertThrows(CloudConnectorException.class, () ->
                underTest.waitForRdsUpgrade(ac, rdsClient, DB_INSTANCE_IDENTIFIER)
        );

        verify(upgradeStartPoller).waitForUpgradeToStart(any());
        verify(customAmazonWaiterProvider, never()).getDbInstanceModifyWaiter(any());
    }

    @Test
    void testCreatePatameterGroupWithCustomSettings() {
        DatabaseServer databaseServer = mock(DatabaseServer.class);
        when(databaseServer.getServerId()).thenReturn(DB_SERVER_ID);
        when(databaseServer.getEngine()).thenReturn(DatabaseEngine.POSTGRESQL);
        when(awsRdsVersionOperations.getDBParameterGroupFamily(DatabaseEngine.POSTGRESQL, "highestVersion")).thenReturn(PARAMETER_GROUP_FAMILY);
        String dbParameterGroupName = "dpg-" + DB_SERVER_ID + "highestVersion";

        underTest.createPatameterGroupWithCustomSettings(rdsClient, databaseServer, "highestVersion");

        verify(rdsClient).createParameterGroup(PARAMETER_GROUP_FAMILY, dbParameterGroupName);
        ArgumentCaptor<List<Parameter>> parameterArgumentCaptor = ArgumentCaptor.forClass(List.class);
        verify(rdsClient).changeParameterInGroup(eq(dbParameterGroupName), parameterArgumentCaptor.capture());
        List<Parameter> modifiedParameters = parameterArgumentCaptor.getValue();
        assertThat(modifiedParameters, hasSize(1));
        Parameter sslParameter = modifiedParameters.get(0);
        assertEquals("rds.force_ssl", sslParameter.getParameterName());
        assertEquals("1", sslParameter.getParameterValue());

    }

    private AuthenticatedContext setupAuthenticatedContext() {
        AuthenticatedContext ac = mock(AuthenticatedContext.class);
        CloudContext cc = mock(CloudContext.class);
        when(ac.getCloudContext()).thenReturn(cc);
        return ac;
    }

    @SafeVarargs
    private DescribeDBEngineVersionsResult setupDescribeDbEngineVersionResults(Tuple2<String, Set<String>>... currentAndTargetVersions) {
        DescribeDBEngineVersionsResult describeDBEngineVersionsResult = new DescribeDBEngineVersionsResult();
        Set<DBEngineVersion> dbEngineVersions = new HashSet<>();
        for (Tuple2<String, Set<String>> engineAndTargetVersion : currentAndTargetVersions) {
            DBEngineVersion dbEngineVersion = new DBEngineVersion();
            dbEngineVersion.setEngineVersion(engineAndTargetVersion.t1);
            Set<UpgradeTarget> targets = engineAndTargetVersion.t2.stream().map(tv -> new UpgradeTarget().withEngineVersion(tv)).collect(Collectors.toSet());
            dbEngineVersion.setValidUpgradeTarget(targets);
            dbEngineVersions.add(dbEngineVersion);
        }
        describeDBEngineVersionsResult.setDBEngineVersions(dbEngineVersions);
        return describeDBEngineVersionsResult;
    }

    private DescribeDBInstancesResult setupDescribeDbInstancesResult(String... engineVersions) {
        DescribeDBInstancesResult describeDBInstancesResult = new DescribeDBInstancesResult();
        Set<DBInstance> dbInstances = new HashSet<>();
        for (String engineVersion : engineVersions) {
            DBInstance dbInstance = new DBInstance();
            dbInstance.setEngineVersion(engineVersion);
            dbInstances.add(dbInstance);
        }
        describeDBInstancesResult.setDBInstances(dbInstances);
        return describeDBInstancesResult;
    }
}
