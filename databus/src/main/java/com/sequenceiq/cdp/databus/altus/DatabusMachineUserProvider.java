package com.sequenceiq.cdp.databus.altus;

import java.util.Map;
import java.util.Optional;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.altus.model.AltusCredential;
import com.sequenceiq.cloudbreak.auth.altus.service.AltusIAMService;
import com.sequenceiq.common.api.telemetry.model.DataBusCredential;

@Component
public class DatabusMachineUserProvider {

    private final AltusIAMService altusIAMService;

    public DatabusMachineUserProvider(AltusIAMService altusIAMService) {
        this.altusIAMService = altusIAMService;
    }

    public Optional<AltusCredential> getOrCreateMachineUserAndAccessKey(String machineUserName, String accountId) {
        return ThreadBasedUserCrnProvider.doAsInternalActor(() -> altusIAMService.generateMachineUserWithAccessKey(
                machineUserName,
                ThreadBasedUserCrnProvider.getUserCrn(),
                accountId,
                true));
    }

    public boolean isDataBusCredentialStillExist(String accountId, DataBusCredential dataBusCredential) {
        return ThreadBasedUserCrnProvider.doAsInternalActor(
                () -> altusIAMService.doesMachineUserHasAccessKey(
                        ThreadBasedUserCrnProvider.getUserCrn(),
                        accountId,
                        dataBusCredential.getMachineUserName(), dataBusCredential.getAccessKey(),
                        true));
    }

    public Map<String, Long> getAccessKeyUsageMapForMachineUser(String machineUserName, String accountId) {
        return ThreadBasedUserCrnProvider.doAsInternalActor(
                () -> altusIAMService.getAccessKeyUsageMapForMachineUser(ThreadBasedUserCrnProvider.getUserCrn(),
                        accountId, machineUserName, true));
    }

    public void deleteAccessKeyForMachineUser(String accessKeyId, String accountId) {
        ThreadBasedUserCrnProvider.doAsInternalActor(
                () -> altusIAMService.deleteAccessKey(ThreadBasedUserCrnProvider.getUserCrn(), accountId, accessKeyId, true));
    }
}
