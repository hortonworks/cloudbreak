package com.sequenceiq.cloudbreak.service.altus;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.cloudera.thunderhead.service.usermanagement.UserManagementProto;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.StackType;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.altus.Crn;
import com.sequenceiq.cloudbreak.auth.altus.model.AltusCredential;
import com.sequenceiq.cloudbreak.auth.altus.service.AltusIAMService;
import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.service.cluster.ClusterService;
import com.sequenceiq.common.api.telemetry.model.DataBusCredential;
import com.sequenceiq.common.api.telemetry.model.Telemetry;

@Service
public class AltusMachineUserService {

    private static final String FLUENT_DATABUS_MACHINE_USER_NAME_PATTERN = "%s-fluent-databus-uploader-%s";

    private final AltusIAMService altusIAMService;

    private final ClusterService clusterService;

    public AltusMachineUserService(AltusIAMService altusIAMService, ClusterService clusterService) {
        this.altusIAMService = altusIAMService;
        this.clusterService = clusterService;
    }

    /**
     * Generate machine user for fluentd - databus communication
     */
    public Optional<AltusCredential> generateDatabusMachineUserForFluent(Stack stack, Telemetry telemetry) {
        if (isMeteringOrAnyDataBusBasedFeatureSupported(stack, telemetry)) {
            return ThreadBasedUserCrnProvider.doAsInternalActor(() -> altusIAMService.generateMachineUserWithAccessKey(
                    getFluentDatabusMachineUserName(stack),
                    ThreadBasedUserCrnProvider.getUserCrn(),
                    Crn.fromString(stack.getResourceCrn()).getAccountId(),
                    telemetry.isUseSharedAltusCredentialEnabled()));
        }
        return Optional.empty();
    }

    /**
     * Delete machine user for fluent based upload with its access keys (and unassign databus role if required)
     */
    public void clearFluentMachineUser(Stack stack, Telemetry telemetry) {
        if (isMeteringOrAnyDataBusBasedFeatureSupported(stack, telemetry)) {
            String machineUserName = getFluentDatabusMachineUserName(stack);
            ThreadBasedUserCrnProvider.doAsInternalActor(
                    () -> altusIAMService.clearMachineUser(
                            machineUserName,
                            ThreadBasedUserCrnProvider.getUserCrn(),
                            Crn.fromString(stack.getResourceCrn()).getAccountId(),
                            telemetry.isUseSharedAltusCredentialEnabled()));
        }
    }

    /**
     * Delete machine user with access keys (and unassign databus role if required) by provided machine user name and account id
     */
    public void cleanupMachineUser(String machineUserName, String accountId) {
        ThreadBasedUserCrnProvider.doAsInternalActor(
                () -> altusIAMService.clearMachineUser(machineUserName,
                        ThreadBasedUserCrnProvider.getUserCrn(),
                        accountId
                )
        );
    }

    /**
     * Store databus access / secret keypair and machine user name in the cluster if altus credential exists
     * @param altusCredential dto for databus access/private key
     * @param stack component will be attached to this stack
     * @return domain object that holds databus credential
     */
    public DataBusCredential storeDataBusCredential(Optional<AltusCredential> altusCredential, Stack stack) {
        if (altusCredential.isPresent()) {
            DataBusCredential dataBusCredential = new DataBusCredential();
            dataBusCredential.setMachineUserName(getFluentDatabusMachineUserName(stack));
            dataBusCredential.setAccessKey(altusCredential.get().getAccessKey());
            dataBusCredential.setPrivateKey(altusCredential.get().getPrivateKey() != null ? new String(altusCredential.get().getPrivateKey()) : null);
            String databusCredentialJsonString = new Json(dataBusCredential).getValue();
            Cluster cluster = stack.getCluster();
            if (cluster != null) {
                cluster.setDatabusCredential(databusCredentialJsonString);
                clusterService.updateCluster(cluster);
            }
            return dataBusCredential;
        }
        return null;
    }

    /**
     * Check that machine user still have the access key on UMS side
     * @param dataBusCredential databus credential DTO that comes from cloudbreak database which contains access key and machune user name as well.
     * @param stack stack object holder that can be used to calculate the machine user name
     * @return check result - if true, no need to regenerate keys
     */
    public boolean isDataBusCredentialStillExist(Telemetry telemetry, DataBusCredential dataBusCredential, Stack stack) {
        return ThreadBasedUserCrnProvider.doAsInternalActor(
                () -> altusIAMService.doesMachineUserHasAccessKey(
                        ThreadBasedUserCrnProvider.getUserCrn(),
                        Crn.fromString(stack.getResourceCrn()).getAccountId(),
                        dataBusCredential.getMachineUserName(), dataBusCredential.getAccessKey(),
                        telemetry.isUseSharedAltusCredentialEnabled()));
    }

    // for datalake metering is not supported/required right now
    public boolean isMeteringOrAnyDataBusBasedFeatureSupported(Stack stack, Telemetry telemetry) {
        return telemetry != null && (telemetry.isAnyDataBusBasedFeatureEnablred() || (telemetry.isMeteringFeatureEnabled()
                && !StackType.DATALAKE.equals(stack.getType())));
    }

    public List<UserManagementProto.MachineUser> getAllInternalMachineUsers(String accountId) {
        return ThreadBasedUserCrnProvider.doAsInternalActor(
                () -> altusIAMService.getAllMachineUsersForAccount(
                        ThreadBasedUserCrnProvider.getUserCrn(),
                        accountId)
        );
    }

    public String getFluentDatabusMachineUserName(String clusterType, String resource) {
        return String.format(FLUENT_DATABUS_MACHINE_USER_NAME_PATTERN, clusterType, resource);
    }

    private String getFluentDatabusMachineUserName(Stack stack) {
        String clusterType = "cb";
        if (StackType.DATALAKE.equals(stack.getType())) {
            clusterType = "datalake";
        } else if (StackType.WORKLOAD.equals(stack.getType())) {
            clusterType = "datahub";
        }
        return getFluentDatabusMachineUserName(clusterType, Crn.fromString(stack.getResourceCrn()).getResource());
    }
}