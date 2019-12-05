package com.sequenceiq.cloudbreak.auth.altus.service;

import java.util.Optional;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.cloudera.thunderhead.service.usermanagement.UserManagementProto;
import com.sequenceiq.cloudbreak.altus.AltusDatabusConfiguration;
import com.sequenceiq.cloudbreak.auth.altus.GrpcUmsClient;
import com.sequenceiq.cloudbreak.auth.altus.model.AltusCredential;

@Service
public class AltusIAMService {

    private static final Logger LOGGER = LoggerFactory.getLogger(AltusIAMService.class);

    private final boolean useSharedAltusCredential;

    private final String sharedAltusAccessKey;

    private final char[] sharedAltusSecretKey;

    private final GrpcUmsClient umsClient;

    public AltusIAMService(GrpcUmsClient umsClient,
            AltusDatabusConfiguration altusDatabusConfiguration) {
        this.umsClient = umsClient;
        this.useSharedAltusCredential = altusDatabusConfiguration.isUseSharedAltusCredential();
        this.sharedAltusAccessKey = altusDatabusConfiguration.getSharedAccessKey();
        this.sharedAltusSecretKey = altusDatabusConfiguration.getSharedSecretKey();
    }

    /**
     * Generate machine user with access keys
     */
    public Optional<AltusCredential> generateMachineUserWithAccessKey(String machineUserName, String actorCrn, boolean useSharedCredential) {
        if (useSharedAltusCredential) {
            LOGGER.debug("Use shared altus credential is turned on for generating altus credential and access keys.");
            if (areDatabusCredentialsFilled(useSharedCredential)) {
                LOGGER.debug("Access and secret keys are set manually application wide for Databus, skip machine user and access key generation");
                return Optional.of(new AltusCredential(sharedAltusAccessKey, sharedAltusSecretKey));
            } else {
                LOGGER.debug("Use shared credential global config credential is set, but no shared access/secret keypair is used.");
            }
        }

        return Optional.of(umsClient.createMachineUserAndGenerateKeys(
                machineUserName,
                actorCrn,
                umsClient.getBuiltInDatabusRoleCrn(),
                UserManagementProto.AccessKeyType.Value.ED25519));
    }

    public Optional<AltusCredential> generateMachineUserWithAccessKey(String machineUserName, String actorCrn) {
        return generateMachineUserWithAccessKey(machineUserName, actorCrn, false);
    }

    /**
     * Delete machine user with its access keys (and unassign databus role if required)
     */
    public void clearMachineUser(String machineUserName, String actorCrn, boolean useSharedCredential) {
        try {
            if (useSharedAltusCredential && areDatabusCredentialsFilled(useSharedCredential)) {
                LOGGER.debug("Access and secret keys are set manually application wide for Databus, skip machine user cleanup.");
            } else {
                umsClient.clearMachineUserWithAccessKeysAndRole(machineUserName, actorCrn, umsClient.getBuiltInDatabusRoleCrn());
            }
        } catch (Exception e) {
            LOGGER.warn("Cluster Databus resource cleanup failed (fluent - databus user). It is not a fatal issue, "
                    + "but note that you could have remaining UMS resources for your account", e);
        }
    }

    public void clearMachineUser(String machineUserName, String actorCrn) {
        clearMachineUser(machineUserName, actorCrn, false);
    }

    private boolean areDatabusCredentialsFilled(boolean useSharedCredential) {
        return useSharedCredential && sharedAltusSecretKey != null && sharedAltusSecretKey.length > 0 && StringUtils.isNotBlank(sharedAltusAccessKey);
    }

}