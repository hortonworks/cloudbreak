package com.sequenceiq.cloudbreak.auth.altus.service;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.cloudera.thunderhead.service.usermanagement.UserManagementProto;
import com.sequenceiq.cloudbreak.auth.altus.GrpcUmsClient;
import com.sequenceiq.cloudbreak.auth.altus.model.AltusCredential;
import com.sequenceiq.cloudbreak.auth.crn.RegionAwareInternalCrnGeneratorFactory;
import com.sequenceiq.common.api.telemetry.model.AnonymizationRule;

@Service
public class AltusIAMService {

    private static final Logger LOGGER = LoggerFactory.getLogger(AltusIAMService.class);

    private final GrpcUmsClient umsClient;

    private final SharedAltusCredentialProvider sharedAltusCredentialProvider;

    private final RoleCrnGenerator roleCrnGenerator;

    private final RegionAwareInternalCrnGeneratorFactory regionAwareInternalCrnGeneratorFactory;

    public AltusIAMService(GrpcUmsClient umsClient,
            SharedAltusCredentialProvider sharedAltusCredentialProvider,
            RoleCrnGenerator roleCrnGenerator,
            RegionAwareInternalCrnGeneratorFactory regionAwareInternalCrnGeneratorFactory) {
        this.umsClient = umsClient;
        this.sharedAltusCredentialProvider = sharedAltusCredentialProvider;
        this.roleCrnGenerator = roleCrnGenerator;
        this.regionAwareInternalCrnGeneratorFactory = regionAwareInternalCrnGeneratorFactory;
    }

    /**
     * Generate machine user with access keys
     */
    public AltusCredential generateMachineUserWithAccessKeyForLegacyCm(String machineUserName, String actorCrn, String accountId,
            Map<String, String> resourceRoles) {
        return umsClient.createMachineUserAndGenerateKeys(
                machineUserName,
                actorCrn,
                accountId,
                roleCrnGenerator.getBuiltInDatabusRoleCrn(accountId),
                resourceRoles,
                regionAwareInternalCrnGeneratorFactory);
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
                        roleCrnGenerator.getBuiltInDatabusRoleCrn(accountId),
                        Collections.emptyMap(),
                        UserManagementProto.AccessKeyType.Value.ED25519,
                        regionAwareInternalCrnGeneratorFactory)));
    }

    /**
     * Checks that machine user has a specific access key on UMS side
     */
    public boolean doesMachineUserHasAccessKey(String actorCrn, String accountId, String machineUserName, String accessKey,
            boolean useSharedAltusCredentialEnabled) {
        boolean result = false;
        if (sharedAltusCredentialProvider.isSharedAltusCredentialInUse(useSharedAltusCredentialEnabled)) {
            LOGGER.debug("Shared altus credential is used, no need for checking databus credentials against UMS");
            result = true;
        } else {
            LOGGER.debug("Query (or create if needed) machine user with name {}", machineUserName);
            Optional<String> machineUserCrn = umsClient.createMachineUser(machineUserName, actorCrn, accountId,
                    regionAwareInternalCrnGeneratorFactory);
            if (machineUserCrn.isPresent()) {
                return umsClient.doesMachineUserHasAccessKey(actorCrn, accountId, machineUserCrn.get(), accessKey,
                        regionAwareInternalCrnGeneratorFactory);
            } else {
                LOGGER.debug("Machine user ('{}') does not exist (even after the creation).", machineUserName);
            }
        }
        return result;
    }

    /**
     * Delete machine user with its access keys (and unassign databus role if required)
     */
    public void clearMachineUser(String machineUserName, String actorCrn, String accountId, boolean useSharedCredential) {
        try {
            if (sharedAltusCredentialProvider.isSharedAltusCredentialInUse(useSharedCredential)) {
                LOGGER.debug("Access and secret keys are set manually application wide for Databus, skip machine user cleanup.");
            } else {
                umsClient.clearMachineUserWithAccessKeysAndRole(machineUserName, actorCrn, accountId,
                        roleCrnGenerator.getBuiltInDatabusRoleCrn(accountId), regionAwareInternalCrnGeneratorFactory);
            }
        } catch (Exception e) {
            LOGGER.warn("Cluster Databus resource cleanup failed (fluent - databus user). It is not a fatal issue, "
                    + "but note that you could have remaining UMS resources for your account", e);
        }
    }

    /**
     * Delete machine user with its access keys (and unassign databus role if required)
     */
    public void clearLegacyMachineUser(String machineUserName, String actorCrn, String accountId) {
        clearMachineUser(machineUserName, actorCrn, accountId, false);
    }

    public List<AnonymizationRule> getAnonymizationRules(String accountId, String actorCrn) {
        return umsClient.getAnonymizationRules(accountId, actorCrn);
    }

    public void clearMachineUser(String machineUserName, String actorCrn, String accountId) {
        clearMachineUser(machineUserName, actorCrn, accountId, false);
    }

    public List<UserManagementProto.MachineUser> getAllMachineUsersForAccount(String accountId) {
        return umsClient.listAllMachineUsers(accountId, true, true, regionAwareInternalCrnGeneratorFactory);
    }
}