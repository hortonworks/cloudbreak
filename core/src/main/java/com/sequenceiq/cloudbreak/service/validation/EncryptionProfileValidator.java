package com.sequenceiq.cloudbreak.service.validation;

import static com.sequenceiq.cloudbreak.cmtemplate.CMRepositoryVersionUtil.CLOUDERA_STACK_VERSION_7_3_2;
import static com.sequenceiq.cloudbreak.cmtemplate.CMRepositoryVersionUtil.isVersionNewerOrEqualThanLimited;

import jakarta.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.dto.StackDto;

@Component
public class EncryptionProfileValidator {

    @Inject
    private EntitlementService entitlementService;

    public void validate(StackDto stackDto) {
        if (!entitlementService.isChangeEncryptionProfileEnabled(ThreadBasedUserCrnProvider.getAccountId())) {
            throw new CloudbreakServiceException("Account not entitled for encryption profile. Please contact your CDP administrator to enable it.");
        }
        if (!isVersionNewerOrEqualThanLimited(stackDto::getStackVersion, CLOUDERA_STACK_VERSION_7_3_2)) {
            throw new CloudbreakServiceException("Encryption profile feature requires runtime 7.3.2 or above");
        }
        if (!stackDto.getStatus().isAvailable()) {
            throw new CloudbreakServiceException("Cluster need to be in AVAILABLE state to enable encryption profile. Status: " + stackDto.getStatus());
        }
    }
}
