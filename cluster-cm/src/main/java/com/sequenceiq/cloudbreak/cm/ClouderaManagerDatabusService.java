package com.sequenceiq.cloudbreak.cm;

import java.util.Optional;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.cloudera.thunderhead.service.usermanagement.UserManagementProto;
import com.google.common.annotations.VisibleForTesting;
import com.sequenceiq.cloudbreak.auth.altus.Crn;
import com.sequenceiq.cloudbreak.auth.altus.GrpcUmsClient;
import com.sequenceiq.cloudbreak.auth.altus.model.AltusCredential;
import com.sequenceiq.cloudbreak.domain.stack.Stack;

@Service
public class ClouderaManagerDatabusService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClouderaManagerDatabusService.class);

    private static final String DATABUS_CRN_PATTERN = "dataeng-wa-publisher-%s";

    private static final String DATABUS_UPLOADER_RESOURCE_NAME = "DbusUploader";

    @Inject
    private GrpcUmsClient umsClient;

    /**
     * Generate new machine user (if it is needed) and access api key for this user.
     * Also assign built-in dabaus uploader role for the machine user.
     * @param stack stack that is used to get user crn
     * @return cre
     */
    AltusCredential createMachineUserAndGenerateKeys(Stack stack) {
        String userCrn = stack.getCreator().getUserCrn();
        String machineUserName = getWAMachineUserName(userCrn, stack);
        UserManagementProto.MachineUser machineUser = umsClient.createMachineUser(machineUserName, userCrn, Optional.empty());
        String builtInDbusRoleCrn = getBuiltInDatabusCrn();
        umsClient.assignMachineUserRole(userCrn, machineUser.getCrn(), builtInDbusRoleCrn, Optional.empty());
        return umsClient.generateAccessSecretKeyPair(userCrn, machineUser.getCrn(), Optional.empty());
    }

    /**
     * Cleanup machine user related resources (access keys, role, user)
     * @param stack stack that is used to get user crn
     */
    void cleanUpMachineUser(Stack stack) {
        try {
            String userCrn = stack.getCreator().getUserCrn();
            String machineUserName = getWAMachineUserName(userCrn, stack);
            String builtInDbusRoleCrn = getBuiltInDatabusCrn();
            umsClient.unassignMachineUserRole(userCrn, machineUserName, builtInDbusRoleCrn, Optional.empty());
            umsClient.deleteMachineUserAccessKeys(userCrn, machineUserName, Optional.empty());
            umsClient.deleteMachineUser(machineUserName, userCrn, Optional.empty());
        } catch (Exception e) {
            LOGGER.warn("Cluster Databus resource cleanup failed. It is not a fatal issue, "
                    + "but note that you could have remaining UMS resources for your account", e);
        }
    }

    @VisibleForTesting
    AltusCredential getAltusCredential(Stack stack) {
        AltusCredential credential = createMachineUserAndGenerateKeys(stack);
        String accessKey = credential.getAccessKey();
        String privateKey = trimAndReplacePrivateKey(credential.getPrivateKey());
        return new AltusCredential(accessKey, privateKey.toCharArray());
    }

    // Partition and region is hard coded right now,
    // if it will change use the same as the user crn
    @VisibleForTesting
    String getBuiltInDatabusCrn() {
        Crn databusCrn = Crn.builder()
                .setAccountId("altus")
                .setService(Crn.Service.IAM)
                .setResourceType(Crn.ResourceType.ROLE)
                .setResource(DATABUS_UPLOADER_RESOURCE_NAME)
                .build();
        return databusCrn.toString();
    }

    // CM expects the private key to come in as a single line, so we need to
    // encode the newlines. We trim it to avoid a CM warning that the key
    // should not start or end with whitespace.
    @VisibleForTesting
    String trimAndReplacePrivateKey(char[] privateKey) {
        return new String(privateKey).trim().replace("\n", "\\n");
    }

    private String getWAMachineUserName(String userCrn, Stack stack) {
        String machineUserSuffix = null;
        if (StringUtils.isNotEmpty(stack.getResourceCrn())) {
            machineUserSuffix = Crn.fromString(stack.getResourceCrn()).getResource();
        } else {
            machineUserSuffix = String.format("%s-%s", Crn.fromString(userCrn).getAccountId(), stack.getCluster().getId());
        }
        return String.format(DATABUS_CRN_PATTERN, machineUserSuffix);
    }
}