package com.sequenceiq.freeipa.service;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.cloudera.thunderhead.service.usermanagement.UserManagementProto;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.altus.model.AltusCredential;
import com.sequenceiq.cloudbreak.auth.altus.service.AltusIAMService;
import com.sequenceiq.cloudbreak.auth.crn.Crn;
import com.sequenceiq.cloudbreak.auth.crn.RegionAwareInternalCrnGeneratorFactory;
import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.common.api.telemetry.model.DataBusCredential;
import com.sequenceiq.common.api.telemetry.model.MonitoringCredential;
import com.sequenceiq.common.api.telemetry.model.Telemetry;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.service.stack.StackService;

@Service
public class AltusMachineUserService {

    private static final Logger LOGGER = LoggerFactory.getLogger(AltusMachineUserService.class);

    private static final String FREEIPA_FLUENT_DATABUS_MACHINE_USER_PATTERN = "freeipa-fluent-databus-uploader-%s";

    private static final String FREEIPA_MONITORING_MACHINE_USER_PATTERN = "freeipa-monitoring-%s";

    private final AltusIAMService altusIAMService;

    private final StackService stackService;

    private final RegionAwareInternalCrnGeneratorFactory regionAwareInternalCrnGeneratorFactory;

    public AltusMachineUserService(AltusIAMService altusIAMService, StackService stackService,
            RegionAwareInternalCrnGeneratorFactory regionAwareInternalCrnGeneratorFactory) {
        this.altusIAMService = altusIAMService;
        this.stackService = stackService;
        this.regionAwareInternalCrnGeneratorFactory = regionAwareInternalCrnGeneratorFactory;
    }

    public Optional<AltusCredential> createDatabusMachineUserWithAccessKeys(Stack stack, Telemetry telemetry) {
        String machineUserName = getFluentMachineUser(stack);
        return ThreadBasedUserCrnProvider.doAsInternalActor(
                regionAwareInternalCrnGeneratorFactory.iam().getInternalCrnForServiceAsString(),
                () -> altusIAMService.generateDatabusMachineUserWithAccessKey(machineUserName,
                        ThreadBasedUserCrnProvider.getUserCrn(),
                        Crn.fromString(stack.getResourceCrn()).getAccountId(),
                        telemetry.isUseSharedAltusCredentialEnabled()));
    }

    public Optional<AltusCredential> createMonitoringMachineUserWithAccessKeys(Stack stack, Telemetry telemetry) {
        String machineUserName = getMonitoringMachineUser(stack);
        return ThreadBasedUserCrnProvider.doAsInternalActor(
                regionAwareInternalCrnGeneratorFactory.iam().getInternalCrnForServiceAsString(),
                () -> altusIAMService.generateMonitoringMachineUserWithAccessKey(machineUserName,
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

    public void cleanupMonitoringMachineUser(Stack stack) {
        String machineUserName = getMonitoringMachineUser(stack);
        ThreadBasedUserCrnProvider.doAsInternalActor(
                regionAwareInternalCrnGeneratorFactory.iam().getInternalCrnForServiceAsString(),
                () -> altusIAMService.clearMachineUser(machineUserName,
                        ThreadBasedUserCrnProvider.getUserCrn(),
                        Crn.fromString(stack.getResourceCrn()).getAccountId()));
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

    public String getMonitoringMachineUser(Stack stack) {
        return String.format(FREEIPA_MONITORING_MACHINE_USER_PATTERN,
                Crn.fromString(stack.getResourceCrn()).getResource());
    }

    /**
     * Gather or create DataBus credential for a stack.
     * On creation it will generate a new workload user with new access/private keys.
     *
     * @param stackId id of the stack
     * @return databus credential holder
     */
    public DataBusCredential getOrCreateDataBusCredentialIfNeeded(Long stackId) throws IOException {
        return getOrCreateDataBusCredentialIfNeeded(stackService.getStackById(stackId));
    }

    /**
     * Gather or create DataBus credential for a stack.
     * On creation it will generate a new workload user with new access/private keys.
     *
     * @param stack stack object that holds details about the cluster
     * @return databus credential holder
     */
    public DataBusCredential getOrCreateDataBusCredentialIfNeeded(Stack stack) throws IOException {
        LOGGER.debug("Get or create databus credential for stack");
        Telemetry telemetry = stack.getTelemetry();
        if (stack.getDatabusCredential() != null) {
            LOGGER.debug("Databus credential has been found for the stack");
            DataBusCredential dataBusCredential = new Json(stack.getDatabusCredential()).get(DataBusCredential.class);
            if (isCredentialExists(telemetry, dataBusCredential.getMachineUserName(), dataBusCredential.getAccessKey(), stack)) {
                LOGGER.debug("Databus credential exists both in the stack and on UMS side");
                return dataBusCredential;
            } else {
                LOGGER.debug("Databus credential exists on stack side but does not exists on UMS side, it will be updated ...");
            }
        } else {
            LOGGER.debug("Databus credential does not exist for the stack, it will be created ...");
        }
        Optional<AltusCredential> altusCredential = createDatabusMachineUserWithAccessKeys(stack, telemetry);
        return storeDataBusCredential(altusCredential, stack);
    }

    public Optional<MonitoringCredential> getOrCreateMonitoringCredentialIfNeeded(Stack stack) throws IOException {
        LOGGER.debug("Get or create databus credential for stack");
        Telemetry telemetry = stack.getTelemetry();
        Optional<MonitoringCredential> monitoringCredential = getMonitoringCredentialIfExists(stack, telemetry);
        if (monitoringCredential.isEmpty()) {
            monitoringCredential = createMonitoringMachineUserWithAccessKeys(stack, telemetry)
                    .map(altusCredential -> storeMonitoringCredential(altusCredential, stack));
        }
        return monitoringCredential;
    }

    private Optional<MonitoringCredential> getMonitoringCredentialIfExists(Stack stack, Telemetry telemetry) throws IOException {
        Optional<MonitoringCredential> result = Optional.empty();
        if (stack.getMonitoringCredential() != null) {
            LOGGER.debug("Monitoring credential has been found for the stack");
            MonitoringCredential monitoringCredential = new Json(stack.getMonitoringCredential()).get(MonitoringCredential.class);
            if (isCredentialExists(telemetry, monitoringCredential.getMachineUserName(), monitoringCredential.getAccessKey(), stack)) {
                LOGGER.debug("Monitoring credential exists both in the stack and on UMS side");
                result = Optional.of(monitoringCredential);
            } else {
                LOGGER.debug("Monitoring credential exists on stack side but does not exists on UMS side, it will be updated ...");
            }
        } else {
            LOGGER.debug("Monitoring credential does not exist for the stack, it will be created ...");
        }
        return result;
    }

    /**
     * Store databus access / secret keypair and machine user name in the cluster if altus credential exists
     *
     * @param altusCredential dto for databus access/private key
     * @param stack           component will be attached to this stack
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

    private MonitoringCredential storeMonitoringCredential(AltusCredential altusCredential, Stack stack) {
        MonitoringCredential monitoringCredential = new MonitoringCredential();
        monitoringCredential.setMachineUserName(getMonitoringMachineUser(stack));
        monitoringCredential.setAccessKey(altusCredential.getAccessKey());
        monitoringCredential.setPrivateKey(altusCredential.getPrivateKey() != null ? new String(altusCredential.getPrivateKey()) : null);
        String monitoringCredentialJsonString = new Json(monitoringCredential).getValue();
        stack.setMonitoringCredential(monitoringCredentialJsonString);
        stackService.save(stack);
        return monitoringCredential;
    }

    public boolean isCredentialExists(Telemetry telemetry, String machineUser, String accessKey, Stack stack) {
        return ThreadBasedUserCrnProvider.doAsInternalActor(
                regionAwareInternalCrnGeneratorFactory.iam().getInternalCrnForServiceAsString(),
                () -> altusIAMService.doesMachineUserHasAccessKey(
                        ThreadBasedUserCrnProvider.getUserCrn(),
                        Crn.fromString(stack.getResourceCrn()).getAccountId(),
                        machineUser, accessKey,
                        telemetry.isUseSharedAltusCredentialEnabled()));
    }

}