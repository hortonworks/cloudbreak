package com.sequenceiq.cloudbreak.cloud.aws.common.validator;

import java.util.Set;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.service.identitymapping.AccountMappingSubject;

@Component
public class AwsRazRolePermissionValidator extends AwsDataAccessRolePermissionValidator {

    @Override
    Set<String> getUsers() {
        return Set.of(AccountMappingSubject.RANGER_RAZ_USER);
    }

    @Override
    String getRoleType() {
        return "Raz Role";
    }
}
