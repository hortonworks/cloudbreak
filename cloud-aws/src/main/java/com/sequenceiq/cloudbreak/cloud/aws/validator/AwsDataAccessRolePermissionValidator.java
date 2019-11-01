package com.sequenceiq.cloudbreak.cloud.aws.validator;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.service.identitymapping.AccountMappingSubject;
import com.sequenceiq.common.api.cloudstorage.StorageLocationBase;
import com.sequenceiq.common.model.FileSystemType;

@Component
public class AwsDataAccessRolePermissionValidator extends AwsIDBrokerMappedRolePermissionValidator {
    @Override
    Set<String> getUsers() {
        return AccountMappingSubject.DATA_ACCESS_USERS;
    }

    @Override
    List<String> getPolicyFileNames(boolean s3guardEnabled) {
        List<String> policyFileNames = new ArrayList<>();
        policyFileNames.add("aws-cdp-bucket-access-policy.json");
        policyFileNames.add("aws-cdp-datalake-admin-s3-policy.json");
        if (s3guardEnabled) {
            policyFileNames.add("aws-cdp-dynamodb-policy.json");
        }

        return policyFileNames;
    }

    @Override
    String getStorageLocationBase(StorageLocationBase location) {
        return location.getValue().replace(FileSystemType.S3.getProtocol() + "://", "");
    }

    @Override
    boolean checkLocation(StorageLocationBase location) {
        return true;
    }
}
