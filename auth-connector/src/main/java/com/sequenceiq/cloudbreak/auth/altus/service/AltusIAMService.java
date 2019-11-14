package com.sequenceiq.cloudbreak.auth.altus.service;

import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.cloudera.thunderhead.service.usermanagement.UserManagementProto;
import com.sequenceiq.cloudbreak.auth.altus.GrpcUmsClient;
import com.sequenceiq.cloudbreak.auth.altus.model.AltusCredential;

@Service
public class AltusIAMService {

    private static final Logger LOGGER = LoggerFactory.getLogger(AltusIAMService.class);

    private final GrpcUmsClient umsClient;

    public AltusIAMService(GrpcUmsClient umsClient) {
        this.umsClient = umsClient;
    }

    /**
     * Generate machine user with access keys
     */
    public Optional<AltusCredential> generateMachineUserWithAccessKey(String machineUserName, String actorCrn) {
        return Optional.of(umsClient.createMachineUserAndGenerateKeys(
                machineUserName,
                actorCrn,
                umsClient.getBuiltInDatabusRoleCrn(),
                UserManagementProto.AccessKeyType.Value.ED25519));
    }

    /**
     * Delete machine user with its access keys (and unassign databus role if required)
     */
    public void clearMachineUser(String machineUserName, String actorCrn) {
        try {
            umsClient.clearMachineUserWithAccessKeysAndRole(machineUserName, actorCrn, umsClient.getBuiltInDatabusRoleCrn());
        } catch (Exception e) {
            LOGGER.warn("Cluster Databus resource cleanup failed (fluent - databus user). It is not a fatal issue, "
                    + "but note that you could have remaining UMS resources for your account", e);
        }
    }

}