package com.sequenceiq.cloudbreak.service.encryption;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import com.sequenceiq.cloudbreak.cloud.aws.common.AwsConstants;
import com.sequenceiq.cloudbreak.cloud.model.Platform;
import com.sequenceiq.cloudbreak.cloud.model.Variant;
import com.sequenceiq.cloudbreak.domain.FileSystem;
import com.sequenceiq.cloudbreak.domain.cloudstorage.CloudIdentity;
import com.sequenceiq.cloudbreak.domain.cloudstorage.CloudStorage;
import com.sequenceiq.cloudbreak.domain.cloudstorage.S3Identity;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.common.api.cloudstorage.old.S3CloudStorageV1Parameters;
import com.sequenceiq.common.api.telemetry.response.LoggingResponse;
import com.sequenceiq.common.api.telemetry.response.TelemetryResponse;
import com.sequenceiq.common.model.FileSystemType;
import com.sequenceiq.environment.api.v1.credential.model.parameters.aws.AwsCredentialParameters;
import com.sequenceiq.environment.api.v1.credential.model.parameters.aws.RoleBasedParameters;
import com.sequenceiq.environment.api.v1.credential.model.response.CredentialResponse;
import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class AwsCloudInformationDecoratorTest {

    private static final String CROSS_ACCOUNT_ROLE = "cross-account-role";

    private static final String LOGGER_INSTANCE_PROFILE = "logger-instance-profile";

    @Mock
    private DetailedEnvironmentResponse environment;

    @Mock
    private Stack stack;

    @InjectMocks
    private AwsCloudInformationDecorator underTest;

    @BeforeEach
    public void setUp() {
        setUpEnvironment();
    }

    @Test
    void testGetLoggerInstances() {
        List<String> loggerInstances = underTest.getLoggerInstances(environment, stack);
        assertEquals(List.of(LOGGER_INSTANCE_PROFILE), loggerInstances);
    }

    @Test
    void testGetLoggerInstancesTelemetryNotAvailable() {
        when(environment.getTelemetry()).thenReturn(null);
        List<String> loggerInstances = underTest.getLoggerInstances(environment, stack);
        assertEquals(List.of(), loggerInstances);
    }

    @Test
    void testGetLoggerInstancesTelemetryLoggingNotAvailable() {
        TelemetryResponse telemetryResponse = mock(TelemetryResponse.class);
        when(telemetryResponse.getLogging()).thenReturn(null);
        when(environment.getTelemetry()).thenReturn(telemetryResponse);
        List<String> loggerInstances = underTest.getLoggerInstances(environment, stack);
        assertEquals(List.of(), loggerInstances);
    }

    @Test
    void testGetLoggerInstancesTelemetryLoggingS3InstanceProfileNotAvailable() {
        S3CloudStorageV1Parameters s3CloudStorageV1Parameters = mock(S3CloudStorageV1Parameters.class);
        when(s3CloudStorageV1Parameters.getInstanceProfile()).thenReturn(null);
        LoggingResponse loggingResponse = mock(LoggingResponse.class);
        when(loggingResponse.getS3()).thenReturn(s3CloudStorageV1Parameters);
        TelemetryResponse telemetryResponse = mock(TelemetryResponse.class);
        when(telemetryResponse.getLogging()).thenReturn(loggingResponse);
        when(environment.getTelemetry()).thenReturn(telemetryResponse);
        List<String> loggerInstances = underTest.getLoggerInstances(environment, stack);
        assertEquals(List.of(), loggerInstances);
    }

    @Test
    void testGetCredentialPrincipal() {
        String credentialPrincipal = underTest.getCredentialPrincipal(environment, stack).get();
        assertEquals(CROSS_ACCOUNT_ROLE, credentialPrincipal);
    }

    @Test
    void testGetCredentialPrincipalCredentialIsNull() {
        when(environment.getCredential()).thenReturn(null);
        Optional<String> credentialPrincipal = underTest.getCredentialPrincipal(environment, stack);
        assertEquals(true, credentialPrincipal.isEmpty());
    }

    @Test
    void testGetCredentialPrincipalCredentialAwsIsNull() {
        CredentialResponse credentialResponse = mock(CredentialResponse.class);
        when(credentialResponse.getAws()).thenReturn(null);
        when(environment.getCredential()).thenReturn(credentialResponse);
        Optional<String> credentialPrincipal = underTest.getCredentialPrincipal(environment, stack);
        assertEquals(true, credentialPrincipal.isEmpty());
    }

    @Test
    void testGetCredentialPrincipalCredentialAwsRoleBasedIsNull() {
        AwsCredentialParameters awsCredentialParameters = mock(AwsCredentialParameters.class);
        when(awsCredentialParameters.getRoleBased()).thenReturn(null);
        CredentialResponse credentialResponse = mock(CredentialResponse.class);
        when(credentialResponse.getAws()).thenReturn(awsCredentialParameters);
        when(environment.getCredential()).thenReturn(credentialResponse);
        Optional<String> credentialPrincipal = underTest.getCredentialPrincipal(environment, stack);
        assertEquals(true, credentialPrincipal.isEmpty());
    }

    @Test
    void testGetCredentialPrincipalCredentialAwsRoleBasedArnIsNull() {
        RoleBasedParameters roleBasedParameters = mock(RoleBasedParameters.class);
        when(roleBasedParameters.getRoleArn()).thenReturn(null);
        AwsCredentialParameters awsCredentialParameters = mock(AwsCredentialParameters.class);
        when(awsCredentialParameters.getRoleBased()).thenReturn(roleBasedParameters);
        CredentialResponse credentialResponse = mock(CredentialResponse.class);
        when(credentialResponse.getAws()).thenReturn(awsCredentialParameters);
        when(environment.getCredential()).thenReturn(credentialResponse);
        Optional<String> credentialPrincipal = underTest.getCredentialPrincipal(environment, stack);
        assertEquals(true, credentialPrincipal.isEmpty());
    }

    @Test
    void testGetCloudIdentities() {
        List<String> cloudIdentities = underTest.getCloudIdentities(environment, stack);
        assertEquals(List.of("idbroker", "logger"), cloudIdentities);
    }

    @Test
    void testGetCloudIdentitiesClusterIsNull() {
        when(stack.getCluster()).thenReturn(null);
        List<String> cloudIdentities = underTest.getCloudIdentities(environment, stack);
        assertEquals(List.of(), cloudIdentities);
    }

    @Test
    void testGetCloudIdentitiesClusterFileSystemIsNull() {
        Cluster cluster = mock(Cluster.class);
        when(cluster.getFileSystem()).thenReturn(null);
        when(stack.getCluster()).thenReturn(cluster);
        List<String> cloudIdentities = underTest.getCloudIdentities(environment, stack);
        assertEquals(List.of(), cloudIdentities);
    }

    @Test
    void testGetCloudIdentitiesClusterFileSystemCloudStorageIsNull() {
        FileSystem fileSystem = mock(FileSystem.class);
        when(fileSystem.getCloudStorage()).thenReturn(null);
        Cluster cluster = mock(Cluster.class);
        when(cluster.getFileSystem()).thenReturn(fileSystem);
        when(stack.getCluster()).thenReturn(cluster);
        List<String> cloudIdentities = underTest.getCloudIdentities(environment, stack);
        assertEquals(List.of(), cloudIdentities);
    }

    @Test
    void testGetCloudIdentitiesClusterFileSystemCloudStorageCloudIdentitiesAreEmpty() {
        CloudStorage cloudStorage = mock(CloudStorage.class);
        when(cloudStorage.getCloudIdentities()).thenReturn(List.of());
        FileSystem fileSystem = mock(FileSystem.class);
        when(fileSystem.getCloudStorage()).thenReturn(cloudStorage);
        Cluster cluster = mock(Cluster.class);
        when(cluster.getFileSystem()).thenReturn(fileSystem);
        when(stack.getCluster()).thenReturn(cluster);
        List<String> cloudIdentities = underTest.getCloudIdentities(environment, stack);
        assertEquals(List.of(), cloudIdentities);
    }

    @Test
    void testPlatform() {
        Platform platform = underTest.platform();
        assertEquals(AwsConstants.AWS_PLATFORM, platform);
    }

    @Test
    void testVariant() {
        Variant variant = underTest.variant();
        assertEquals(AwsConstants.AWS_DEFAULT_VARIANT, variant);
    }

    private void setUpEnvironment() {
        S3CloudStorageV1Parameters s3CloudStorageV1Parameters = mock(S3CloudStorageV1Parameters.class);
        when(s3CloudStorageV1Parameters.getInstanceProfile()).thenReturn(LOGGER_INSTANCE_PROFILE);
        LoggingResponse loggingResponse = mock(LoggingResponse.class);
        when(loggingResponse.getS3()).thenReturn(s3CloudStorageV1Parameters);
        TelemetryResponse telemetryResponse = mock(TelemetryResponse.class);
        when(telemetryResponse.getLogging()).thenReturn(loggingResponse);
        when(environment.getTelemetry()).thenReturn(telemetryResponse);


        RoleBasedParameters roleBasedParameters = mock(RoleBasedParameters.class);
        when(roleBasedParameters.getRoleArn()).thenReturn(CROSS_ACCOUNT_ROLE);
        AwsCredentialParameters awsCredentialParameters = mock(AwsCredentialParameters.class);
        when(awsCredentialParameters.getRoleBased()).thenReturn(roleBasedParameters);
        CredentialResponse credentialResponse = mock(CredentialResponse.class);
        when(credentialResponse.getAws()).thenReturn(awsCredentialParameters);
        when(environment.getCredential()).thenReturn(credentialResponse);

        S3Identity s3Identity = mock(S3Identity.class);
        when(s3Identity.getInstanceProfile()).thenReturn("idbroker").thenReturn("logger");
        FileSystemType fileSystemType = mock(FileSystemType.class);
        when(fileSystemType.isS3()).thenReturn(true);
        CloudIdentity cloudIdentity = mock(CloudIdentity.class);
        when(cloudIdentity.getS3Identity()).thenReturn(s3Identity);
        when(cloudIdentity.getFileSystemType()).thenReturn(fileSystemType);
        CloudStorage cloudStorage = mock(CloudStorage.class);
        when(cloudStorage.getCloudIdentities()).thenReturn(List.of(cloudIdentity, cloudIdentity));
        FileSystem fileSystem = mock(FileSystem.class);
        when(fileSystem.getCloudStorage()).thenReturn(cloudStorage);
        Cluster cluster = mock(Cluster.class);
        when(cluster.getFileSystem()).thenReturn(fileSystem);
        when(stack.getCluster()).thenReturn(cluster);
    }
}
