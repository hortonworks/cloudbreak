package com.sequenceiq.freeipa.service.encryption;

import java.util.List;

import jakarta.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.aws.common.AwsConstants;
import com.sequenceiq.cloudbreak.cloud.aws.common.util.ArnService;
import com.sequenceiq.cloudbreak.cloud.model.Platform;
import com.sequenceiq.cloudbreak.cloud.model.Variant;
import com.sequenceiq.cloudbreak.cloud.model.encryption.EncryptionKeyType;
import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.common.api.type.ResourceType;
import com.sequenceiq.environment.api.v1.credential.model.response.CredentialResponse;
import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;
import com.sequenceiq.freeipa.entity.InstanceMetaData;
import com.sequenceiq.freeipa.entity.Stack;

import software.amazon.awssdk.arns.Arn;

@Component
public class AwsCloudInformationDecorator implements CloudInformationDecorator {

    private static final String COMMERCIAL_ARN_PARTITION = "aws";

    @Inject
    private ArnService arnService;

    @Override
    public List<String> getLuksEncryptionKeyCryptographicPrincipals(DetailedEnvironmentResponse environment) {
        String instanceProfileArn = getInstanceProfileArnFromEnvironment(environment);
        return List.of(instanceProfileArn);
    }

    @Override
    public List<String> getCloudSecretManagerEncryptionKeyCryptographicPrincipals(DetailedEnvironmentResponse environment) {
        String crossAccountRoleArn = getCrossAccountRoleArnFromCredential(environment.getCredential());
        String instanceProfileArn = getInstanceProfileArnFromEnvironment(environment);
        return List.of(crossAccountRoleArn, instanceProfileArn);
    }

    @Override
    public ResourceType getLuksEncryptionKeyResourceType() {
        return ResourceType.AWS_KMS_KEY;
    }

    @Override
    public ResourceType getCloudSecretManagerEncryptionKeyResourceType() {
        return ResourceType.AWS_KMS_KEY;
    }

    @Override
    public String getAuthorizedClientForLuksEncryptionKey(Stack stack, InstanceMetaData instanceMetaData) {
        String accountId = getAccountIdFromInstanceProfileArn(getInstanceProfileArnFromStack(stack));
        return arnService.buildEc2InstanceArn(getArnPartition(), stack.getRegion(), accountId, instanceMetaData.getInstanceId());
    }

    protected String getArnPartition() {
        return COMMERCIAL_ARN_PARTITION;
    }

    @Override
    public EncryptionKeyType getUserdataSecretEncryptionKeyType() {
        return EncryptionKeyType.AWS_KMS_KEY_ARN;
    }

    @Override
    public List<String> getUserdataSecretCryptographicPrincipals(Stack stack, CredentialResponse credentialResponse) {
        String instanceProfileArn = getInstanceProfileArnFromStack(stack);
        String crossAccountRoleArn = getCrossAccountRoleArnFromCredential(credentialResponse);
        return List.of(instanceProfileArn, crossAccountRoleArn);
    }

    @Override
    public List<String> getUserdataSecretCryptographicAuthorizedClients(Stack stack, String instanceId) {
        String accountId = getAccountIdFromInstanceProfileArn(getInstanceProfileArnFromStack(stack));
        return List.of(arnService.buildEc2InstanceArn(getArnPartition(), stack.getRegion(), accountId, instanceId));
    }

    private String getCrossAccountRoleArnFromCredential(CredentialResponse credentialResponse) {
        if (credentialResponse != null && credentialResponse.getAws() != null
                && credentialResponse.getAws().getRoleBased() != null
                && StringUtils.isNotBlank(credentialResponse.getAws().getRoleBased().getRoleArn())) {
            return credentialResponse.getAws().getRoleBased().getRoleArn();
        } else {
            throw new CloudbreakServiceException(String.format("Cross account role not found for credential %s.", credentialResponse));
        }
    }

    private String getInstanceProfileArnFromEnvironment(DetailedEnvironmentResponse environment) {
        if (environment.getTelemetry() != null && environment.getTelemetry().getLogging() != null
                && environment.getTelemetry().getLogging().getS3() != null
                && StringUtils.isNotBlank(environment.getTelemetry().getLogging().getS3().getInstanceProfile())) {
            return environment.getTelemetry().getLogging().getS3().getInstanceProfile();
        } else {
            throw new CloudbreakServiceException(String.format("Logger instance profile not found for environment %s.", environment.getName()));
        }
    }

    private String getInstanceProfileArnFromStack(Stack stack) {
        if (stack.getTelemetry() != null && stack.getTelemetry().getLogging() != null
                && stack.getTelemetry().getLogging().getS3() != null
                && StringUtils.isNotBlank(stack.getTelemetry().getLogging().getS3().getInstanceProfile())) {
            return stack.getTelemetry().getLogging().getS3().getInstanceProfile();
        } else {
            throw new CloudbreakServiceException(String.format("Logger instance profile not found for stack %s.", stack.getName()));
        }
    }

    private String getAccountIdFromInstanceProfileArn(String instanceProfileArn) {
        Arn arn = Arn.fromString(instanceProfileArn);
        if (arn.accountId().isPresent()) {
            return arn.accountId().get();
        } else {
            throw new CloudbreakServiceException("Instance profile ARN is missing the accountid!");
        }
    }

    @Override
    public Platform platform() {
        return AwsConstants.AWS_PLATFORM;
    }

    @Override
    public Variant variant() {
        return AwsConstants.AWS_DEFAULT_VARIANT;
    }
}
