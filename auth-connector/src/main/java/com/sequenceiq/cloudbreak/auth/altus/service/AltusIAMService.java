package com.sequenceiq.cloudbreak.auth.altus.service;

import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.cloudera.thunderhead.service.usermanagement.UserManagementProto;
import com.sequenceiq.cloudbreak.auth.altus.GrpcUmsClient;
import com.sequenceiq.cloudbreak.auth.altus.model.AltusCredential;
import com.sequenceiq.common.api.telemetry.model.AnonymizationRule;

@Service
public class AltusIAMService {

    private static final Logger LOGGER = LoggerFactory.getLogger(AltusIAMService.class);

    private final GrpcUmsClient umsClient;

    private final SharedAltusCredentialProvider sharedAltusCredentialProvider;

    public AltusIAMService(GrpcUmsClient umsClient,
            SharedAltusCredentialProvider sharedAltusCredentialProvider) {
        this.umsClient = umsClient;
        this.sharedAltusCredentialProvider = sharedAltusCredentialProvider;
    }

    /**
     * Generate machine user with access keys
     */
    public Optional<AltusCredential> generateMachineUserWithAccessKey(String machineUserName, String actorCrn, String accountId, boolean useSharedCredential) {
        return Optional.ofNullable(sharedAltusCredentialProvider.getSharedCredentialIfConfigured(useSharedCredential)
                .orElse(umsClient.createMachineUserAndGenerateKeys(
                        machineUserName,
                        actorCrn,
                        accountId,
                        umsClient.getBuiltInDatabusRoleCrn(),
                        UserManagementProto.AccessKeyType.Value.ED25519)));
    }

    /**
     * Delete machine user with its access keys (and unassign databus role if required)
     */
    public void clearMachineUser(String machineUserName, String actorCrn, String accountId, boolean useSharedCredential) {
        try {
            if (sharedAltusCredentialProvider.isSharedAltusCredentialInUse(useSharedCredential)) {
                LOGGER.debug("Access and secret keys are set manually application wide for Databus, skip machine user cleanup.");
            } else {
                umsClient.clearMachineUserWithAccessKeysAndRole(machineUserName, actorCrn, accountId, umsClient.getBuiltInDatabusRoleCrn());
            }
        } catch (Exception e) {
            LOGGER.warn("Cluster Databus resource cleanup failed (fluent - databus user). It is not a fatal issue, "
                    + "but note that you could have remaining UMS resources for your account", e);
        }
    }

    public List<AnonymizationRule> getAnonymizationRules(String accountId, String actorCrn) {
        return umsClient.getAnonymizationRules(accountId, actorCrn);
    }

    public void clearMachineUser(String machineUserName, String actorCrn, String accountId) {
        clearMachineUser(machineUserName, actorCrn, accountId, false);
    }
}