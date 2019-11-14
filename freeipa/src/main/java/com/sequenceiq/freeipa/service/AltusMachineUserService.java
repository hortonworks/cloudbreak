package com.sequenceiq.freeipa.service;

import java.util.Optional;

import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.auth.altus.Crn;
import com.sequenceiq.cloudbreak.auth.altus.model.AltusCredential;
import com.sequenceiq.cloudbreak.auth.altus.service.AltusIAMService;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.util.CrnService;

@Service
public class AltusMachineUserService {

    private static final String FREEIPA_FLUENT_DATABUS_MACHINE_USER_PATTERN = "freeipa-fluent-databus-uploader-%s";

    private final CrnService crnService;

    private final AltusIAMService altusIAMService;

    public AltusMachineUserService(AltusIAMService altusIAMService, CrnService crnService) {
        this.altusIAMService = altusIAMService;
        this.crnService = crnService;
    }

    public Optional<AltusCredential> createMachineUserWithAccessKeys(Stack stack) {
        String userCrn = crnService.getUserCrn();
        String machineUserName = getFluentMachineUser(stack);
        return altusIAMService.generateMachineUserWithAccessKey(machineUserName, userCrn);
    }

    public void cleanupMachineUser(Stack stack) {
        String userCrn = crnService.getUserCrn();
        String machineUserName = getFluentMachineUser(stack);
        altusIAMService.clearMachineUser(machineUserName, userCrn);
    }

    private String getFluentMachineUser(Stack stack) {
        return String.format(FREEIPA_FLUENT_DATABUS_MACHINE_USER_PATTERN,
                Crn.fromString(stack.getResourceCrn()).getResource());
    }

}