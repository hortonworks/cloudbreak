package com.sequenceiq.freeipa.service.encryption;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.cloud.aws.common.AwsConstants;
import com.sequenceiq.cloudbreak.cloud.aws.common.util.ArnService;
import com.sequenceiq.cloudbreak.cloud.model.encryption.EncryptionKeyType;
import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.common.api.telemetry.model.Logging;
import com.sequenceiq.common.api.telemetry.model.Telemetry;
import com.sequenceiq.common.api.telemetry.response.LoggingResponse;
import com.sequenceiq.common.api.telemetry.response.TelemetryResponse;
import com.sequenceiq.common.api.type.ResourceType;
import com.sequenceiq.environment.api.v1.credential.model.response.CredentialResponse;
import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;
import com.sequenceiq.freeipa.TestUtil;
import com.sequenceiq.freeipa.entity.InstanceMetaData;
import com.sequenceiq.freeipa.entity.Stack;

@ExtendWith(MockitoExtension.class)
class AwsCloudInformationDecoratorTest {

    private static final String INSTANCE_PROFILE_ARN = "arn:aws:iam::123456789012:instance-profile/example-instance-profile";

    private static final String CROSS_ACCOUNT_ROLE_ARN = "arn:aws:iam::123456789012:role/example-cred-role";

    private static final String EC2_INSTANCE_ARN = "arn:aws:ec2:us-west-1:123456789012:instance/instance-id";

    @Mock
    private ArnService arnService;

    @InjectMocks
    private AwsCloudInformationDecorator underTest;

    @Test
    void testGetLuksEncryptionKeyCryptographicPrincipals() {
        TelemetryResponse telemetryResponse = new TelemetryResponse();
        LoggingResponse loggingResponse = new LoggingResponse();
        loggingResponse.setS3(TestUtil.getS3CloudStorageV1Parameters(INSTANCE_PROFILE_ARN));
        telemetryResponse.setLogging(loggingResponse);
        DetailedEnvironmentResponse environment = DetailedEnvironmentResponse.builder()
                .withTelemetry(telemetryResponse)
                .build();

        List<String> result = underTest.getLuksEncryptionKeyCryptographicPrincipals(environment);

        assertEquals(List.of(INSTANCE_PROFILE_ARN), result);
    }

    @Test
    void testGetCloudSecretManagerEncryptionKeyCryptographicPrincipals() {
        TelemetryResponse telemetryResponse = new TelemetryResponse();
        LoggingResponse loggingResponse = new LoggingResponse();
        loggingResponse.setS3(TestUtil.getS3CloudStorageV1Parameters(INSTANCE_PROFILE_ARN));
        telemetryResponse.setLogging(loggingResponse);
        DetailedEnvironmentResponse environment = DetailedEnvironmentResponse.builder()
                .withCredential(CredentialResponse.builder()
                        .withAws(TestUtil.getAwsCredentialParameters(CROSS_ACCOUNT_ROLE_ARN))
                        .build())
                .withTelemetry(telemetryResponse)
                .build();

        List<String> result = underTest.getCloudSecretManagerEncryptionKeyCryptographicPrincipals(environment);

        assertEquals(List.of(CROSS_ACCOUNT_ROLE_ARN, INSTANCE_PROFILE_ARN), result);
    }

    @Test
    void testGetLuksEncryptionKeyResourceType() {
        assertEquals(ResourceType.AWS_KMS_KEY, underTest.getLuksEncryptionKeyResourceType());
    }

    @Test
    void testGetCloudSecretManagerEncryptionKeyResourceType() {
        assertEquals(ResourceType.AWS_KMS_KEY, underTest.getCloudSecretManagerEncryptionKeyResourceType());
    }

    @Test
    void testGetAuthorizedClientForLuksEncryptionKey() {
        Stack stack = getStackWithInstanceProfileAndRegion(INSTANCE_PROFILE_ARN, "us-west-1");
        InstanceMetaData instanceMetaData = new InstanceMetaData();
        instanceMetaData.setInstanceId("instance-id");
        when(arnService.buildEc2InstanceArn("aws", "us-west-1", "123456789012", "instance-id")).thenReturn(EC2_INSTANCE_ARN);

        String result = underTest.getAuthorizedClientForLuksEncryptionKey(stack, instanceMetaData);

        assertEquals(EC2_INSTANCE_ARN, result);
    }

    @Test
    void testGetAuthorizedClientForLuksEncryptionKeyWhenNoAccountIdInInstanceProfile() {
        Stack stack = getStackWithInstanceProfileAndRegion(INSTANCE_PROFILE_ARN.replace("123456789012", ""), "us-west-1");
        InstanceMetaData instanceMetaData = new InstanceMetaData();

        assertThrows(CloudbreakServiceException.class, () -> underTest.getAuthorizedClientForLuksEncryptionKey(stack, instanceMetaData));
    }

    @Test
    void testGetArnPartition() {
        assertEquals("aws", underTest.getArnPartition());
    }

    @Test
    void testGetUserdataSecretEncryptionKeyType() {
        assertEquals(EncryptionKeyType.AWS_KMS_KEY_ARN, underTest.getUserdataSecretEncryptionKeyType());
    }

    @Test
    void testGetUserdataSecretCryptographicPrincipals() {
        Stack stack = getStackWithInstanceProfileAndRegion(INSTANCE_PROFILE_ARN, "us-west-1");
        CredentialResponse credentialResponse = CredentialResponse.builder()
                .withAws(TestUtil.getAwsCredentialParameters(CROSS_ACCOUNT_ROLE_ARN))
                .build();

        List<String> result = underTest.getUserdataSecretCryptographicPrincipals(stack, credentialResponse);

        assertEquals(List.of(INSTANCE_PROFILE_ARN, CROSS_ACCOUNT_ROLE_ARN), result);
    }

    @Test
    void testGetUserdataSecretCryptographicAuthorizedClients() {
        Stack stack = getStackWithInstanceProfileAndRegion(INSTANCE_PROFILE_ARN, "us-west-1");
        when(arnService.buildEc2InstanceArn("aws", "us-west-1", "123456789012", "instance-id")).thenReturn(EC2_INSTANCE_ARN);

        List<String> result = underTest.getUserdataSecretCryptographicAuthorizedClients(stack, "instance-id");

        assertEquals(List.of(EC2_INSTANCE_ARN), result);
    }

    @Test
    void testGetUserdataSecretResourceType() {
        assertEquals(ResourceType.AWS_SECRETSMANAGER_SECRET, underTest.getUserdataSecretResourceType());
    }

    @Test
    void testPlatform() {
        assertEquals(AwsConstants.AWS_PLATFORM, underTest.platform());
    }

    @Test
    void testVariant() {
        assertEquals(AwsConstants.AWS_DEFAULT_VARIANT, underTest.variant());
    }

    private static Stack getStackWithInstanceProfileAndRegion(String instanceProfile, String region) {
        Stack stack = new Stack();
        Telemetry telemetry = new Telemetry();
        Logging logging = new Logging();
        logging.setS3(TestUtil.getS3CloudStorageV1Parameters(instanceProfile));
        telemetry.setLogging(logging);
        stack.setTelemetry(telemetry);
        stack.setRegion(region);
        return stack;
    }
}
