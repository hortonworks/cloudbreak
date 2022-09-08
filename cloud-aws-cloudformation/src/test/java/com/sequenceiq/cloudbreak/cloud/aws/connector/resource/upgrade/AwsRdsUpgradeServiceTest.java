package com.sequenceiq.cloudbreak.cloud.aws.connector.resource.upgrade;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.cloud.aws.AwsCloudFormationClient;
import com.sequenceiq.cloudbreak.cloud.aws.common.client.AmazonRdsClient;
import com.sequenceiq.cloudbreak.cloud.aws.connector.resource.upgrade.operation.AwsRdsUpgradeValidatorService;
import com.sequenceiq.cloudbreak.cloud.aws.connector.resource.upgrade.operation.RdsInfo;
import com.sequenceiq.cloudbreak.cloud.aws.connector.resource.upgrade.operation.RdsState;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.exception.CloudConnectorException;
import com.sequenceiq.cloudbreak.cloud.model.DatabaseServer;
import com.sequenceiq.cloudbreak.cloud.model.DatabaseStack;
import com.sequenceiq.cloudbreak.cloud.model.Location;
import com.sequenceiq.cloudbreak.cloud.model.Region;
import com.sequenceiq.cloudbreak.common.database.Version;

@ExtendWith(MockitoExtension.class)
public class AwsRdsUpgradeServiceTest {

    private static final String REGION_NAME = "MyRegion";

    private static final String DB_INSTANCE_IDENTIFIER = "dbInstanceIdentifier";

    private static final String TARGET_VERSION = "targetVersion";

    @Mock
    private AwsCloudFormationClient awsClient;

    @Mock
    private AwsRdsUpgradeSteps awsRdsUpgradeSteps;

    @Mock
    private AwsRdsUpgradeValidatorService awsRdsUpgradeValidatorService;

    @InjectMocks
    private AwsRdsUpgradeService underTest;

    @Mock
    private AmazonRdsClient rdsClient;

    @Mock
    private DatabaseStack databaseStack;

    @Mock
    private DatabaseServer databaseServer;

    @Mock
    private AuthenticatedContext ac;

    private final Version targetMajorVersion = () -> TARGET_VERSION;

    @BeforeEach
    void setupDbStack() {
        when(databaseStack.getDatabaseServer()).thenReturn(databaseServer);
        when(databaseServer.getServerId()).thenReturn(DB_INSTANCE_IDENTIFIER);
        when(awsClient.createRdsClient(any(), any())).thenReturn(rdsClient);
        setupAuthenticatedContext();
    }

    @Test
    void testUpgrade() {
        RdsInfo rdsInfo = new RdsInfo(RdsState.AVAILABLE, null, null);
        when(awsRdsUpgradeSteps.getRdsInfo(rdsClient, DB_INSTANCE_IDENTIFIER)).thenReturn(rdsInfo);
        when(awsRdsUpgradeValidatorService.isRdsMajorVersionSmallerThanTarget(rdsInfo, targetMajorVersion)).thenReturn(true);

        underTest.upgrade(ac, databaseStack, targetMajorVersion);

        InOrder inOrder = Mockito.inOrder(awsRdsUpgradeValidatorService, awsRdsUpgradeSteps);
        inOrder.verify(awsRdsUpgradeValidatorService).validateRdsIsAvailableOrUpgrading(rdsInfo);
        inOrder.verify(awsRdsUpgradeSteps).upgradeRds(rdsClient, databaseServer, rdsInfo, targetMajorVersion);
        inOrder.verify(awsRdsUpgradeSteps).waitForUpgrade(ac, rdsClient, databaseServer);
    }

    @Test
    void testUpgradeWhenRdsIsUpgradingThenUpgradeIsNotCalled() {
        RdsInfo rdsInfo = new RdsInfo(RdsState.UPGRADING, null, null);
        when(awsRdsUpgradeSteps.getRdsInfo(rdsClient, DB_INSTANCE_IDENTIFIER)).thenReturn(rdsInfo);
        when(awsRdsUpgradeValidatorService.isRdsMajorVersionSmallerThanTarget(rdsInfo, targetMajorVersion)).thenReturn(true);

        underTest.upgrade(ac, databaseStack, targetMajorVersion);

        InOrder inOrder = Mockito.inOrder(awsRdsUpgradeValidatorService, awsRdsUpgradeSteps);
        inOrder.verify(awsRdsUpgradeValidatorService).validateRdsIsAvailableOrUpgrading(rdsInfo);
        inOrder.verify(awsRdsUpgradeSteps).waitForUpgrade(ac, rdsClient, databaseServer);
        verify(awsRdsUpgradeSteps, never()).upgradeRds(rdsClient, databaseServer, rdsInfo, targetMajorVersion);
    }

    @Test
    void testUpgradeIfValidateThrowsThenNoUpgrade() {
        RdsInfo rdsInfo = new RdsInfo(RdsState.UNKNOWN, null, null);
        when(awsRdsUpgradeSteps.getRdsInfo(rdsClient, DB_INSTANCE_IDENTIFIER)).thenReturn(rdsInfo);
        when(awsRdsUpgradeValidatorService.isRdsMajorVersionSmallerThanTarget(rdsInfo, targetMajorVersion)).thenReturn(true);
        doThrow(CloudConnectorException.class).when(awsRdsUpgradeValidatorService).validateRdsIsAvailableOrUpgrading(rdsInfo);

        Assertions.assertThrows(CloudConnectorException.class, () ->
            underTest.upgrade(ac, databaseStack, targetMajorVersion)
        );

        verify(awsRdsUpgradeSteps, never()).upgradeRds(rdsClient, databaseServer, rdsInfo, targetMajorVersion);
        verify(awsRdsUpgradeSteps, never()).waitForUpgrade(ac, rdsClient, databaseServer);
    }

    @Test
    void testUpgradeWhenUpgradeThrowsThenDoNotWait() {
        RdsInfo rdsInfo = new RdsInfo(RdsState.AVAILABLE, null, null);
        when(awsRdsUpgradeSteps.getRdsInfo(rdsClient, DB_INSTANCE_IDENTIFIER)).thenReturn(rdsInfo);
        when(awsRdsUpgradeValidatorService.isRdsMajorVersionSmallerThanTarget(rdsInfo, targetMajorVersion)).thenReturn(true);
        doThrow(CloudConnectorException.class).when(awsRdsUpgradeSteps).upgradeRds(rdsClient, databaseServer, rdsInfo, targetMajorVersion);

        Assertions.assertThrows(CloudConnectorException.class, () ->
                underTest.upgrade(ac, databaseStack, targetMajorVersion)
        );

        InOrder inOrder = Mockito.inOrder(awsRdsUpgradeValidatorService, awsRdsUpgradeSteps);
        inOrder.verify(awsRdsUpgradeValidatorService).validateRdsIsAvailableOrUpgrading(rdsInfo);
        inOrder.verify(awsRdsUpgradeSteps).upgradeRds(rdsClient, databaseServer, rdsInfo, targetMajorVersion);
        verify(awsRdsUpgradeSteps, never()).waitForUpgrade(ac, rdsClient, databaseServer);
    }

    @Test
    void testUpgradeWhenRdsMajorVersionIsNotSmallerThanTargetThenUpgradeIsNotCalled() {
        RdsInfo rdsInfo = new RdsInfo(RdsState.AVAILABLE, null, null);
        when(awsRdsUpgradeSteps.getRdsInfo(rdsClient, DB_INSTANCE_IDENTIFIER)).thenReturn(rdsInfo);
        when(awsRdsUpgradeValidatorService.isRdsMajorVersionSmallerThanTarget(rdsInfo, targetMajorVersion)).thenReturn(false);

        underTest.upgrade(ac, databaseStack, targetMajorVersion);

        verify(awsRdsUpgradeSteps, never()).upgradeRds(rdsClient, databaseServer, rdsInfo, targetMajorVersion);
        verify(awsRdsUpgradeSteps, never()).waitForUpgrade(ac, rdsClient, databaseServer);
    }

    private void setupAuthenticatedContext() {
        CloudContext cc = mock(CloudContext.class);
        Location location = Location.location(Region.region(REGION_NAME));
        when(cc.getLocation()).thenReturn(location);
        when(ac.getCloudContext()).thenReturn(cc);
    }

}