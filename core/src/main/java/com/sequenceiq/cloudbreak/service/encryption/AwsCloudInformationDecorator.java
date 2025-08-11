package com.sequenceiq.cloudbreak.service.encryption;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import jakarta.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import com.sequenceiq.cloudbreak.cloud.aws.common.AwsConstants;
import com.sequenceiq.cloudbreak.cloud.aws.common.util.ArnService;
import com.sequenceiq.cloudbreak.cloud.model.Platform;
import com.sequenceiq.cloudbreak.cloud.model.Variant;
import com.sequenceiq.cloudbreak.cloud.model.encryption.EncryptionKeyType;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessor;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessorFactory;
import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.converter.spi.CloudIdentityTypeDecider;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceGroup;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;
import com.sequenceiq.common.api.type.ResourceType;
import com.sequenceiq.common.model.CloudIdentityType;
import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;

import software.amazon.awssdk.arns.Arn;

@Component
public class AwsCloudInformationDecorator implements CloudInformationDecorator {

    private static final String COMMERCIAL_ARN_PARTITION = "aws";

    @Inject
    private ArnService arnService;

    @Inject
    private CmTemplateProcessorFactory cmTemplateProcessorFactory;

    @Inject
    private CloudIdentityTypeDecider cloudIdentityTypeDecider;

    @Override
    public List<String> getLuksEncryptionKeyCryptographicPrincipals(DetailedEnvironmentResponse environment, Stack stack) {
        return switch (stack.getType()) {
            case DATALAKE -> getInstanceProfileArnsFromStack(stack);
            case WORKLOAD -> List.of(getLoggerInstanceProfileArnFromEnvironment(environment));
            default -> throw new CloudbreakServiceException(String.format("Unsupported cluster type: %s", stack.getType()));
        };
    }

    private List<String> getInstanceProfileArnsFromStack(Stack stack) {
        List<String> arns = new ArrayList<>();
        if (stack.getCluster() != null && stack.getCluster().getFileSystem() != null &&
                stack.getCluster().getFileSystem().getCloudStorage() != null
                && stack.getCluster().getFileSystem().getCloudStorage().getCloudIdentities() != null) {
            arns.addAll(stack.getCluster().getFileSystem().getCloudStorage().getCloudIdentities().stream()
                    .filter(cloudIdentity -> cloudIdentity.getFileSystemType().isS3())
                    .map(cloudIdentity -> cloudIdentity.getS3Identity().getInstanceProfile())
                    .filter(StringUtils::isNotBlank)
                    .toList());
        }
        if (CollectionUtils.isEmpty(arns)) {
            throw new CloudbreakServiceException("Unable to determine cryptographic principals for the LUKS KMS key!");
        }
        return arns;
    }

    private String getLoggerInstanceProfileArnFromEnvironment(DetailedEnvironmentResponse environment) {
        if (environment.getTelemetry() != null && environment.getTelemetry().getLogging() != null
                && environment.getTelemetry().getLogging().getS3() != null
                && StringUtils.isNotBlank(environment.getTelemetry().getLogging().getS3().getInstanceProfile())) {
            return environment.getTelemetry().getLogging().getS3().getInstanceProfile();
        } else {
            throw new CloudbreakServiceException("Unable to determine instance profile for the environment!");
        }
    }

    @Override
    public List<String> getCloudSecretManagerEncryptionKeyCryptographicPrincipals(DetailedEnvironmentResponse environment, Stack stack) {
        List<String> principalIds = new ArrayList<>();
        principalIds.add(getCrossAccountRoleArnFromEnvironment(environment));
        switch (stack.getType()) {
            case DATALAKE -> principalIds.addAll(getInstanceProfileArnsFromStack(stack));
            case WORKLOAD -> principalIds.add(getLoggerInstanceProfileArnFromEnvironment(environment));
            default -> throw new CloudbreakServiceException(String.format("Unsupported cluster type: %s", stack.getType()));
        }
        return principalIds;
    }

    private String getCrossAccountRoleArnFromEnvironment(DetailedEnvironmentResponse environment) {
        if (environment.getCredential() != null && environment.getCredential().getAws() != null
                && environment.getCredential().getAws().getRoleBased() != null
                && StringUtils.isNotBlank(environment.getCredential().getAws().getRoleBased().getRoleArn())) {
            return environment.getCredential().getAws().getRoleBased().getRoleArn();
        } else {
            throw new CloudbreakServiceException("Unable to determine cross account role arn for the environment!");
        }
    }

    @Override
    public EncryptionKeyType getUserdataSecretEncryptionKeyType() {
        return EncryptionKeyType.AWS_KMS_KEY_ARN;
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
        String accountId = getAccountIdFromInstanceProfileArn(getInstanceProfileArnsFromStack(stack).getFirst());
        return arnService.buildEc2InstanceArn(getArnPartition(), stack.getRegion(), accountId, instanceMetaData.getInstanceId());
    }

    private String getAccountIdFromInstanceProfileArn(String instanceProfileArn) {
        Arn arn = Arn.fromString(instanceProfileArn);
        if (arn.accountId().isPresent()) {
            return arn.accountId().get();
        } else {
            throw new CloudbreakServiceException("Instance profile ARN is missing the accountid!");
        }
    }

    protected String getArnPartition() {
        return COMMERCIAL_ARN_PARTITION;
    }

    @Override
    public Map<String, List<String>> getUserdataSecretCryptographicPrincipalsForInstanceGroups(DetailedEnvironmentResponse environment, Stack stack) {
        CmTemplateProcessor cmTemplateProcessor = cmTemplateProcessorFactory.get(stack.getBlueprintJsonText());
        Map<String, Set<String>> componentsByHostGroup = cmTemplateProcessor.getComponentsByHostGroup();
        Map<String, List<String>> cryptographicPrincipals = new HashMap<>();
        String crossAccountRoleArn = getCrossAccountRoleArnFromEnvironment(environment);
        for (InstanceGroup instanceGroup : stack.getInstanceGroups()) {
            String instanceProfileArn = getInstanceProfileArnBasedOnInstanceGroupType(stack, instanceGroup.getGroupName(), componentsByHostGroup);
            cryptographicPrincipals.put(instanceGroup.getGroupName(), List.of(crossAccountRoleArn, instanceProfileArn));
        }
        return cryptographicPrincipals;
    }

    private String getInstanceProfileArnBasedOnInstanceGroupType(Stack stack, String instanceGroupName, Map<String, Set<String>> componentsByHostGroup) {
        Optional<String> instanceProfileArn = Optional.empty();
        if (stack.getCluster() != null && stack.getCluster().getFileSystem() != null &&
                stack.getCluster().getFileSystem().getCloudStorage() != null
                && stack.getCluster().getFileSystem().getCloudStorage().getCloudIdentities() != null) {
            CloudIdentityType identityType = cloudIdentityTypeDecider.getIdentityTypeForInstanceGroup(instanceGroupName, componentsByHostGroup);
            instanceProfileArn = stack.getCluster().getFileSystem().getCloudStorage().getCloudIdentities().stream()
                    .filter(cloudIdentity -> cloudIdentity.getFileSystemType().isS3() && identityType == cloudIdentity.getIdentityType())
                    .map(cloudIdentity -> cloudIdentity.getS3Identity().getInstanceProfile())
                    .filter(Objects::nonNull)
                    .findFirst();
        }
        if (instanceProfileArn.isEmpty()) {
            throw new CloudbreakServiceException(String.format("Unable to determine instance profile for stack '%s' and instance group '%s'!",
                    stack.getName(), instanceGroupName));
        }
        return instanceProfileArn.get();
    }

    @Override
    public List<String> getUserdataSecretCryptographicAuthorizedClients(Stack stack, String instanceId) {
        String accountId = getAccountIdFromInstanceProfileArn(getInstanceProfileArnsFromStack(stack).getFirst());
        return List.of(arnService.buildEc2InstanceArn(getArnPartition(), stack.getRegion(), accountId, instanceId));
    }

    @Override
    public ResourceType getUserdataSecretResourceType() {
        return ResourceType.AWS_SECRETSMANAGER_SECRET;
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
