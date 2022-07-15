package com.sequenceiq.cloudbreak.cloud.aws.connector.resource.upgrade;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Set;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.cloud.aws.AwsCloudFormationClient;
import com.sequenceiq.cloudbreak.cloud.aws.common.client.AmazonRdsClient;
import com.sequenceiq.cloudbreak.cloud.aws.connector.resource.upgrade.operation.AwsRdsUpgradeOperations;
import com.sequenceiq.cloudbreak.cloud.aws.connector.resource.upgrade.operation.AwsRdsVersionOperations;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.exception.CloudConnectorException;
import com.sequenceiq.cloudbreak.cloud.model.DatabaseServer;
import com.sequenceiq.cloudbreak.cloud.model.DatabaseStack;
import com.sequenceiq.cloudbreak.cloud.model.Location;
import com.sequenceiq.cloudbreak.cloud.model.Region;
import com.sequenceiq.cloudbreak.common.database.MajorVersion;

@ExtendWith(MockitoExtension.class)
public class AwsRdsUpgradeServiceTest {

    private static final String REGION_NAME = "MyRegion";

    private static final String DB_INSTANCE_IDENTIFIER = "dbInstanceIdentifier";

    private static final String CURRENT_DB_VERSION = "currentDbVersion";

    private static final String UPGRADE_TARGET_VERSION = "upgradeTargetVersion";

    private static final String UPGRADE_TARGET_MAJOR_VERSION = "upgradeTargetMajorVersion";

    @Mock
    private AwsCloudFormationClient awsClient;

    @Mock
    private AwsRdsUpgradeOperations awsRdsUpgradeOperations;

    @Mock
    private AwsRdsVersionOperations awsRdsVersionOperations;

    @InjectMocks
    private AwsRdsUpgradeService underTest;

    @Mock
    private AmazonRdsClient rdsClient;

    @Test
    void testUpgrade() {
        AuthenticatedContext ac = setupAuthenticatedContext();
        when(awsClient.createRdsClient(any(), eq(REGION_NAME))).thenReturn(rdsClient);
        DatabaseStack databaseStack = setupDbStack();
        when(awsRdsUpgradeOperations.getCurrentDbEngineVersion(rdsClient, DB_INSTANCE_IDENTIFIER)).thenReturn(CURRENT_DB_VERSION);
        Set<String> upgradeTargets = Set.of(UPGRADE_TARGET_VERSION);
        when(awsRdsUpgradeOperations.getUpgradeTargetVersions(rdsClient, CURRENT_DB_VERSION)).thenReturn(upgradeTargets);
        MajorVersion upgradeTargetMajorVersion = () -> UPGRADE_TARGET_MAJOR_VERSION;
        when(awsRdsVersionOperations.getHighestUpgradeVersion(upgradeTargets, upgradeTargetMajorVersion)).thenReturn(UPGRADE_TARGET_VERSION);

        underTest.upgrade(ac, databaseStack, upgradeTargetMajorVersion);

        verify(awsRdsUpgradeOperations).upgradeRds(rdsClient, UPGRADE_TARGET_VERSION, DB_INSTANCE_IDENTIFIER);
        verify(awsRdsUpgradeOperations).waitForRdsUpgrade(ac, rdsClient, DB_INSTANCE_IDENTIFIER);
    }

    @Test
    void testUpgradeWhenGetCurrentDbEngineVersionThrowsThenThrows() {
        AuthenticatedContext ac = setupAuthenticatedContext();
        when(awsClient.createRdsClient(any(), eq(REGION_NAME))).thenReturn(rdsClient);
        DatabaseStack databaseStack = setupDbStack();
        when(awsRdsUpgradeOperations.getCurrentDbEngineVersion(rdsClient, DB_INSTANCE_IDENTIFIER)).thenThrow(new CloudConnectorException("My exception"));
        MajorVersion upgradeTargetMajorVersion = () -> UPGRADE_TARGET_MAJOR_VERSION;

        Assertions.assertThrows(CloudConnectorException.class, () ->
                underTest.upgrade(ac, databaseStack, upgradeTargetMajorVersion)
        );

        verify(awsRdsUpgradeOperations, never()).getUpgradeTargetVersions(any(), any());
        verify(awsRdsVersionOperations, never()).getHighestUpgradeVersion(any(), any());
        verify(awsRdsUpgradeOperations, never()).upgradeRds(rdsClient, UPGRADE_TARGET_VERSION, DB_INSTANCE_IDENTIFIER);
        verify(awsRdsUpgradeOperations, never()).waitForRdsUpgrade(ac, rdsClient, DB_INSTANCE_IDENTIFIER);
    }

    @Test
    void testUpgradeWhenGetUpgradeTargetVersionThrowsThenThrows() {
        AuthenticatedContext ac = setupAuthenticatedContext();
        when(awsClient.createRdsClient(any(), eq(REGION_NAME))).thenReturn(rdsClient);
        DatabaseStack databaseStack = setupDbStack();
        when(awsRdsUpgradeOperations.getCurrentDbEngineVersion(rdsClient, DB_INSTANCE_IDENTIFIER)).thenReturn(CURRENT_DB_VERSION);
        when(awsRdsUpgradeOperations.getUpgradeTargetVersions(rdsClient, CURRENT_DB_VERSION)).thenThrow(new CloudConnectorException("My Exception"));
        MajorVersion upgradeTargetMajorVersion = () -> UPGRADE_TARGET_MAJOR_VERSION;

        Assertions.assertThrows(CloudConnectorException.class, () ->
                underTest.upgrade(ac, databaseStack, upgradeTargetMajorVersion)
        );

        verify(awsRdsVersionOperations, never()).getHighestUpgradeVersion(any(), any());
        verify(awsRdsUpgradeOperations, never()).upgradeRds(rdsClient, UPGRADE_TARGET_VERSION, DB_INSTANCE_IDENTIFIER);
        verify(awsRdsUpgradeOperations, never()).waitForRdsUpgrade(ac, rdsClient, DB_INSTANCE_IDENTIFIER);
    }

    @Test
    void testUpgradeWhenGetHighestUpgradeVersionThrowsThenThrows() {
        AuthenticatedContext ac = setupAuthenticatedContext();
        when(awsClient.createRdsClient(any(), eq(REGION_NAME))).thenReturn(rdsClient);
        DatabaseStack databaseStack = setupDbStack();
        when(awsRdsUpgradeOperations.getCurrentDbEngineVersion(rdsClient, DB_INSTANCE_IDENTIFIER)).thenReturn(CURRENT_DB_VERSION);
        Set<String> upgradeTargets = Set.of(UPGRADE_TARGET_VERSION);
        when(awsRdsUpgradeOperations.getUpgradeTargetVersions(rdsClient, CURRENT_DB_VERSION)).thenReturn(upgradeTargets);
        MajorVersion upgradeTargetMajorVersion = () -> UPGRADE_TARGET_MAJOR_VERSION;
        when(awsRdsVersionOperations.getHighestUpgradeVersion(upgradeTargets, upgradeTargetMajorVersion)).thenThrow(new CloudConnectorException("My Exception"));

        Assertions.assertThrows(CloudConnectorException.class, () ->
                underTest.upgrade(ac, databaseStack, upgradeTargetMajorVersion)
        );

        verify(awsRdsUpgradeOperations, never()).upgradeRds(rdsClient, UPGRADE_TARGET_VERSION, DB_INSTANCE_IDENTIFIER);
        verify(awsRdsUpgradeOperations, never()).waitForRdsUpgrade(ac, rdsClient, DB_INSTANCE_IDENTIFIER);
    }

    @Test
    void testUpgradeWhenUpgradeRdsThrowsThenThrows() {
        AuthenticatedContext ac = setupAuthenticatedContext();
        when(awsClient.createRdsClient(any(), eq(REGION_NAME))).thenReturn(rdsClient);
        DatabaseStack databaseStack = setupDbStack();
        when(awsRdsUpgradeOperations.getCurrentDbEngineVersion(rdsClient, DB_INSTANCE_IDENTIFIER)).thenReturn(CURRENT_DB_VERSION);
        Set<String> upgradeTargets = Set.of(UPGRADE_TARGET_VERSION);
        when(awsRdsUpgradeOperations.getUpgradeTargetVersions(rdsClient, CURRENT_DB_VERSION)).thenReturn(upgradeTargets);
        MajorVersion upgradeTargetMajorVersion = () -> UPGRADE_TARGET_MAJOR_VERSION;
        when(awsRdsVersionOperations.getHighestUpgradeVersion(upgradeTargets, upgradeTargetMajorVersion)).thenReturn(UPGRADE_TARGET_VERSION);
        doThrow(new CloudConnectorException("My exception")).when(awsRdsUpgradeOperations).upgradeRds(rdsClient, UPGRADE_TARGET_VERSION, DB_INSTANCE_IDENTIFIER);

        Assertions.assertThrows(CloudConnectorException.class, () ->
                underTest.upgrade(ac, databaseStack, upgradeTargetMajorVersion)
        );

        verify(awsRdsUpgradeOperations, never()).waitForRdsUpgrade(ac, rdsClient, DB_INSTANCE_IDENTIFIER);
    }

    @Test
    void testUpgradeWhenWaitForRdsUpgradeThrowsThenThrows() {
        AuthenticatedContext ac = setupAuthenticatedContext();
        when(awsClient.createRdsClient(any(), eq(REGION_NAME))).thenReturn(rdsClient);
        DatabaseStack databaseStack = setupDbStack();
        when(awsRdsUpgradeOperations.getCurrentDbEngineVersion(rdsClient, DB_INSTANCE_IDENTIFIER)).thenReturn(CURRENT_DB_VERSION);
        Set<String> upgradeTargets = Set.of(UPGRADE_TARGET_VERSION);
        when(awsRdsUpgradeOperations.getUpgradeTargetVersions(rdsClient, CURRENT_DB_VERSION)).thenReturn(upgradeTargets);
        MajorVersion upgradeTargetMajorVersion = () -> UPGRADE_TARGET_MAJOR_VERSION;
        when(awsRdsVersionOperations.getHighestUpgradeVersion(upgradeTargets, upgradeTargetMajorVersion)).thenReturn(UPGRADE_TARGET_VERSION);
        doThrow(new CloudConnectorException("myException")).when(awsRdsUpgradeOperations).waitForRdsUpgrade(ac, rdsClient, DB_INSTANCE_IDENTIFIER);

        Assertions.assertThrows(CloudConnectorException.class, () ->
                underTest.upgrade(ac, databaseStack, upgradeTargetMajorVersion)
        );
    }

    private DatabaseStack setupDbStack() {
        DatabaseStack dbStack = mock(DatabaseStack.class);
        DatabaseServer databaseServer = mock(DatabaseServer.class);
        when(databaseServer.getServerId()).thenReturn(DB_INSTANCE_IDENTIFIER);
        when(dbStack.getDatabaseServer()).thenReturn(databaseServer);
        return dbStack;
    }

    private AuthenticatedContext setupAuthenticatedContext() {
        AuthenticatedContext ac = mock(AuthenticatedContext.class);
        CloudContext cc = mock(CloudContext.class);
        Location location = Location.location(Region.region(REGION_NAME));
        when(cc.getLocation()).thenReturn(location);
        when(ac.getCloudContext()).thenReturn(cc);
        return ac;
    }

}