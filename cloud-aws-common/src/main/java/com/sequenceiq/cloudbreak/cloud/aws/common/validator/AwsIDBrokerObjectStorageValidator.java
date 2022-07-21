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

import com.amazonaws.services.identitymanagement.model.InstanceProfile;
import com.amazonaws.services.identitymanagement.model.Role;
import com.google.common.base.Strings;
import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.cloud.aws.common.client.AmazonIdentityManagementClient;
import com.sequenceiq.cloudbreak.cloud.aws.common.util.AwsIamService;
import com.sequenceiq.cloudbreak.cloud.model.SpiFileSystem;
import com.sequenceiq.cloudbreak.cloud.model.filesystem.CloudFileSystemView;
import com.sequenceiq.cloudbreak.cloud.model.filesystem.CloudS3View;
import com.sequenceiq.cloudbreak.validation.ValidationResult;
import com.sequenceiq.cloudbreak.validation.ValidationResult.ValidationResultBuilder;
import com.sequenceiq.common.api.cloudstorage.AccountMappingBase;
import com.sequenceiq.common.model.CloudIdentityType;

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

    public ValidationResult validateObjectStorage(AmazonIdentityManagementClient iam,
            SpiFileSystem spiFileSystem,
            String logsLocationBase,
            String backupLocationBase,
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
                if (CloudIdentityType.ID_BROKER.equals(cloudIdentityType)) {
                    validateIDBroker(iam, instanceProfile, cloudFileSystem, backupLocationBase, accountId, resultBuilder);
                } else if (CloudIdentityType.LOG.equals(cloudIdentityType)) {
                    validateLocation(iam, instanceProfile, cloudFileSystem, logsLocationBase, backupLocationBase, accountId, resultBuilder);
                }
            }
        }
        return resultBuilder.build();
    }

    private void validateIDBroker(AmazonIdentityManagementClient iam, InstanceProfile instanceProfile,
            CloudS3View cloudFileSystem, String backupLocationBase, String accountId, ValidationResultBuilder resultBuilder) {
        LOGGER.info("Permission validation on IBBroker role: {}", instanceProfile.getInstanceProfileName());
        awsInstanceProfileEC2TrustValidator.isTrusted(instanceProfile, cloudFileSystem.getCloudIdentityType(), resultBuilder);
        Set<Role> allMappedRoles = getAllMappedRoles(iam, cloudFileSystem, resultBuilder);
        awsIDBrokerAssumeRoleValidator.canAssumeRoles(iam, instanceProfile, allMappedRoles, resultBuilder);
        awsDataAccessRolePermissionValidator.validate(iam, cloudFileSystem, backupLocationBase, accountId, resultBuilder);
        awsRangerAuditRolePermissionValidator.validate(iam, cloudFileSystem, backupLocationBase, accountId, resultBuilder);
    }

    private void validateLocation(AmazonIdentityManagementClient iam, InstanceProfile instanceProfile, CloudS3View cloudFileSystem,
            String logsLocationBase, String backupLocationBase, String accountId, ValidationResultBuilder resultBuilder) {
        LOGGER.info("Permission validation on Role: {}", instanceProfile.getInstanceProfileName());
        awsInstanceProfileEC2TrustValidator.isTrusted(instanceProfile, cloudFileSystem.getCloudIdentityType(), resultBuilder);
        awsLogRolePermissionValidator.validateLog(iam, instanceProfile, cloudFileSystem, logsLocationBase, resultBuilder);
        if (entitlementService.isDatalakeBackupRestorePrechecksEnabled(accountId)) {
            LOGGER.info("Permission validation on backup location: {}", backupLocationBase);
            awsLogRolePermissionValidator.validateBackup(iam, instanceProfile, cloudFileSystem,
                    Strings.isNullOrEmpty(backupLocationBase) ? logsLocationBase : backupLocationBase,
                    resultBuilder);
        }
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
