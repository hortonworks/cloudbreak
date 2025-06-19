package com.sequenceiq.cloudbreak.cloud.aws.connector.resource.upgrade;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.cloud.aws.AwsCloudFormationClient;
import com.sequenceiq.cloudbreak.cloud.aws.common.client.AmazonRdsClient;
import com.sequenceiq.cloudbreak.cloud.aws.connector.resource.upgrade.operation.AwsRdsUpgradeValidatorProvider;
import com.sequenceiq.cloudbreak.cloud.aws.connector.resource.upgrade.operation.RdsEngineVersion;
import com.sequenceiq.cloudbreak.cloud.aws.connector.resource.upgrade.operation.RdsInfo;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.model.DatabaseServer;
import com.sequenceiq.cloudbreak.cloud.model.DatabaseStack;
import com.sequenceiq.cloudbreak.common.database.TargetMajorVersion;

@ExtendWith(MockitoExtension.class)
public class AwsRdsUpgradeValidatorServiceTest {

    @Mock
    private AwsRdsUpgradeValidatorProvider awsRdsUpgradeValidatorProvider;

    @Mock
    private AwsRdsUpgradeSteps awsRdsUpgradeSteps;

    @Mock
    private AwsCloudFormationClient awsClient;

    @Mock
    private AmazonRdsClient amazonRdsClient;

    @Mock
    private AuthenticatedContext authenticatedContext;

    @Mock
    private DatabaseStack databaseStack;

    @Mock
    private TargetMajorVersion targetMajorVersion;

    @Mock
    private DatabaseServer databaseServer;

    @Mock
    private RdsInfo rdsInfo;

    @Mock
    private RdsEngineVersion rdsEngineVersion;

    @InjectMocks
    private AwsRdsUpgradeValidatorService underTest;

    @BeforeEach
    void setUp() {
    }

    @Test
    void testValidateUpgradeDatabaseServerThrowsException() {
        doThrow(new RuntimeException("Validation failed"))
                .when(awsRdsUpgradeValidatorProvider).validateCustomPropertiesAdded(authenticatedContext, databaseStack);

        RuntimeException exception = assertThrows(RuntimeException.class, () ->
                underTest.validateUpgradeDatabaseServer(authenticatedContext, databaseStack, targetMajorVersion));

        assertEquals("Validation failed", exception.getMessage());
        verify(awsRdsUpgradeValidatorProvider).validateCustomPropertiesAdded(authenticatedContext, databaseStack);
    }

    @Test
    void testValidateVersionUpgradeSupportedNoUpgradeTargetVersion() {
        when(databaseStack.getDatabaseServer()).thenReturn(databaseServer);
        when(databaseStack.getDatabaseServer().getServerId()).thenReturn("db-instance-id");
        when(awsClient.createRdsClient(any())).thenReturn(amazonRdsClient);

        when(awsRdsUpgradeSteps.getRdsInfo(any(), any())).thenReturn(rdsInfo);
        when(rdsInfo.getRdsEngineVersion()).thenReturn(rdsEngineVersion);

        when(awsRdsUpgradeSteps.getRdsInfo(amazonRdsClient, "db-instance-id")).thenReturn(rdsInfo);
        when(awsRdsUpgradeValidatorProvider.getHighestUpgradeTargetVersion(amazonRdsClient, targetMajorVersion, rdsEngineVersion))
                .thenReturn(null);

        underTest.validateUpgradeDatabaseServer(authenticatedContext, databaseStack, targetMajorVersion);

        verify(awsRdsUpgradeValidatorProvider).getHighestUpgradeTargetVersion(amazonRdsClient, targetMajorVersion, rdsEngineVersion);
    }

}