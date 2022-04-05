package com.sequenceiq.freeipa.service;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.cloudera.thunderhead.service.usermanagement.UserManagementProto;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.crn.Crn;
import com.sequenceiq.cloudbreak.auth.altus.model.AltusCredential;
import com.sequenceiq.cloudbreak.auth.altus.service.AltusIAMService;
import com.sequenceiq.cloudbreak.auth.crn.RegionAwareInternalCrnGeneratorFactory;
import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.common.api.telemetry.model.DataBusCredential;
import com.sequenceiq.common.api.telemetry.model.Telemetry;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.service.stack.StackService;

@Service
public class AltusMachineUserService {

    private static final Logger LOGGER = LoggerFactory.getLogger(AltusMachineUserService.class);

    private static final String FREEIPA_FLUENT_DATABUS_MACHINE_USER_PATTERN = "freeipa-fluent-databus-uploader-%s";

    private final AltusIAMService altusIAMService;

    private final StackService stackService;

    private final RegionAwareInternalCrnGeneratorFactory regionAwareInternalCrnGeneratorFactory;

    public AltusMachineUserService(AltusIAMService altusIAMService, StackService stackService,
        RegionAwareInternalCrnGeneratorFactory regionAwareInternalCrnGeneratorFactory) {
        this.altusIAMService = altusIAMService;
        this.stackService = stackService;
        this.regionAwareInternalCrnGeneratorFactory = regionAwareInternalCrnGeneratorFactory;
    }

    public Optional<AltusCredential> createMachineUserWithAccessKeys(Stack stack, Telemetry telemetry) {
        String machineUserName = getFluentMachineUser(stack);
        return ThreadBasedUserCrnProvider.doAsInternalActor(
                regionAwareInternalCrnGeneratorFactory.iam().getInternalCrnForServiceAsString(),
                () -> altusIAMService.generateMachineUserWithAccessKey(machineUserName,
                        ThreadBasedUserCrnProvider.getUserCrn(),
                        Crn.fromString(stack.getResourceCrn()).getAccountId(),
                        telemetry.isUseSharedAltusCredentialEnabled()));
    }

    public void cleanupMachineUser(String machineUserName, String accountId) {
        ThreadBasedUserCrnProvider.doAsInternalActor(
                regionAwareInternalCrnGeneratorFactory.iam().getInternalCrnForServiceAsString(),
                () -> altusIAMService.clearMachineUser(machineUserName,
                        ThreadBasedUserCrnProvider.getUserCrn(),
                        accountId
                )
        );
    }

    public void cleanupMachineUser(Stack stack, Telemetry telemetry) {
        String machineUserName = getFluentMachineUser(stack);
        ThreadBasedUserCrnProvider.doAsInternalActor(
                regionAwareInternalCrnGeneratorFactory.iam().getInternalCrnForServiceAsString(),
                () -> altusIAMService.clearMachineUser(machineUserName,
                        ThreadBasedUserCrnProvider.getUserCrn(),
                        Crn.fromString(stack.getResourceCrn()).getAccountId(),
                        telemetry.isUseSharedAltusCredentialEnabled()));
    }

    public List<UserManagementProto.MachineUser> getAllInternalMachineUsers(String accountId) {
        return ThreadBasedUserCrnProvider.doAsInternalActor(
                regionAwareInternalCrnGeneratorFactory.iam().getInternalCrnForServiceAsString(),
                () -> altusIAMService.getAllMachineUsersForAccount(accountId)
        );
    }

    public String getFluentMachineUser(Stack stack) {
        return String.format(FREEIPA_FLUENT_DATABUS_MACHINE_USER_PATTERN,
                Crn.fromString(stack.getResourceCrn()).getResource());
    }

    /**
     * Gather or create DataBus credential for a stack.
     * On creation it will generate aa new workload user with new access/private keys.
     * @param stackId id of the stack
     * @return databus credential holder
     */
    public DataBusCredential getOrCreateDataBusCredentialIfNeeded(Long stackId) throws IOException {
        return getOrCreateDataBusCredentialIfNeeded(stackService.getStackById(stackId));
    }

    /**
     * Gather or create DataBus credential for a stack.
     * On creation it will generate aa new workload user with new access/private keys.
     * @param stack stack object that holds details about the cluster
     * @return databus credential holder
     */
    public DataBusCredential getOrCreateDataBusCredentialIfNeeded(Stack stack) throws IOException {
        LOGGER.debug("Get or create databus credential for stack");
        Telemetry telemetry = stack.getTelemetry();
        if (stack.getDatabusCredential() != null) {
            LOGGER.debug("Databus credential has been found for the stack");
            DataBusCredential dataBusCredential = new Json(stack.getDatabusCredential()).get(DataBusCredential.class);
            if (isDataBusCredentialStillExist(telemetry, dataBusCredential, stack)) {
                LOGGER.debug("Databus credential exists both in the stack and on UMS side");
                return dataBusCredential;
            } else {
                LOGGER.debug("Databus credential exists on stack side but does not exists on UMS side, it will be updated ...");
            }
        } else {
            LOGGER.debug("Databus credential does not exist for the stack, it will be created ...");
        }
        Optional<AltusCredential> altusCredential = createMachineUserWithAccessKeys(stack, telemetry);
        return storeDataBusCredential(altusCredential, stack);
    }

    /**
     * Store databus access / secret keypair and machine user name in the cluster if altus credential exists
     * @param altusCredential dto for databus access/private key
     * @param stack component will be attached to this stack
     * @return domain object that holds databus credential
     */
    public DataBusCredential storeDataBusCredential(Optional<AltusCredential> altusCredential, Stack stack) {
        if (altusCredential.isPresent()) {
            DataBusCredential dataBusCredential = new DataBusCredential();
            dataBusCredential.setMachineUserName(getFluentMachineUser(stack));
            dataBusCredential.setAccessKey(altusCredential.get().getAccessKey());
            dataBusCredential.setPrivateKey(altusCredential.get().getPrivateKey() != null ? new String(altusCredential.get().getPrivateKey()) : null);
            String databusCredentialJsonString = new Json(dataBusCredential).getValue();
            stack.setDatabusCredential(databusCredentialJsonString);
            stackService.save(stack);
            return dataBusCredential;
        }
        return null;
    }

    /**
     * Check that machine user still have the access key on UMS side
     * @param dataBusCredential databus credential DTO that comes from cloudbreak database which contains access key and machune user name as well.
     * @param stack stack object holder that can be used to calculate the machine user name
     * @return check result - if true, no need to regenerate keys
     */
    public boolean isDataBusCredentialStillExist(Telemetry telemetry, DataBusCredential dataBusCredential, Stack stack) {
        return ThreadBasedUserCrnProvider.doAsInternalActor(
                regionAwareInternalCrnGeneratorFactory.iam().getInternalCrnForServiceAsString(),
                () -> altusIAMService.doesMachineUserHasAccessKey(
                        ThreadBasedUserCrnProvider.getUserCrn(),
                        Crn.fromString(stack.getResourceCrn()).getAccountId(),
                        dataBusCredential.getMachineUserName(), dataBusCredential.getAccessKey(),
                        telemetry.isUseSharedAltusCredentialEnabled()));
    }

}