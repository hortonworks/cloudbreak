package com.sequenceiq.environment.encryptionprofile.service;

import jakarta.ws.rs.ForbiddenException;

import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.api.endpoint.v4.dto.NameOrCrn;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.environment.encryptionprofile.domain.EncryptionProfile;
import com.sequenceiq.environment.environment.dto.EnvironmentDto;
import com.sequenceiq.environment.environment.flow.EnvironmentReactorFlowManager;
import com.sequenceiq.environment.environment.service.EnvironmentService;
import com.sequenceiq.environment.environment.service.sdx.SdxService;
import com.sequenceiq.environment.environment.service.stack.StackService;
import com.sequenceiq.flow.api.model.FlowIdentifier;
import com.sequenceiq.sdx.api.model.SdxClusterResponse;

@Service
public class EncryptionProfileFlowService {

    private final EnvironmentService environmentService;

    private final EncryptionProfileService encryptionProfileService;

    private final EnvironmentReactorFlowManager reactorFlowManager;

    private final SdxService sdxService;

    private final StackService stackService;

    private final EntitlementService entitlementService;

    public EncryptionProfileFlowService(EnvironmentService environmentService, EncryptionProfileService encryptionProfileService,
            EnvironmentReactorFlowManager reactorFlowManager, SdxService sdxService, StackService stackService, EntitlementService entitlementService) {
        this.environmentService = environmentService;
        this.encryptionProfileService = encryptionProfileService;
        this.reactorFlowManager = reactorFlowManager;
        this.sdxService = sdxService;
        this.stackService = stackService;
        this.entitlementService = entitlementService;
    }

    public FlowIdentifier enableEncryptionProfileByName(NameOrCrn envNameOrCrn, String encryptionProfileName) {
        EnvironmentDto environmentDto = getEnvironment(envNameOrCrn);
        EncryptionProfile encryptionProfile = encryptionProfileService.getByNameAndAccountId(encryptionProfileName, ThreadBasedUserCrnProvider.getAccountId());
        return enableEncryptionProfile(environmentDto, encryptionProfile);
    }

    public FlowIdentifier enableEncryptionProfileByCrn(NameOrCrn envNameOrCrn, String encryptionProfileCrn) {
        EnvironmentDto environmentDto = getEnvironment(envNameOrCrn);
        EncryptionProfile encryptionProfile = encryptionProfileService.getByCrn(encryptionProfileCrn);
        return enableEncryptionProfile(environmentDto, encryptionProfile);
    }

    private FlowIdentifier enableEncryptionProfile(EnvironmentDto environmentDto, EncryptionProfile encryptionProfile) {
        if (!entitlementService.isChangeEncryptionProfileEnabled(ThreadBasedUserCrnProvider.getAccountId())) {
            throw new ForbiddenException("Enable encryption profile is not granted for the account");
        }
        return reactorFlowManager.triggerEnableEncryptionProfile(environmentDto.getId(), environmentDto.getName(), environmentDto.getResourceCrn(),
                encryptionProfile.getResourceCrn());
    }

    public FlowIdentifier disableEncryptionProfile(NameOrCrn envNameOrCrn) {
        if (!entitlementService.isChangeEncryptionProfileEnabled(ThreadBasedUserCrnProvider.getAccountId())) {
            throw new ForbiddenException("Disable encryption profile is not granted for the account");
        }
        EnvironmentDto environmentDto = getEnvironment(envNameOrCrn);
        SdxClusterResponse sdxCluster = sdxService.list(environmentDto.getName()).getFirst();
        environmentService.disableEncryptionProfile(environmentDto.getResourceCrn());
        return stackService.updatePillarConfigurationByCrn(sdxCluster.getCrn());
    }

    private EnvironmentDto getEnvironment(NameOrCrn nameOrCrn) {
        if (nameOrCrn.hasCrn()) {
            return environmentService.getByCrnAndAccountId(nameOrCrn.getCrn(), ThreadBasedUserCrnProvider.getAccountId());
        } else {
            return environmentService.getByNameAndAccountId(nameOrCrn.getName(), ThreadBasedUserCrnProvider.getAccountId());
        }
    }
}
