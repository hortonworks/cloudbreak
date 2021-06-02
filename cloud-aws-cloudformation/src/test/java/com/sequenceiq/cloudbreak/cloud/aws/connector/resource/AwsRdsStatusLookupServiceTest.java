package com.sequenceiq.cloudbreak.cloud.aws.connector.resource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import com.amazonaws.services.rds.model.DBInstance;
import com.amazonaws.services.rds.model.DBInstanceNotFoundException;
import com.amazonaws.services.rds.model.DescribeDBInstancesRequest;
import com.amazonaws.services.rds.model.DescribeDBInstancesResult;
import com.sequenceiq.cloudbreak.cloud.aws.LegacyAwsClient;
import com.sequenceiq.cloudbreak.cloud.aws.common.client.AmazonRdsClient;
import com.sequenceiq.cloudbreak.cloud.aws.common.view.AwsCredentialView;
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
    private LegacyAwsClient awsClient;

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
    private DescribeDBInstancesResult describeDBInstancesResult;

    @Mock
    private DBInstance dbInstance;

    @Mock
    private DatabaseStack dbStack;

    @Mock
    private DatabaseServer databaseServer;

    @InjectMocks
    private AwsRdsStatusLookupService victim;

    @BeforeEach
    public void initTests() {
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
    public void shouldLookupStartedExternalDatabaseStatus() {
        when(amazonRDS.describeDBInstances(any(DescribeDBInstancesRequest.class))).thenReturn(describeDBInstancesResult);
        when(describeDBInstancesResult.getDBInstances()).thenReturn(List.of(dbInstance));
        when(dbInstance.getDBInstanceStatus()).thenReturn(DB_INSTANCE_STATUS_STARTED);

        ExternalDatabaseStatus result = victim.getStatus(authenticatedContext, dbStack);

        assertThat(result).isEqualTo(ExternalDatabaseStatus.STARTED);
    }

    @Test
    public void shouldLookupStoppedExternalDatabaseStatus() {
        when(amazonRDS.describeDBInstances(any(DescribeDBInstancesRequest.class))).thenReturn(describeDBInstancesResult);
        when(describeDBInstancesResult.getDBInstances()).thenReturn(List.of(dbInstance));
        when(dbInstance.getDBInstanceStatus()).thenReturn(DB_INSTANCE_STATUS_STOPPED);

        ExternalDatabaseStatus result = victim.getStatus(authenticatedContext, dbStack);

        assertThat(result).isEqualTo(ExternalDatabaseStatus.STOPPED);
    }

    @Test
    public void shouldLookupUpdateInProgressExternalDatabaseStatus() {
        when(amazonRDS.describeDBInstances(any(DescribeDBInstancesRequest.class))).thenReturn(describeDBInstancesResult);
        when(describeDBInstancesResult.getDBInstances()).thenReturn(List.of(dbInstance));
        when(dbInstance.getDBInstanceStatus()).thenReturn(DB_INSTANCE_STATUS_ANY);

        ExternalDatabaseStatus result = victim.getStatus(authenticatedContext, dbStack);

        assertThat(result).isEqualTo(ExternalDatabaseStatus.UPDATE_IN_PROGRESS);
    }

    @Test
    public void shouldReturnDeletedInCaseOfDBInstanceNotFoundException() {
        when(amazonRDS.describeDBInstances(any(DescribeDBInstancesRequest.class))).thenThrow(DBInstanceNotFoundException.class);

        ExternalDatabaseStatus result = victim.getStatus(authenticatedContext, dbStack);

        assertThat(result).isEqualTo(ExternalDatabaseStatus.DELETED);
    }

    @Test()
    public void shouldThrowCloudConnectorExceptionInCaseOfAnyRuntimeException() {
        when(amazonRDS.describeDBInstances(any(DescribeDBInstancesRequest.class))).thenThrow(RuntimeException.class);

        assertThrows(CloudConnectorException.class, () -> victim.getStatus(authenticatedContext, dbStack));
    }

    @Test
    public void isDeleteProtectionEnabledTest() {
        when(describeDBInstancesResult.getDBInstances()).thenReturn(List.of(dbInstance));
        when(dbInstance.getDeletionProtection()).thenReturn(true);

        boolean result = victim.isDeleteProtectionEnabled(describeDBInstancesResult);
        assertThat(result).isTrue();

        when(dbInstance.getDeletionProtection()).thenReturn(false);

        result = victim.isDeleteProtectionEnabled(describeDBInstancesResult);
        assertThat(result).isFalse();
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
        assertThat(victim.isDbStackExistOnProviderSide(describeDBInstancesResult)).isTrue();
    }

    @Test
    void getDescribeDBInstancesResultForDeleteProtectionTestWhenSuccess() {
        when(amazonRDS.describeDBInstances(any(DescribeDBInstancesRequest.class))).thenReturn(describeDBInstancesResult);

        DescribeDBInstancesResult result = victim.getDescribeDBInstancesResultForDeleteProtection(authenticatedContext, dbStack);

        assertThat(result).isSameAs(describeDBInstancesResult);
    }

    @Test
    void getDescribeDBInstancesResultForDeleteProtectionTestWhenDBInstanceNotFoundException() {
        when(amazonRDS.describeDBInstances(any(DescribeDBInstancesRequest.class))).thenThrow(DBInstanceNotFoundException.class);

        DescribeDBInstancesResult result = victim.getDescribeDBInstancesResultForDeleteProtection(authenticatedContext, dbStack);

        assertThat(result).isNull();
    }

    @Test
    void getDescribeDBInstancesResultForDeleteProtectionTestWhenRuntimeExceptionThenThrowCloudConnectorException() {
        when(amazonRDS.describeDBInstances(any(DescribeDBInstancesRequest.class))).thenThrow(RuntimeException.class);

        assertThrows(CloudConnectorException.class, () -> victim.getDescribeDBInstancesResultForDeleteProtection(authenticatedContext, dbStack));
    }

    @Test
    void getActiveSslRootCertificateTestWhenSuccess() {
        when(amazonRDS.describeDBInstances(any(DescribeDBInstancesRequest.class))).thenReturn(describeDBInstancesResult);
        when(describeDBInstancesResult.getDBInstances()).thenReturn(List.of(dbInstance));
        when(dbInstance.getCACertificateIdentifier()).thenReturn(CA_CERTIFICATE_IDENTIFIER);

        CloudDatabaseServerSslCertificate result = victim.getActiveSslRootCertificate(authenticatedContext, dbStack);

        assertThat(result).isNotNull();
        assertThat(result.getCertificateType()).isEqualTo(CloudDatabaseServerSslCertificateType.ROOT);
        assertThat(result.getCertificateIdentifier()).isEqualTo(CA_CERTIFICATE_IDENTIFIER);
    }

    @Test
    void getActiveSslRootCertificateTestWhenDBInstanceNotFoundException() {
        when(amazonRDS.describeDBInstances(any(DescribeDBInstancesRequest.class))).thenThrow(DBInstanceNotFoundException.class);

        CloudDatabaseServerSslCertificate result = victim.getActiveSslRootCertificate(authenticatedContext, dbStack);

        assertThat(result).isNull();
    }

    @Test
    void getActiveSslRootCertificateTestTestWhenRuntimeExceptionThenThrowCloudConnectorException() {
        when(amazonRDS.describeDBInstances(any(DescribeDBInstancesRequest.class))).thenThrow(RuntimeException.class);

        assertThrows(CloudConnectorException.class, () -> victim.getActiveSslRootCertificate(authenticatedContext, dbStack));
    }

}