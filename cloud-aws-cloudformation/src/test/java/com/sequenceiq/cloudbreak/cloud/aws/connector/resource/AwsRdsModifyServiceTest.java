package com.sequenceiq.cloudbreak.cloud.aws.connector.resource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.cloud.aws.AwsCloudFormationClient;
import com.sequenceiq.cloudbreak.cloud.aws.common.client.AmazonRdsClient;
import com.sequenceiq.cloudbreak.cloud.aws.common.view.AwsCredentialView;
import com.sequenceiq.cloudbreak.cloud.aws.scheduler.CustomAmazonWaiterProvider;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.exception.CloudConnectorException;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.DatabaseServer;
import com.sequenceiq.cloudbreak.cloud.model.DatabaseStack;
import com.sequenceiq.cloudbreak.cloud.model.Location;
import com.sequenceiq.cloudbreak.cloud.model.Region;

import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.core.waiters.Waiter;
import software.amazon.awssdk.services.rds.model.DescribeDbInstancesResponse;
import software.amazon.awssdk.services.rds.model.ModifyDbInstanceRequest;
import software.amazon.awssdk.services.rds.model.ModifyDbInstanceResponse;

@ExtendWith(MockitoExtension.class)
public class AwsRdsModifyServiceTest {

    private static final String REGION = "region";

    private static final String DB_INSTANCE_IDENTIFIER = "dbInstance";

    private static final String NEW_PASSWORD = "newPassword";

    @Mock
    private AwsCloudFormationClient awsClient;

    @Mock
    private AuthenticatedContext authenticatedContext;

    @Mock
    private CloudCredential cloudCredential;

    @Mock
    private CloudContext cloudContext;

    @Mock
    private Location location;

    @Mock
    private Region region;

    @Mock
    private AmazonRdsClient amazonRDS;

    @Mock
    private DatabaseStack dbStack;

    @Mock
    private DatabaseServer databaseServer;

    @Mock
    private Waiter<DescribeDbInstancesResponse> rdsWaiter;

    @Mock
    private CustomAmazonWaiterProvider provider;

    @InjectMocks
    private AwsRdsModifyService victim;

    @BeforeEach
    void initTests() {
        when(authenticatedContext.getCloudCredential()).thenReturn(cloudCredential);
        when(authenticatedContext.getCloudContext()).thenReturn(cloudContext);
        when(cloudContext.getLocation()).thenReturn(location);
        when(location.getRegion()).thenReturn(region);
        when(region.value()).thenReturn(REGION);
        when(awsClient.createRdsClient(any(AwsCredentialView.class), eq(REGION))).thenReturn(amazonRDS);
        when(dbStack.getDatabaseServer()).thenReturn(databaseServer);
        when(databaseServer.getServerId()).thenReturn(DB_INSTANCE_IDENTIFIER);
    }

    @Test
    void disableDeleteProtectionShouldStopAndScheduleTask() {
        ArgumentCaptor<ModifyDbInstanceRequest> modifyDBInstanceRequestArgumentCaptor = ArgumentCaptor.forClass(ModifyDbInstanceRequest.class);
        when(amazonRDS.modifyDBInstance(modifyDBInstanceRequestArgumentCaptor.capture())).thenReturn(null);
        when(provider.getDbInstanceModifyWaiter()).thenReturn(rdsWaiter);

        victim.disableDeleteProtection(authenticatedContext, dbStack);

        assertEquals(DB_INSTANCE_IDENTIFIER, modifyDBInstanceRequestArgumentCaptor.getValue().dbInstanceIdentifier());
        verify(rdsWaiter, times(1)).run(any());
    }

    @Test
    void disableDeleteProtectionShouldThrowCloudConnectorExceptionInCaseOfRdsClientException() {
        ArgumentCaptor<ModifyDbInstanceRequest> modifyDBInstanceRequestArgumentCaptor = ArgumentCaptor.forClass(ModifyDbInstanceRequest.class);

        when(amazonRDS.modifyDBInstance(modifyDBInstanceRequestArgumentCaptor.capture())).thenThrow(new RuntimeException());

        assertThrows(CloudConnectorException.class, () -> victim.disableDeleteProtection(authenticatedContext, dbStack));
    }

    @Test
    void disableDeleteProtectionShouldThrowCloudConnectorExceptionInCaseOfSchedulerException() {
        ArgumentCaptor<ModifyDbInstanceRequest> modifyDBInstanceRequestArgumentCaptor = ArgumentCaptor.forClass(ModifyDbInstanceRequest.class);
        when(amazonRDS.modifyDBInstance(modifyDBInstanceRequestArgumentCaptor.capture())).thenReturn(null);
        when(provider.getDbInstanceModifyWaiter()).thenReturn(rdsWaiter);
        doThrow(SdkException.class).when(rdsWaiter).run(any());

        assertThrows(CloudConnectorException.class, () -> victim.disableDeleteProtection(authenticatedContext, dbStack));
    }

    @Test
    void updateMasterUserPasswordShouldThrowCloudConnectorExceptionInCaseOfRdsClientException() {
        ArgumentCaptor<ModifyDbInstanceRequest> modifyDBInstanceRequestArgumentCaptor = ArgumentCaptor.forClass(ModifyDbInstanceRequest.class);
        when(amazonRDS.modifyDBInstance(modifyDBInstanceRequestArgumentCaptor.capture())).thenThrow(new RuntimeException());

        assertThrows(CloudConnectorException.class, () -> victim.updateMasterUserPassword(authenticatedContext, dbStack, NEW_PASSWORD));
        verify(rdsWaiter, never()).run(any());
    }

    @Test
    void updateMasterUserPasswordShouldSucceed() {
        ArgumentCaptor<ModifyDbInstanceRequest> modifyDBInstanceRequestArgumentCaptor = ArgumentCaptor.forClass(ModifyDbInstanceRequest.class);
        when(amazonRDS.modifyDBInstance(modifyDBInstanceRequestArgumentCaptor.capture())).thenReturn(ModifyDbInstanceResponse.builder().build());
        when(provider.getDbInstanceModifyWaiter()).thenReturn(rdsWaiter);

        victim.updateMasterUserPassword(authenticatedContext, dbStack, NEW_PASSWORD);

        assertEquals(DB_INSTANCE_IDENTIFIER, modifyDBInstanceRequestArgumentCaptor.getValue().dbInstanceIdentifier());
        verify(rdsWaiter, times(1)).run(any());
    }
}
