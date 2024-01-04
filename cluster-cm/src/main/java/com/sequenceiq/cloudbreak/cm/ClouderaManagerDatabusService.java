package com.sequenceiq.cloudbreak.cm;

import java.util.Map;

import jakarta.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.google.common.annotations.VisibleForTesting;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.altus.model.AltusCredential;
import com.sequenceiq.cloudbreak.auth.altus.service.AltusIAMService;
import com.sequenceiq.cloudbreak.auth.altus.service.RoleCrnGenerator;
import com.sequenceiq.cloudbreak.auth.crn.Crn;
import com.sequenceiq.cloudbreak.auth.crn.CrnResourceDescriptor;
import com.sequenceiq.cloudbreak.auth.crn.RegionAwareCrnGenerator;
import com.sequenceiq.cloudbreak.auth.crn.RegionAwareInternalCrnGeneratorFactory;
import com.sequenceiq.cloudbreak.dto.StackDtoDelegate;

@Service
public class ClouderaManagerDatabusService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClouderaManagerDatabusService.class);

    private static final String DATABUS_CRN_PATTERN = "datahub-wa-publisher-%s";

    @Inject
    private AltusIAMService altusIAMService;

    @Inject
    private RegionAwareInternalCrnGeneratorFactory regionAwareInternalCrnGeneratorFactory;

    @Inject
    private RoleCrnGenerator roleCrnGenerator;

    @Inject
    private RegionAwareCrnGenerator regionAwareCrnGenerator;

    /**
     * Generate new machine user (if it is needed) and access api key for this user.
     * Also assign built-in dabaus uploader role for the machine user.
     *
     * @param stack stack that is used to get user crn
     * @return credential
     */
    AltusCredential createMachineUserAndGenerateKeys(StackDtoDelegate stack, Map<String, String> resourceRoles) {
        String machineUserName = getWAMachineUserName(stack);
        return ThreadBasedUserCrnProvider.doAsInternalActor(
                regionAwareInternalCrnGeneratorFactory.iam().getInternalCrnForServiceAsString(),
                () -> altusIAMService.generateMachineUserWithAccessKeyForLegacyCm(
                        machineUserName,
                        ThreadBasedUserCrnProvider.getUserCrn(),
                        Crn.fromString(stack.getResourceCrn()).getAccountId(),
                        resourceRoles));
    }

    /**
     * Cleanup machine user related resources (access keys, role, user)
     * @param stack stack that is used to get user crn
     */
    void cleanUpMachineUser(StackDtoDelegate stack) {
        try {
            String machineUserName = getWAMachineUserName(stack);
            ThreadBasedUserCrnProvider.doAsInternalActor(
                    regionAwareInternalCrnGeneratorFactory.iam().getInternalCrnForServiceAsString(),
                    () ->  altusIAMService.clearLegacyMachineUser(machineUserName, Crn.fromString(stack.getResourceCrn()).getAccountId()));
        } catch (Exception e) {
            LOGGER.warn("Cluster Databus resource cleanup failed. It is not a fatal issue, "
                    + "but note that you could have remaining UMS resources for your account", e);
        }
    }

    AltusCredential getAltusCredential(StackDtoDelegate stack, String sdxStackCrn) {
        Map<String, String> resourceRoles = calculateResourceRoles(sdxStackCrn);
        AltusCredential credential = createMachineUserAndGenerateKeys(stack, resourceRoles);
        String accessKey = credential.getAccessKey();
        String privateKey = trimAndReplacePrivateKey(credential.getPrivateKey());
        return new AltusCredential(accessKey, privateKey.toCharArray());
    }

    private Map<String, String> calculateResourceRoles(String sdxStackCrn) {
        String accountId = Crn.safeFromString(sdxStackCrn).getAccountId();
        String resourceRoleCrn = roleCrnGenerator.getBuiltInWXMClusterAdminResourceRoleCrn(accountId);
        String wxmEnvironmentCrn = calculateWXMEnvironmentCrnBasedOnSdxCrn(sdxStackCrn);
        return Map.of(wxmEnvironmentCrn, resourceRoleCrn);
    }

    private String calculateWXMEnvironmentCrnBasedOnSdxCrn(String sdxStackCrn) {
        Crn sdxCrn = Crn.safeFromString(sdxStackCrn);
        String resource = sdxCrn.getResource() + "/" + sdxCrn.getResource();
        return regionAwareCrnGenerator.generateCrnString(CrnResourceDescriptor.WXM_ENVIRONMENT, resource, sdxCrn.getAccountId());
    }

    // CM expects the private key to come in as a single line, so we need to
    // encode the newlines. We trim it to avoid a CM warning that the key
    // should not start or end with whitespace.
    @VisibleForTesting
    String trimAndReplacePrivateKey(char[] privateKey) {
        return new String(privateKey).trim().replace("\n", "\\n");
    }

    private String getWAMachineUserName(StackDtoDelegate stack) {
        String machineUserSuffix = null;
        if (StringUtils.isNotEmpty(stack.getResourceCrn())) {
            machineUserSuffix = Crn.fromString(stack.getResourceCrn()).getResource();
        } else {
            machineUserSuffix = String.format("%s-%s", Crn.fromString(stack.getResourceCrn()).getAccountId(), stack.getCluster().getId());
        }
        return String.format(DATABUS_CRN_PATTERN, machineUserSuffix);
    }
}
