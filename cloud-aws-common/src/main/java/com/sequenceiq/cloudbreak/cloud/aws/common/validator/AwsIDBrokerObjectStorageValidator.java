package com.sequenceiq.cloudbreak.cloud.aws.common.validator;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.google.common.base.Strings;
import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.cloud.aws.common.client.AmazonIdentityManagementClient;
import com.sequenceiq.cloudbreak.cloud.aws.common.util.AwsIamService;
import com.sequenceiq.cloudbreak.cloud.model.BackupOperationType;
import com.sequenceiq.cloudbreak.cloud.model.SpiFileSystem;
import com.sequenceiq.cloudbreak.cloud.model.filesystem.CloudFileSystemView;
import com.sequenceiq.cloudbreak.cloud.model.filesystem.CloudS3View;
import com.sequenceiq.cloudbreak.cloud.model.objectstorage.ObjectStorageValidateRequest;
import com.sequenceiq.cloudbreak.validation.ValidationResult;
import com.sequenceiq.cloudbreak.validation.ValidationResult.ValidationResultBuilder;
import com.sequenceiq.common.api.cloudstorage.AccountMappingBase;
import com.sequenceiq.common.model.CloudIdentityType;

import software.amazon.awssdk.services.iam.model.InstanceProfile;
import software.amazon.awssdk.services.iam.model.Role;

@Component
public class AwsIDBrokerObjectStorageValidator {

    private static final Logger LOGGER = LoggerFactory.getLogger(AwsIDBrokerObjectStorageValidator.class);

    @Inject
    private AwsInstanceProfileEC2TrustValidator awsInstanceProfileEC2TrustValidator;

    @Inject
    private AwsIDBrokerAssumeRoleValidator awsIDBrokerAssumeRoleValidator;

    @Inject
    private AwsDataAccessRolePermissionValidator awsDataAccessRolePermissionValidator;

    @Inject
    private AwsRangerAuditRolePermissionValidator awsRangerAuditRolePermissionValidator;

    @Inject
    private AwsIamService awsIamService;

    @Inject
    private AwsLogRolePermissionValidator awsLogRolePermissionValidator;

    @Inject
    private EntitlementService entitlementService;

    public ValidationResult validateObjectStorage(ObjectStorageValidateRequest validationRequest,
            AmazonIdentityManagementClient iam,
            SpiFileSystem spiFileSystem,
            String accountId,
            ValidationResultBuilder resultBuilder) {
        List<CloudFileSystemView> cloudFileSystems = spiFileSystem.getCloudFileSystems();
        for (CloudFileSystemView cloudFileSystemView : cloudFileSystems) {
            CloudS3View cloudFileSystem = (CloudS3View) cloudFileSystemView;
            String instanceProfileArn = cloudFileSystem.getInstanceProfile();
            InstanceProfile instanceProfile = awsIamService.getInstanceProfile(iam, instanceProfileArn,
                    cloudFileSystem.getCloudIdentityType(), resultBuilder);
            if (instanceProfile != null) {
                CloudIdentityType cloudIdentityType = cloudFileSystem.getCloudIdentityType();
                boolean skipOrgPolicyDecisions = validationRequest.getCredential().getCredentialSettings().isSkipOrgPolicyDecisions();
                if (CloudIdentityType.ID_BROKER.equals(cloudIdentityType)) {
                    validateIDBroker(validationRequest, iam, instanceProfile, cloudFileSystem, accountId, skipOrgPolicyDecisions, resultBuilder);
                } else if (CloudIdentityType.LOG.equals(cloudIdentityType)) {
                    validateLocation(validationRequest, iam, instanceProfile, cloudFileSystem, accountId, skipOrgPolicyDecisions, resultBuilder);
                }
            }
        }
        return resultBuilder.build();
    }

    private void validateIDBroker(ObjectStorageValidateRequest validationRequest, AmazonIdentityManagementClient iam, InstanceProfile instanceProfile,
            CloudS3View cloudFileSystem, String accountId, boolean skipOrgPolicyDecisions, ValidationResultBuilder resultBuilder) {
        LOGGER.info("Permission validation on IBBroker role: {} for operation: {}",
                instanceProfile.instanceProfileName(), validationRequest.getBackupOperationType().name());
        awsInstanceProfileEC2TrustValidator.isTrusted(instanceProfile, cloudFileSystem.getCloudIdentityType(), resultBuilder);
        Set<Role> allMappedRoles = getAllMappedRoles(iam, cloudFileSystem, resultBuilder);
        awsIDBrokerAssumeRoleValidator.canAssumeRoles(iam, instanceProfile, allMappedRoles, skipOrgPolicyDecisions, resultBuilder);
        awsDataAccessRolePermissionValidator.validate(iam, cloudFileSystem, validationRequest.getBackupLocationBase(),
                accountId, validationRequest.getBackupOperationType(), resultBuilder, skipOrgPolicyDecisions);
        awsRangerAuditRolePermissionValidator.validate(iam, cloudFileSystem, validationRequest.getBackupLocationBase(),
                accountId, validationRequest.getBackupOperationType(), resultBuilder, skipOrgPolicyDecisions);
    }

    private void validateLocation(ObjectStorageValidateRequest validationRequest, AmazonIdentityManagementClient iam,
            InstanceProfile instanceProfile, CloudS3View cloudFileSystem, String accountId, boolean skipOrgPolicyDecisions,
            ValidationResultBuilder resultBuilder) {
        LOGGER.info("Permission validation on Role: {} for operation: {}",
                instanceProfile.instanceProfileName(), validationRequest.getBackupOperationType().name());
        awsInstanceProfileEC2TrustValidator.isTrusted(instanceProfile, cloudFileSystem.getCloudIdentityType(), resultBuilder);
        awsLogRolePermissionValidator.validateLog(iam, instanceProfile, cloudFileSystem, validationRequest.getLogsLocationBase(), skipOrgPolicyDecisions,
                resultBuilder);
        if (entitlementService.isDatalakeBackupRestorePrechecksEnabled(accountId) &&
                BackupOperationType.isRestore(validationRequest.getBackupOperationType())) {
            LOGGER.info("Permission validation on backup location: {}", validationRequest.getBackupLocationBase());
            awsLogRolePermissionValidator.validateBackup(iam, instanceProfile, cloudFileSystem, backupLocationBase(validationRequest),
                    skipOrgPolicyDecisions, resultBuilder);
        }
    }

    private static String backupLocationBase(ObjectStorageValidateRequest validationRequest) {
        return Strings.isNullOrEmpty(validationRequest.getBackupLocationBase()) ? validationRequest.getLogsLocationBase()
                : validationRequest.getBackupLocationBase();
    }

    private Set<Role> getAllMappedRoles(AmazonIdentityManagementClient iam, CloudFileSystemView cloudFileSystemView,
            ValidationResultBuilder resultBuilder) {
        Set<Role> roles = Collections.emptySet();
        AccountMappingBase accountMappings = cloudFileSystemView.getAccountMapping();
        if (accountMappings != null) {
            SortedSet<String> roleArns = new TreeSet<>();
            roleArns.addAll(accountMappings.getUserMappings().values());
            roleArns.addAll(accountMappings.getGroupMappings().values());
            roles = awsIamService.getValidRoles(iam, roleArns, resultBuilder);
        }
        return roles;
    }
}
