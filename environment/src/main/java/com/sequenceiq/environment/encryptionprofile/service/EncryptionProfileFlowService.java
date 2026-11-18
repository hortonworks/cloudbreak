package com.sequenceiq.environment.encryptionprofile.service;

import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.api.endpoint.v4.dto.NameOrCrn;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.crn.Crn;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.environment.encryptionprofile.domain.EncryptionProfile;
import com.sequenceiq.environment.environment.EnvironmentStatus;
import com.sequenceiq.environment.environment.dto.EnvironmentDto;
import com.sequenceiq.environment.environment.flow.EnvironmentReactorFlowManager;
import com.sequenceiq.environment.environment.flow.encryptionprofile.validator.EncryptionProfileValidator;
import com.sequenceiq.environment.environment.service.EnvironmentService;
import com.sequenceiq.flow.api.model.FlowIdentifier;

@Service
public class EncryptionProfileFlowService {

    private final EnvironmentService environmentService;

    private final EncryptionProfileService encryptionProfileService;

    private final EnvironmentReactorFlowManager reactorFlowManager;

    private final EncryptionProfileValidator encryptionProfileValidator;

    public EncryptionProfileFlowService(EnvironmentService environmentService, EncryptionProfileService encryptionProfileService,
            EnvironmentReactorFlowManager reactorFlowManager, EncryptionProfileValidator encryptionProfileValidator) {
        this.environmentService = environmentService;
        this.encryptionProfileService = encryptionProfileService;
        this.reactorFlowManager = reactorFlowManager;
        this.encryptionProfileValidator = encryptionProfileValidator;
    }

    public FlowIdentifier enableEncryptionProfileByName(NameOrCrn envNameOrCrn, String encryptionProfileNameOrCrn) {
        EnvironmentDto environmentDto = getEnvironmentByNameOrCrn(envNameOrCrn);
        EncryptionProfile encryptionProfile = getEncryptionProfileByNameOrCrn(encryptionProfileNameOrCrn);
        return enableEncryptionProfile(environmentDto, encryptionProfile);
    }

    public FlowIdentifier enableEncryptionProfileByCrn(NameOrCrn envNameOrCrn, String encryptionProfileNameOrCrn) {
        EnvironmentDto environmentDto = getEnvironmentByNameOrCrn(envNameOrCrn);
        EncryptionProfile encryptionProfile = getEncryptionProfileByNameOrCrn(encryptionProfileNameOrCrn);
        return enableEncryptionProfile(environmentDto, encryptionProfile);
    }

    private FlowIdentifier enableEncryptionProfile(EnvironmentDto environmentDto, EncryptionProfile encryptionProfile) {
        if (!environmentDto.getStatus().isEncryptionProfileEnableAllowed()) {
            throw new BadRequestException(String.format("Environment '%s' needs to be in one of the following states to enable encryption profile: %s. "
                    + "Current status: %s", environmentDto.getName(), allowedStatesText(EnvironmentStatus.ENCRYPTION_PROFILE_ENABLEABLE_STATES),
                    environmentDto.getStatus()));
        }
        encryptionProfileValidator.validate(environmentDto.getResourceCrn());
        return reactorFlowManager.triggerEnableEncryptionProfile(environmentDto.getId(), environmentDto.getName(), environmentDto.getResourceCrn(),
                encryptionProfile.getResourceCrn());
    }

    public FlowIdentifier disableEncryptionProfile(NameOrCrn envNameOrCrn) {
        encryptionProfileValidator.validateDisable();
        EnvironmentDto environmentDto = getEnvironmentByNameOrCrn(envNameOrCrn);
        if (!environmentDto.getStatus().isEncryptionProfileDisableAllowed()) {
            throw new BadRequestException(String.format("Environment '%s' needs to be in one of the following states to disable encryption profile: %s. "
                    + "Current status: %s", environmentDto.getName(), allowedStatesText(EnvironmentStatus.ENCRYPTION_PROFILE_DISABLEABLE_STATES),
                    environmentDto.getStatus()));
        }
        return reactorFlowManager.triggerDisableEncryptionProfile(environmentDto.getId(), environmentDto.getName(), environmentDto.getResourceCrn());
    }

    private static String allowedStatesText(Set<EnvironmentStatus> states) {
        return states.stream().map(Enum::name).sorted().collect(Collectors.joining(", "));
    }

    private EnvironmentDto getEnvironmentByNameOrCrn(NameOrCrn nameOrCrn) {
        if (nameOrCrn.hasCrn()) {
            return environmentService.getByCrnAndAccountId(nameOrCrn.getCrn(), ThreadBasedUserCrnProvider.getAccountId());
        } else {
            return environmentService.getByNameAndAccountId(nameOrCrn.getName(), ThreadBasedUserCrnProvider.getAccountId());
        }
    }

    private EncryptionProfile getEncryptionProfileByNameOrCrn(String nameOrCrn) {
        String accountId = ThreadBasedUserCrnProvider.getAccountId();
        if (Crn.isCrn(nameOrCrn)) {
            return encryptionProfileService.getByCrn(nameOrCrn, accountId);
        } else {
            return encryptionProfileService.getByNameAndAccountId(nameOrCrn, accountId);
        }
    }
}
