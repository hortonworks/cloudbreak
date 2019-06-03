package com.sequenceiq.cloudbreak.cm;

import java.util.Optional;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.cloudera.thunderhead.service.usermanagement.UserManagementProto;
import com.google.common.annotations.VisibleForTesting;
import com.sequenceiq.cloudbreak.auth.altus.Crn;
import com.sequenceiq.cloudbreak.auth.altus.GrpcUmsClient;
import com.sequenceiq.cloudbreak.auth.altus.model.AltusCredential;
import com.sequenceiq.cloudbreak.cloud.model.Telemetry;
import com.sequenceiq.cloudbreak.domain.stack.Stack;

@Service
public class ClouderaManagerDatabusService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClouderaManagerDatabusService.class);

    private static final String DATABUS_CRN_PATTERN = "dataeng-wa-publisher-%s-%s";

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
        String machineUserName = getWAMachineUserName(userCrn, stack.getCluster().getId().toString());
        UserManagementProto.MachineUser machineUser = umsClient.createMachineUser(machineUserName, userCrn, Optional.empty());
        String builtInDbusRoleCrn = getBuiltInDatabusCrn();
        umsClient.assignMachineUserRole(userCrn, machineUser.getCrn(), builtInDbusRoleCrn, Optional.empty());
        return umsClient.generateAccessSecretKeyPair(userCrn, machineUser.getCrn(), Optional.empty());
    }

    /**
     * Cleanup machine user related resources (access keys, role, user)
     * @param stack stack that is used to get user crn
     */
    void cleanUpMachineUser(Stack stack, Telemetry telemetry) {
        if (telemetry.getWorkloadAnalytics().getPrivateKey() == null || telemetry.getWorkloadAnalytics().getAccessKey() == null) {
            try {
                String userCrn = stack.getCreator().getUserCrn();
                String machineUserName = getWAMachineUserName(userCrn, stack.getCluster().getId().toString());
                String builtInDbusRoleCrn = getBuiltInDatabusCrn();
                umsClient.unassignMachineUserRole(userCrn, machineUserName, builtInDbusRoleCrn, Optional.empty());
                umsClient.deleteMachineUserAccessKeys(userCrn, machineUserName, Optional.empty());
                umsClient.deleteMachineUser(machineUserName, userCrn, Optional.empty());
            } catch (Exception e) {
                    LOGGER.warn("Cluster Databus resource cleanup failed. It is not a fatal issue, "
                            + "but note that you could have remaining UMS resources for your account", e);
                }
        } else {
            LOGGER.info("Skipping machine user deletion as api keys were provided manually.");
        }
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

    private String getWAMachineUserName(String userCrn, String clusterId) {
        return String.format(DATABUS_CRN_PATTERN, Crn.fromString(userCrn).getAccountId(), clusterId);
    }
}
