package com.sequenceiq.cloudbreak.cloud.aws.connector.resource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import com.sequenceiq.cloudbreak.cloud.aws.AwsCloudFormationClient;
import com.sequenceiq.cloudbreak.cloud.aws.common.client.AmazonRdsClient;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.exception.CloudConnectorException;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.DatabaseServer;
import com.sequenceiq.cloudbreak.cloud.model.DatabaseStack;
import com.sequenceiq.cloudbreak.cloud.model.ExternalDatabaseStatus;
import com.sequenceiq.cloudbreak.cloud.model.Location;
import com.sequenceiq.cloudbreak.cloud.model.Region;
import com.sequenceiq.cloudbreak.cloud.model.database.CloudDatabaseServerSslCertificate;
import com.sequenceiq.cloudbreak.cloud.model.database.CloudDatabaseServerSslCertificateType;

import software.amazon.awssdk.services.rds.model.DBInstance;
import software.amazon.awssdk.services.rds.model.DbInstanceNotFoundException;
import software.amazon.awssdk.services.rds.model.DescribeDbInstancesRequest;
import software.amazon.awssdk.services.rds.model.DescribeDbInstancesResponse;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class AwsRdsStatusLookupServiceTest {

    private static final String REGION = "region";

    private static final String DB_INSTANCE_IDENTIFIER = "dbInstance";

    private static final String DB_INSTANCE_STATUS_STARTED = "available";

    private static final String DB_INSTANCE_STATUS_STOPPED = "stopped";

    private static final String DB_INSTANCE_STATUS_ANY = "any";

    private static final String CA_CERTIFICATE_IDENTIFIER = "mycert";

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

    @InjectMocks
    private AwsRdsStatusLookupService victim;

    @BeforeEach
    public void initTests() {
        when(awsClient.createRdsClient(any())).thenReturn(amazonRDS);
        when(dbStack.getDatabaseServer()).thenReturn(databaseServer);
        when(databaseServer.getServerId()).thenReturn(DB_INSTANCE_IDENTIFIER);
    }

    private DescribeDbInstancesResponse createResponse(boolean deletionProtectionEnabled) {
        return DescribeDbInstancesResponse.builder().dbInstances(DBInstance.builder().deletionProtection(deletionProtectionEnabled).build()).build();
    }

    private DescribeDbInstancesResponse createResponse(String dbInstanceStatus) {
        return DescribeDbInstancesResponse.builder().dbInstances(DBInstance.builder().dbInstanceStatus(dbInstanceStatus).build()).build();
    }

    @Test
    public void shouldLookupStartedExternalDatabaseStatus() {
        when(amazonRDS.describeDBInstances(any(DescribeDbInstancesRequest.class))).thenReturn(createResponse(DB_INSTANCE_STATUS_STARTED));

        ExternalDatabaseStatus result = victim.getStatus(authenticatedContext, dbStack);

        assertThat(result).isEqualTo(ExternalDatabaseStatus.STARTED);
    }

    @Test
    public void shouldLookupStoppedExternalDatabaseStatus() {
        when(amazonRDS.describeDBInstances(any(DescribeDbInstancesRequest.class))).thenReturn(createResponse(DB_INSTANCE_STATUS_STOPPED));

        ExternalDatabaseStatus result = victim.getStatus(authenticatedContext, dbStack);

        assertThat(result).isEqualTo(ExternalDatabaseStatus.STOPPED);
    }

    @Test
    public void shouldLookupUpdateInProgressExternalDatabaseStatus() {
        when(amazonRDS.describeDBInstances(any(DescribeDbInstancesRequest.class))).thenReturn(createResponse(DB_INSTANCE_STATUS_ANY));

        ExternalDatabaseStatus result = victim.getStatus(authenticatedContext, dbStack);

        assertThat(result).isEqualTo(ExternalDatabaseStatus.UPDATE_IN_PROGRESS);
    }

    @Test
    public void shouldReturnDeletedInCaseOfDBInstanceNotFoundException() {
        when(amazonRDS.describeDBInstances(any(DescribeDbInstancesRequest.class))).thenThrow(DbInstanceNotFoundException.class);

        ExternalDatabaseStatus result = victim.getStatus(authenticatedContext, dbStack);

        assertThat(result).isEqualTo(ExternalDatabaseStatus.DELETED);
    }

    @Test()
    public void shouldThrowCloudConnectorExceptionInCaseOfAnyRuntimeException() {
        when(amazonRDS.describeDBInstances(any(DescribeDbInstancesRequest.class))).thenThrow(RuntimeException.class);

        assertThrows(CloudConnectorException.class, () -> victim.getStatus(authenticatedContext, dbStack));
    }

    @Test
    public void isDeleteProtectionEnabledTest() {
        assertTrue(victim.isDeleteProtectionEnabled(createResponse(true)));
        assertFalse(victim.isDeleteProtectionEnabled(createResponse(false)));
    }

    @Test
    public void isDeleteProtectionEnabledShouldReturnFalseInCaseOfDBInstanceNotFound() {
        boolean result = victim.isDeleteProtectionEnabled(null);

        //if AWS returns that DBInstance does not exist, then we shall consider this as the termination protection is not enabled
        assertThat(result).isFalse();
    }

    @Test
    void isDbStackExistOnProviderSideTestWhenNull() {
        assertThat(victim.isDbStackExistOnProviderSide(null)).isFalse();
    }

    @Test
    void isDbStackExistOnProviderSideTestWhenNotNull() {
        assertTrue(victim.isDbStackExistOnProviderSide(DescribeDbInstancesResponse.builder().build()));
    }

    @Test
    void getDescribeDBInstancesResultForDeleteProtectionTestWhenSuccess() {
        DescribeDbInstancesResponse dbInstancesResponse = DescribeDbInstancesResponse.builder().build();
        when(amazonRDS.describeDBInstances(any(DescribeDbInstancesRequest.class))).thenReturn(dbInstancesResponse);

        DescribeDbInstancesResponse result = victim.getDescribeDBInstancesResponseForDeleteProtection(authenticatedContext, dbStack);

        assertEquals(result, dbInstancesResponse);
    }

    @Test
    void getDescribeDBInstancesResultForDeleteProtectionTestWhenDBInstanceNotFoundException() {
        when(amazonRDS.describeDBInstances(any(DescribeDbInstancesRequest.class))).thenThrow(DbInstanceNotFoundException.class);

        DescribeDbInstancesResponse result = victim.getDescribeDBInstancesResponseForDeleteProtection(authenticatedContext, dbStack);

        assertNull(result);
    }

    @Test
    void getDescribeDBInstancesResultForDeleteProtectionTestWhenRuntimeExceptionThenThrowCloudConnectorException() {
        when(amazonRDS.describeDBInstances(any(DescribeDbInstancesRequest.class))).thenThrow(RuntimeException.class);

        assertThrows(CloudConnectorException.class, () -> victim.getDescribeDBInstancesResponseForDeleteProtection(authenticatedContext, dbStack));
    }

    @Test
    void getActiveSslRootCertificateTestWhenSuccess() {
        DescribeDbInstancesResponse response = DescribeDbInstancesResponse.builder()
                .dbInstances(DBInstance.builder().caCertificateIdentifier(CA_CERTIFICATE_IDENTIFIER).build()).build();
        when(amazonRDS.describeDBInstances(any(DescribeDbInstancesRequest.class))).thenReturn(response);

        CloudDatabaseServerSslCertificate result = victim.getActiveSslRootCertificate(authenticatedContext, dbStack);

        assertThat(result).isNotNull();
        assertThat(result.certificateType()).isEqualTo(CloudDatabaseServerSslCertificateType.ROOT);
        assertThat(result.certificateIdentifier()).isEqualTo(CA_CERTIFICATE_IDENTIFIER);
    }

    @Test
    void getActiveSslRootCertificateTestWhenDBInstanceNotFoundException() {
        when(amazonRDS.describeDBInstances(any(DescribeDbInstancesRequest.class))).thenThrow(DbInstanceNotFoundException.class);

        CloudDatabaseServerSslCertificate result = victim.getActiveSslRootCertificate(authenticatedContext, dbStack);

        assertThat(result).isNull();
    }

    @Test
    void getActiveSslRootCertificateTestTestWhenRuntimeExceptionThenThrowCloudConnectorException() {
        when(amazonRDS.describeDBInstances(any(DescribeDbInstancesRequest.class))).thenThrow(RuntimeException.class);

        assertThrows(CloudConnectorException.class, () -> victim.getActiveSslRootCertificate(authenticatedContext, dbStack));
    }

}
