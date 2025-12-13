package com.sequenceiq.cloudbreak.cloud.aws.connector.resource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.cloud.aws.AwsCloudFormationClient;
import com.sequenceiq.cloudbreak.cloud.aws.common.client.AmazonRdsClient;
import com.sequenceiq.cloudbreak.cloud.aws.scheduler.CustomAmazonWaiterProvider;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.exception.CloudConnectorException;
import com.sequenceiq.cloudbreak.cloud.model.DatabaseServer;
import com.sequenceiq.cloudbreak.cloud.model.DatabaseStack;

import software.amazon.awssdk.awscore.exception.AwsErrorDetails;
import software.amazon.awssdk.core.waiters.Waiter;
import software.amazon.awssdk.core.waiters.WaiterResponse;
import software.amazon.awssdk.services.rds.model.DBInstance;
import software.amazon.awssdk.services.rds.model.DescribeDbEngineVersionsResponse;
import software.amazon.awssdk.services.rds.model.DescribeDbInstancesRequest;
import software.amazon.awssdk.services.rds.model.DescribeDbInstancesResponse;
import software.amazon.awssdk.services.rds.model.ModifyDbInstanceRequest;
import software.amazon.awssdk.services.rds.model.RdsException;
import software.amazon.awssdk.services.rds.model.RebootDbInstanceRequest;
import software.amazon.awssdk.services.rds.model.RebootDbInstanceResponse;
import software.amazon.awssdk.services.rds.waiters.RdsWaiter;

@ExtendWith(MockitoExtension.class)
class AwsDatabaseSslCertRotationServiceTest {

    @Mock
    private AwsCloudFormationClient awsClient;

    @Mock
    private AmazonRdsClient rdsClient;

    @Mock
    private RdsWaiter rdsWaiter;

    @Mock
    private WaiterResponse waiterResponse;

    @Mock
    private DescribeDbEngineVersionsResponse describeDbEngineVersionsResponse;

    @Mock
    private CustomAmazonWaiterProvider customAmazonWaiterProvider;

    @InjectMocks
    private AwsDatabaseSslCertRotationService rotationService;

    @Test
    public void testRunWhenEverythingGoesWell() throws Exception {
        AuthenticatedContext ac = mock(AuthenticatedContext.class);
        DatabaseStack dbStack = mock(DatabaseStack.class);
        String desiredCertificate = "desiredCertificate";
        String dbInstanceIdentifier = "desiredCertificate";
        String dbInstanceCertificate = "cert";
        DescribeDbInstancesResponse describeDbInstancesResponse = DescribeDbInstancesResponse.builder()
                .dbInstances(DBInstance.builder()
                        .caCertificateIdentifier(dbInstanceCertificate)
                        .engineVersion("14")
                        .engine("psql")
                        .build())
                .build();
        Waiter<DescribeDbInstancesResponse> waiter = mock(Waiter.class);

        when(ac.getCloudContext()).thenReturn(mock(CloudContext.class));
        when(ac.getCloudContext().getId()).thenReturn(1L);
        when(dbStack.getDatabaseServer()).thenReturn(mock(DatabaseServer.class));
        when(dbStack.getDatabaseServer().getServerId()).thenReturn(dbInstanceIdentifier);
        when(awsClient.createRdsClient(any())).thenReturn(rdsClient);
        when(rdsClient.describeDBInstances(any())).thenReturn(describeDbInstancesResponse);
        when(customAmazonWaiterProvider.getCertRotationStartWaiter()).thenReturn(waiter);
        when(customAmazonWaiterProvider.getDbInstanceModifyWaiter()).thenReturn(waiter);
        when(rdsClient.describeDBInstances(any(DescribeDbInstancesRequest.class))).thenReturn(describeDbInstancesResponse);
        when(rdsWaiter.waitUntilDBInstanceAvailable(any(DescribeDbInstancesRequest.class), any())).thenReturn(waiterResponse);
        when(rdsClient.waiters()).thenReturn(rdsWaiter);
        when(describeDbEngineVersionsResponse.hasDbEngineVersions()).thenReturn(false);
        when(rdsClient.rebootDBInstance(any())).thenReturn(RebootDbInstanceResponse.builder().build());
        when(rdsClient.describeDBEngineVersions(any())).thenReturn(describeDbEngineVersionsResponse);


        rotationService.applyCertificateChange(ac, dbStack, desiredCertificate);

        verify(awsClient).createRdsClient(any());
        verify(rdsClient).modifyDBInstance(any(ModifyDbInstanceRequest.class));
        verify(rdsClient).rebootDBInstance(any(RebootDbInstanceRequest.class));
        verify(rdsClient.waiters()).waitUntilDBInstanceAvailable(any(DescribeDbInstancesRequest.class), any());
    }

    @Test
    public void testDecribeRdsThrowRdsException() {
        AuthenticatedContext ac = mock(AuthenticatedContext.class);
        DatabaseStack dbStack = mock(DatabaseStack.class);
        String desiredCertificate = "desiredCertificate";
        String dbInstanceIdentifier = "desiredCertificate";

        when(dbStack.getDatabaseServer()).thenReturn(mock(DatabaseServer.class));
        when(dbStack.getDatabaseServer().getServerId()).thenReturn(dbInstanceIdentifier);
        when(awsClient.createRdsClient(any())).thenReturn(rdsClient);
        when(rdsClient.describeDBInstances(any(DescribeDbInstancesRequest.class))).thenThrow(
                RdsException.create("error", new CloudConnectorException("error")));

        CloudConnectorException exception = assertThrows(CloudConnectorException.class,
                () -> rotationService.applyCertificateChange(ac, dbStack, desiredCertificate));

        assertEquals(exception.getMessage(), "error");
    }

    @Test
    public void testDecribeRdsThrowAccessDeniedException() {
        AuthenticatedContext ac = mock(AuthenticatedContext.class);
        DatabaseStack dbStack = mock(DatabaseStack.class);
        String desiredCertificate = "desiredCertificate";
        String dbInstanceIdentifier = "desiredCertificate";
        RdsException rdsException = mock(RdsException.class);

        when(ac.getCloudContext()).thenReturn(mock(CloudContext.class));
        when(rdsException.awsErrorDetails()).thenReturn(AwsErrorDetails.builder().errorCode("AccessDenied").build());
        when(dbStack.getDatabaseServer()).thenReturn(mock(DatabaseServer.class));
        when(dbStack.getDatabaseServer().getServerId()).thenReturn(dbInstanceIdentifier);
        when(awsClient.createRdsClient(any())).thenReturn(rdsClient);
        when(rdsClient.describeDBInstances(any(DescribeDbInstancesRequest.class))).thenThrow(rdsException);
        when(rdsClient.waiters()).thenReturn(rdsWaiter);

        rotationService.applyCertificateChange(ac, dbStack, desiredCertificate);

        verify(rdsClient.waiters()).waitUntilDBInstanceAvailable(any(DescribeDbInstancesRequest.class), any());
    }

    @Test
    public void testWaiterWhenThrowException() {
        AuthenticatedContext ac = mock(AuthenticatedContext.class);
        DatabaseStack dbStack = mock(DatabaseStack.class);
        String desiredCertificate = "desiredCertificate";
        String dbInstanceIdentifier = "desiredCertificate";
        RdsException rdsException = mock(RdsException.class);

        when(ac.getCloudContext()).thenReturn(mock(CloudContext.class));
        when(rdsException.awsErrorDetails()).thenReturn(AwsErrorDetails.builder().errorCode("AccessDenied").build());
        when(dbStack.getDatabaseServer()).thenReturn(mock(DatabaseServer.class));
        when(dbStack.getDatabaseServer().getServerId()).thenReturn(dbInstanceIdentifier);
        when(awsClient.createRdsClient(any())).thenReturn(rdsClient);
        when(rdsClient.describeDBInstances(any(DescribeDbInstancesRequest.class))).thenThrow(rdsException);
        when(rdsClient.waiters()).thenThrow(new CloudConnectorException("error"));

        CloudConnectorException exception = assertThrows(CloudConnectorException.class,
                () -> rotationService.applyCertificateChange(ac, dbStack, desiredCertificate));

        assertEquals(exception.getMessage(), "error");
    }
}