package com.sequenceiq.environment.encryptionprofile.service;

import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.crn.Crn;
import com.sequenceiq.environment.encryptionprofile.domain.EncryptionProfile;
import com.sequenceiq.environment.environment.dto.EnvironmentDto;
import com.sequenceiq.environment.environment.flow.EnvironmentReactorFlowManager;
import com.sequenceiq.environment.environment.service.EnvironmentService;
import com.sequenceiq.flow.api.model.FlowIdentifier;

@Service
public class EncryptionProfileFlowService {

    private final EnvironmentService environmentService;

    private final EncryptionProfileService encryptionProfileService;

    private final EnvironmentReactorFlowManager reactorFlowManager;

    public EncryptionProfileFlowService(EnvironmentService environmentService, EncryptionProfileService encryptionProfileService,
            EnvironmentReactorFlowManager reactorFlowManager) {
        this.environmentService = environmentService;
        this.encryptionProfileService = encryptionProfileService;
        this.reactorFlowManager = reactorFlowManager;
    }

    public FlowIdentifier enableEncryptionProfileByName(String envNameOrCrn, String encryptionProfileName) {
        EnvironmentDto environmentDto = getEnvironment(envNameOrCrn);
        EncryptionProfile encryptionProfile = encryptionProfileService.getByNameAndAccountId(encryptionProfileName, ThreadBasedUserCrnProvider.getAccountId());
        return enableEncryptionProfile(environmentDto, encryptionProfile);
    }

    public FlowIdentifier enableEncryptionProfileByCrn(String envNameOrCrn, String encryptionProfileCrn) {
        EnvironmentDto environmentDto = getEnvironment(envNameOrCrn);
        EncryptionProfile encryptionProfile = encryptionProfileService.getByCrn(encryptionProfileCrn);
        return enableEncryptionProfile(environmentDto, encryptionProfile);
    }

    private FlowIdentifier enableEncryptionProfile(EnvironmentDto environmentDto, EncryptionProfile encryptionProfile) {
        return reactorFlowManager.triggerEnableEncryptionProfile(environmentDto.getId(), environmentDto.getName(), environmentDto.getResourceCrn(),
                encryptionProfile.getResourceCrn());
    }

    private EnvironmentDto getEnvironment(String envNameOrCrn) {
        if (Crn.isCrn(envNameOrCrn)) {
            return environmentService.getByCrnAndAccountId(envNameOrCrn, ThreadBasedUserCrnProvider.getAccountId());
        } else {
            return environmentService.getByNameAndAccountId(envNameOrCrn, ThreadBasedUserCrnProvider.getAccountId());
        }
    }
}
