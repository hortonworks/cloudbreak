package com.sequenceiq.freeipa.service;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.cloudera.thunderhead.service.usermanagement.UserManagementProto;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.altus.Crn;
import com.sequenceiq.cloudbreak.auth.altus.model.AltusCredential;
import com.sequenceiq.cloudbreak.auth.altus.service.AltusIAMService;
import com.sequenceiq.common.api.telemetry.model.Telemetry;
import com.sequenceiq.freeipa.entity.Stack;

@Service
public class AltusMachineUserService {

    private static final String FREEIPA_FLUENT_DATABUS_MACHINE_USER_PATTERN = "freeipa-fluent-databus-uploader-%s";

    private final AltusIAMService altusIAMService;

    public AltusMachineUserService(AltusIAMService altusIAMService) {
        this.altusIAMService = altusIAMService;
    }

    public Optional<AltusCredential> createMachineUserWithAccessKeys(Stack stack, Telemetry telemetry) {
        String machineUserName = getFluentMachineUser(stack);
        return ThreadBasedUserCrnProvider.doAsInternalActor(
                () -> altusIAMService.generateMachineUserWithAccessKey(machineUserName,
                        ThreadBasedUserCrnProvider.getUserCrn(),
                        Crn.fromString(stack.getResourceCrn()).getAccountId(),
                        telemetry.isUseSharedAltusCredentialEnabled()));
    }

    public void cleanupMachineUser(String machineUserName, String accountId) {
        ThreadBasedUserCrnProvider.doAsInternalActor(
                () -> altusIAMService.clearMachineUser(machineUserName,
                        ThreadBasedUserCrnProvider.getUserCrn(),
                        accountId
                )
        );
    }

    public void cleanupMachineUser(Stack stack, Telemetry telemetry) {
        String machineUserName = getFluentMachineUser(stack);
        ThreadBasedUserCrnProvider.doAsInternalActor(
                () -> altusIAMService.clearMachineUser(machineUserName,
                        ThreadBasedUserCrnProvider.getUserCrn(),
                        Crn.fromString(stack.getResourceCrn()).getAccountId(),
                        telemetry.isUseSharedAltusCredentialEnabled()));
    }

    public List<UserManagementProto.MachineUser> getAllInternalMachineUsers(String accountId) {
        return ThreadBasedUserCrnProvider.doAsInternalActor(
                () -> altusIAMService.getAllMachineUsersForAccount(
                        ThreadBasedUserCrnProvider.getUserCrn(),
                        accountId)
        );
    }

    public String getFluentMachineUser(Stack stack) {
        return String.format(FREEIPA_FLUENT_DATABUS_MACHINE_USER_PATTERN,
                Crn.fromString(stack.getResourceCrn()).getResource());
    }

}