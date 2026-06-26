package com.sequenceiq.cloudbreak.cloud.aws.connector.resource;

import static com.sequenceiq.common.api.type.ResourceType.AWS_ROOT_DISK;
import static com.sequenceiq.common.api.type.ResourceType.AWS_VOLUMESET;
import static com.sequenceiq.common.api.type.ResourceType.CLOUDFORMATION_STACK;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.cloud.aws.connector.resource.tag.AwsResourceTagUpdaterService;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.DatabaseServer;
import com.sequenceiq.cloudbreak.cloud.model.DatabaseStack;
import com.sequenceiq.cloudbreak.cloud.model.database.CloudDatabaseServerSslCertificate;
import com.sequenceiq.cloudbreak.cloud.notification.PersistenceNotifier;
import com.sequenceiq.common.api.type.ResourceType;

import software.amazon.awssdk.services.rds.model.DescribeDbInstancesResponse;

@ExtendWith(MockitoExtension.class)
class AwsResourceConnectorTest {

    private static final String DB_INSTANCE_IDENTIFIER = "dbInstance";

    @Mock
    private AuthenticatedContext authenticatedContext;

    @Mock
    private DatabaseStack dbStack;

    @Mock
    private AwsRdsStatusLookupService awsRdsStatusLookupService;

    @Mock
    private AwsRdsModifyService awsRdsModifyService;

    @Mock
    private AwsRdsTerminateService awsRdsTerminateService;

    @Mock
    private AwsResourceTagUpdaterService awsResourceTagUpdaterService;

    @InjectMocks
    private AwsResourceConnector underTest;

    @Test
    void terminateDatabaseServerWithDeleteProtectionTest() throws Exception {
        DatabaseServer databaseServer = mock(DatabaseServer.class);
        when(dbStack.getDatabaseServer()).thenReturn(databaseServer);
        when(databaseServer.getServerId()).thenReturn(DB_INSTANCE_IDENTIFIER);

        PersistenceNotifier persistenceNotifier = mock(PersistenceNotifier.class);
        DescribeDbInstancesResponse describeDBInstancesResult = DescribeDbInstancesResponse.builder().build();
        when(awsRdsStatusLookupService.getDescribeDBInstancesResponseForDeleteProtection(any(), any())).thenReturn(describeDBInstancesResult);
        when(awsRdsStatusLookupService.isDeleteProtectionEnabled(describeDBInstancesResult)).thenReturn(true);

        underTest.terminateDatabaseServer(authenticatedContext, dbStack,
                Collections.emptyList(), persistenceNotifier, true);
        verify(awsRdsModifyService, times(1)).disableDeleteProtection(any(), any());
        verify(awsRdsTerminateService, times(1)).terminate(authenticatedContext, dbStack,
                true, persistenceNotifier, Collections.emptyList());

    }

    @Test
    void terminateDatabaseServerWithOutDeleteProtectionTest() throws Exception {
        PersistenceNotifier persistenceNotifier = mock(PersistenceNotifier.class);
        DescribeDbInstancesResponse describeDBInstancesResult = DescribeDbInstancesResponse.builder().build();
        when(awsRdsStatusLookupService.getDescribeDBInstancesResponseForDeleteProtection(any(), any())).thenReturn(describeDBInstancesResult);
        when(awsRdsStatusLookupService.isDeleteProtectionEnabled(describeDBInstancesResult)).thenReturn(false);

        underTest.terminateDatabaseServer(authenticatedContext, dbStack,
                Collections.emptyList(), persistenceNotifier, true);
        verify(awsRdsModifyService, times(0)).disableDeleteProtection(any(), any());
        verify(awsRdsTerminateService, times(1)).terminate(authenticatedContext, dbStack,
                true, persistenceNotifier, Collections.emptyList());
    }

    @Test
    void getDatabaseServerActiveSslRootCertificateTest() {
        CloudDatabaseServerSslCertificate sslCertificate = mock(CloudDatabaseServerSslCertificate.class);
        when(awsRdsStatusLookupService.getActiveSslRootCertificate(authenticatedContext, dbStack)).thenReturn(sslCertificate);

        CloudDatabaseServerSslCertificate result = underTest.getDatabaseServerActiveSslRootCertificate(authenticatedContext, dbStack);

        assertThat(result).isSameAs(sslCertificate);
    }

    @Test
    public void testMigrateDatabaseFromNonSslToSsl() {
        AuthenticatedContext authenticatedContext = mock(AuthenticatedContext.class);
        DatabaseStack databaseStack = mock(DatabaseStack.class);

        underTest.migrateDatabaseFromNonSslToSsl(authenticatedContext, databaseStack);

        verify(awsRdsModifyService, times(1)).migrateNonSslToSsl(any(), any());
    }

    @Test
    void testUpdateTags() {
        AuthenticatedContext ac = mock(AuthenticatedContext.class);
        CloudResource cloudFormationStack = CloudResource.builder()
                .withType(CLOUDFORMATION_STACK)
                .withName("awsCloudFormationStack")
                .build();

        CloudResource awsVolumeSet = CloudResource.builder()
                .withType(AWS_VOLUMESET)
                .withName("awsVolumeSet")
                .build();

        CloudResource awsRootDisk = CloudResource.builder()
                .withType(AWS_ROOT_DISK)
                .withName("awsRootDisk")
                .build();

        Map<String, String> userDefinedTags = Map.of("custom", "value");

        underTest.updateTags(ac, List.of(cloudFormationStack, awsVolumeSet, awsRootDisk), userDefinedTags);

        verify(awsResourceTagUpdaterService).updateTags(ac, List.of(cloudFormationStack, awsVolumeSet, awsRootDisk), userDefinedTags);
    }

    @ParameterizedTest
    @MethodSource("emptyAndNullUserDefinedTags")
    void testUpdateTagsWhenUserDefinedTagsIsEmpty(Map<String, String> userDefinedTags) {
        AuthenticatedContext ac = mock(AuthenticatedContext.class);
        CloudResource cloudFormationStack = CloudResource.builder()
                .withType(ResourceType.CLOUDFORMATION_STACK)
                .withName("awsCloudFormationStack")
                .build();

        CloudResource awsVolumeSet = CloudResource.builder()
                .withType(AWS_VOLUMESET)
                .withName("awsVolumeSet")
                .build();

        CloudResource awsRootDisk = CloudResource.builder()
                .withType(AWS_ROOT_DISK)
                .withName("awsRootDisk")
                .build();

        underTest.updateTags(ac, List.of(cloudFormationStack, awsVolumeSet, awsRootDisk), userDefinedTags);

        verifyNoInteractions(awsResourceTagUpdaterService);
    }

    private static Stream<Arguments> emptyAndNullUserDefinedTags() {
        return Stream.of(
                Arguments.of((Object) null),
                Arguments.of(Collections.emptyMap())
        );
    }
}
