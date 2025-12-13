package com.sequenceiq.cloudbreak.cloud.aws.connector.resource.upgrade;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.cloud.aws.AwsCloudFormationClient;
import com.sequenceiq.cloudbreak.cloud.aws.common.client.AmazonRdsClient;
import com.sequenceiq.cloudbreak.cloud.aws.connector.resource.AwsRdsParameterGroupService;
import com.sequenceiq.cloudbreak.cloud.aws.connector.resource.upgrade.operation.AwsRdsUpgradeValidatorProvider;
import com.sequenceiq.cloudbreak.cloud.aws.connector.resource.upgrade.operation.RdsInfo;
import com.sequenceiq.cloudbreak.cloud.aws.connector.resource.upgrade.operation.RdsState;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.exception.CloudConnectorException;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.DatabaseServer;
import com.sequenceiq.cloudbreak.cloud.model.DatabaseStack;
import com.sequenceiq.cloudbreak.cloud.model.Location;
import com.sequenceiq.cloudbreak.cloud.model.Region;
import com.sequenceiq.cloudbreak.cloud.notification.PersistenceNotifier;
import com.sequenceiq.cloudbreak.common.database.Version;
import com.sequenceiq.common.api.type.ResourceType;

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
    private AwsRdsUpgradeValidatorProvider awsRdsUpgradeValidatorProvider;

    @InjectMocks
    private AwsRdsUpgradeService underTest;

    @Mock
    private AmazonRdsClient rdsClient;

    @Mock
    private DatabaseStack databaseStack;

    @Mock
    private DatabaseServer databaseServer;

    @Mock
    private CloudContext cloudContext;

    @Mock
    private AuthenticatedContext ac;

    @Mock
    private AwsRdsParameterGroupService awsRdsParameterGroupService;

    @Mock
    private PersistenceNotifier persistenceNotifier;

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
        List<CloudResource> resources = List.of(CloudResource.builder().withType(ResourceType.RDS_DB_PARAMETER_GROUP).withName("resource1").build(),
                CloudResource.builder().withType(ResourceType.RDS_DB_PARAMETER_GROUP).withName("resource2").build());
        when(awsRdsUpgradeSteps.getRdsInfo(rdsClient, DB_INSTANCE_IDENTIFIER)).thenReturn(rdsInfo);
        when(awsRdsUpgradeValidatorProvider.isRdsMajorVersionSmallerThanTarget(rdsInfo, targetMajorVersion)).thenReturn(true);
        when(awsRdsParameterGroupService.removeFormerParamGroups(any(AmazonRdsClient.class), eq(databaseServer), eq(List.of())))
                .thenReturn(resources);
        underTest.upgrade(ac, databaseStack, targetMajorVersion, persistenceNotifier, List.of());

        InOrder inOrder = inOrder(awsRdsUpgradeValidatorProvider, awsRdsUpgradeSteps);
        inOrder.verify(awsRdsUpgradeValidatorProvider).validateRdsIsAvailableOrUpgrading(rdsInfo);
        inOrder.verify(awsRdsUpgradeSteps).upgradeRds(ac, rdsClient, databaseServer, rdsInfo, targetMajorVersion);
        inOrder.verify(awsRdsUpgradeSteps).waitForUpgrade(rdsClient, databaseServer);
        verify(persistenceNotifier, times(1)).notifyDeletions(resources, cloudContext);
    }

    @Test
    void testUpgradeWhenRdsIsUpgradingThenUpgradeIsNotCalled() {
        RdsInfo rdsInfo = new RdsInfo(RdsState.UPGRADING, null, null);
        when(awsRdsUpgradeSteps.getRdsInfo(rdsClient, DB_INSTANCE_IDENTIFIER)).thenReturn(rdsInfo);
        when(awsRdsUpgradeValidatorProvider.isRdsMajorVersionSmallerThanTarget(rdsInfo, targetMajorVersion)).thenReturn(true);

        underTest.upgrade(ac, databaseStack, targetMajorVersion, persistenceNotifier, List.of());

        InOrder inOrder = inOrder(awsRdsUpgradeValidatorProvider, awsRdsUpgradeSteps);
        inOrder.verify(awsRdsUpgradeValidatorProvider).validateRdsIsAvailableOrUpgrading(rdsInfo);
        inOrder.verify(awsRdsUpgradeSteps).waitForUpgrade(rdsClient, databaseServer);
        verify(awsRdsUpgradeSteps, never()).upgradeRds(ac, rdsClient, databaseServer, rdsInfo, targetMajorVersion);
    }

    @Test
    void testUpgradeIfValidateThrowsThenNoUpgrade() {
        RdsInfo rdsInfo = new RdsInfo(RdsState.UNKNOWN, null, null);
        when(awsRdsUpgradeSteps.getRdsInfo(rdsClient, DB_INSTANCE_IDENTIFIER)).thenReturn(rdsInfo);
        when(awsRdsUpgradeValidatorProvider.isRdsMajorVersionSmallerThanTarget(rdsInfo, targetMajorVersion)).thenReturn(true);
        doThrow(CloudConnectorException.class).when(awsRdsUpgradeValidatorProvider).validateRdsIsAvailableOrUpgrading(rdsInfo);

        assertThrows(CloudConnectorException.class, () ->
            underTest.upgrade(ac, databaseStack, targetMajorVersion, persistenceNotifier, List.of())
        );

        verify(awsRdsUpgradeSteps, never()).upgradeRds(ac, rdsClient, databaseServer, rdsInfo, targetMajorVersion);
        verify(awsRdsUpgradeSteps, never()).waitForUpgrade(rdsClient, databaseServer);
    }

    @Test
    void testUpgradeWhenUpgradeThrowsThenDoNotWait() {
        RdsInfo rdsInfo = new RdsInfo(RdsState.AVAILABLE, null, null);
        when(awsRdsUpgradeSteps.getRdsInfo(rdsClient, DB_INSTANCE_IDENTIFIER)).thenReturn(rdsInfo);
        when(awsRdsUpgradeValidatorProvider.isRdsMajorVersionSmallerThanTarget(rdsInfo, targetMajorVersion)).thenReturn(true);
        doThrow(CloudConnectorException.class).when(awsRdsUpgradeSteps).upgradeRds(ac, rdsClient, databaseServer, rdsInfo, targetMajorVersion);

        assertThrows(CloudConnectorException.class, () ->
                underTest.upgrade(ac, databaseStack, targetMajorVersion, persistenceNotifier, List.of())
        );

        InOrder inOrder = inOrder(awsRdsUpgradeValidatorProvider, awsRdsUpgradeSteps);
        inOrder.verify(awsRdsUpgradeValidatorProvider).validateRdsIsAvailableOrUpgrading(rdsInfo);
        inOrder.verify(awsRdsUpgradeSteps).upgradeRds(ac, rdsClient, databaseServer, rdsInfo, targetMajorVersion);
        verify(awsRdsUpgradeSteps, never()).waitForUpgrade(rdsClient, databaseServer);
    }

    @Test
    void testUpgradeWhenRdsMajorVersionIsNotSmallerThanTargetThenUpgradeIsNotCalled() {
        RdsInfo rdsInfo = new RdsInfo(RdsState.AVAILABLE, null, null);
        when(awsRdsUpgradeSteps.getRdsInfo(rdsClient, DB_INSTANCE_IDENTIFIER)).thenReturn(rdsInfo);
        when(awsRdsUpgradeValidatorProvider.isRdsMajorVersionSmallerThanTarget(rdsInfo, targetMajorVersion)).thenReturn(false);

        underTest.upgrade(ac, databaseStack, targetMajorVersion, persistenceNotifier, List.of());

        verify(awsRdsUpgradeSteps, never()).upgradeRds(ac, rdsClient, databaseServer, rdsInfo, targetMajorVersion);
        verify(awsRdsUpgradeSteps, never()).waitForUpgrade(rdsClient, databaseServer);
    }

    private void setupAuthenticatedContext() {
        Location location = Location.location(Region.region(REGION_NAME));
        when(cloudContext.getLocation()).thenReturn(location);
        when(ac.getCloudContext()).thenReturn(cloudContext);
    }

}