package com.sequenceiq.cloudbreak.auth.altus.service;

import static com.cloudera.thunderhead.service.usermanagement.UserManagementProto.AccessKeyType;
import static com.cloudera.thunderhead.service.usermanagement.UserManagementProto.MachineUser;

import java.security.Provider;
import java.security.Security;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.auth.altus.GrpcUmsClient;
import com.sequenceiq.cloudbreak.auth.altus.model.AltusCredential;
import com.sequenceiq.cloudbreak.auth.altus.model.CdpAccessKeyType;
import com.sequenceiq.cloudbreak.auth.altus.model.MachineUserRequest;
import com.sequenceiq.common.api.telemetry.model.AnonymizationRule;

@Service
public class AltusIAMService {

    private static final Logger LOGGER = LoggerFactory.getLogger(AltusIAMService.class);

    private static final String SUN_PKCS_11_NSS_FIPS_PROVIDER_NAME = "SunPKCS11-NSS-FIPS";

    private final GrpcUmsClient umsClient;

    private final SharedAltusCredentialProvider sharedAltusCredentialProvider;

    private final RoleCrnGenerator roleCrnGenerator;

    public AltusIAMService(GrpcUmsClient umsClient, SharedAltusCredentialProvider sharedAltusCredentialProvider, RoleCrnGenerator roleCrnGenerator) {
        this.umsClient = umsClient;
        this.sharedAltusCredentialProvider = sharedAltusCredentialProvider;
        this.roleCrnGenerator = roleCrnGenerator;
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
                getDefaultAccessKeyType());
    }

    private AccessKeyType.Value getDefaultAccessKeyType() {
        Provider sunNSSFipsProvider = Security.getProvider(SUN_PKCS_11_NSS_FIPS_PROVIDER_NAME);
        AccessKeyType.Value defaultAccessKeyType = sunNSSFipsProvider != null ? AccessKeyType.Value.ECDSA : AccessKeyType.Value.RSA;
        LOGGER.debug("Default access key type is {}", defaultAccessKeyType);
        return defaultAccessKeyType;
    }

    /**
     * Generate databus machine user with access keys
     */
    public Optional<AltusCredential> generateDatabusMachineUserWithAccessKey(MachineUserRequest machineUserRequest, boolean useSharedCredential) {
        return Optional.ofNullable(sharedAltusCredentialProvider.getSharedCredentialIfConfigured(useSharedCredential)
                .orElse(umsClient.createMachineUserAndGenerateKeys(
                        machineUserRequest.getName(),
                        machineUserRequest.getActorCrn(),
                        machineUserRequest.getAccountId(),
                        roleCrnGenerator.getBuiltInDatabusRoleCrn(machineUserRequest.getAccountId()),
                        Collections.emptyMap(),
                        mapToAccessKeyType(machineUserRequest.getCdpAccessKeyType()))));
    }

    /**
     * Generate monitoring machine user with access keys
     */
    public Optional<AltusCredential> generateMonitoringMachineUserWithAccessKey(MachineUserRequest machineUserRequest, boolean useSharedCredential) {
        String accountId = machineUserRequest.getAccountId();
        return Optional.ofNullable(sharedAltusCredentialProvider.getSharedCredentialIfConfigured(useSharedCredential)
                .orElse(umsClient.createMachineUserAndGenerateKeys(
                        machineUserRequest.getName(),
                        machineUserRequest.getActorCrn(),
                        accountId,
                        Set.of(roleCrnGenerator.getBuiltInDatabusRoleCrn(accountId), roleCrnGenerator.getBuiltInComputeMetricsPublisherRoleCrn(accountId)),
                        Collections.emptyMap(),
                        mapToAccessKeyType(machineUserRequest.getCdpAccessKeyType()))));
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
            Optional<String> machineUserCrn = umsClient.createMachineUser(machineUserName, actorCrn, accountId);
            if (machineUserCrn.isPresent()) {
                return umsClient.doesMachineUserHasAccessKey(accountId, machineUserCrn.get(), accessKey);
            } else {
                LOGGER.debug("Machine user ('{}') does not exist (even after the creation).", machineUserName);
            }
        }
        return result;
    }

    /**
     * Delete machine user.
     */
    public void clearMachineUser(String machineUserName, String accountId, boolean useSharedCredential) {
        try {
            if (sharedAltusCredentialProvider.isSharedAltusCredentialInUse(useSharedCredential)) {
                LOGGER.debug("Access and secret keys are set manually application wide, skip machine user cleanup.");
            } else {
                umsClient.deleteMachineUser(machineUserName, accountId);
            }
        } catch (Exception e) {
            LOGGER.warn("Failed to delete {} machine user. It is not a fatal issue, but note that you could have remaining UMS resources for your account",
                    machineUserName, e);
        }
    }

    /**
     * Delete machine user with its access keys (and unassign databus role if required)
     */
    public void clearLegacyMachineUser(String machineUserName, String accountId) {
        clearMachineUser(machineUserName, accountId, false);
    }

    public List<AnonymizationRule> getAnonymizationRules(String accountId, String actorCrn) {
        return umsClient.getAnonymizationRules(accountId, actorCrn);
    }

    public void clearMachineUser(String machineUserName, String accountId) {
        clearMachineUser(machineUserName, accountId, false);
    }

    public List<MachineUser> getAllMachineUsersForAccount(String accountId) {
        return umsClient.listAllMachineUsers(accountId, true, true);
    }

    private AccessKeyType.Value mapToAccessKeyType(CdpAccessKeyType cdpAccessKeyType) {
        switch (cdpAccessKeyType) {
            case ED25519:
                return AccessKeyType.Value.ED25519;
            case RSA:
                return AccessKeyType.Value.RSA;
            case ECDSA:
                return AccessKeyType.Value.ECDSA;
            default:
                return AccessKeyType.Value.ED25519;
        }
    }
}