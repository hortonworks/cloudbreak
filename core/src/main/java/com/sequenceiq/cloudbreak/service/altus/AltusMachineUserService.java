package com.sequenceiq.cloudbreak.service.altus;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.cloudera.thunderhead.service.usermanagement.UserManagementProto;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.StackType;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.crn.Crn;
import com.sequenceiq.cloudbreak.auth.altus.model.AltusCredential;
import com.sequenceiq.cloudbreak.auth.altus.service.AltusIAMService;
import com.sequenceiq.cloudbreak.auth.crn.RegionAwareInternalCrnGeneratorFactory;
import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.service.ComponentConfigProviderService;
import com.sequenceiq.cloudbreak.service.cluster.ClusterService;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.common.api.telemetry.model.DataBusCredential;
import com.sequenceiq.common.api.telemetry.model.Telemetry;

@Service
public class AltusMachineUserService {

    private static final Logger LOGGER = LoggerFactory.getLogger(AltusMachineUserService.class);

    private static final String FLUENT_DATABUS_MACHINE_USER_NAME_PATTERN = "%s-fluent-databus-uploader-%s";

    private final AltusIAMService altusIAMService;

    private final StackService stackService;

    private final ClusterService clusterService;

    private final ComponentConfigProviderService componentConfigProviderService;

    private final RegionAwareInternalCrnGeneratorFactory regionAwareInternalCrnGeneratorFactory;

    public AltusMachineUserService(
            AltusIAMService altusIAMService,
            StackService stackService,
            ClusterService clusterService,
            ComponentConfigProviderService componentConfigProviderService,
            RegionAwareInternalCrnGeneratorFactory regionAwareInternalCrnGeneratorFactory) {
        this.altusIAMService = altusIAMService;
        this.stackService = stackService;
        this.clusterService = clusterService;
        this.componentConfigProviderService = componentConfigProviderService;
        this.regionAwareInternalCrnGeneratorFactory = regionAwareInternalCrnGeneratorFactory;
    }

    /**
     * Generate machine user for fluentd - databus communication
     */
    public Optional<AltusCredential> generateDatabusMachineUserForFluent(Stack stack, Telemetry telemetry, boolean forced) {
        if (isMeteringOrAnyDataBusBasedFeatureSupported(stack, telemetry) || forced) {
            return ThreadBasedUserCrnProvider.doAsInternalActor(
                    regionAwareInternalCrnGeneratorFactory.iam().getInternalCrnForServiceAsString(),
                    () -> altusIAMService.generateMachineUserWithAccessKey(
                    getFluentDatabusMachineUserName(stack),
                    ThreadBasedUserCrnProvider.getUserCrn(),
                    Crn.fromString(stack.getResourceCrn()).getAccountId(),
                    telemetry.isUseSharedAltusCredentialEnabled()));
        }
        return Optional.empty();
    }

    public Optional<AltusCredential> generateDatabusMachineUserForFluent(Stack stack, Telemetry telemetry) {
        return generateDatabusMachineUserForFluent(stack, telemetry, false);
    }

    /**
     * Delete machine user for fluent based upload with its access keys (and unassign databus role if required)
     */
    public void clearFluentMachineUser(Stack stack, Telemetry telemetry) {
        if (isMeteringOrAnyDataBusBasedFeatureSupported(stack, telemetry) || isDataBusCredentialAvailable(stack)) {
            String machineUserName = getFluentDatabusMachineUserName(stack);
            ThreadBasedUserCrnProvider.doAsInternalActor(
                    regionAwareInternalCrnGeneratorFactory.iam().getInternalCrnForServiceAsString(),
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
                regionAwareInternalCrnGeneratorFactory.iam().getInternalCrnForServiceAsString(),
                () -> altusIAMService.clearMachineUser(machineUserName,
                        ThreadBasedUserCrnProvider.getUserCrn(),
                        accountId
                )
        );
    }

    /**
     * Gather or create DataBus credential for a stack.
     * On creation it will generate aa new workload user with new access/private keys.
     * @param stackId id of the stack
     * @return databus credential holder
     */
    public DataBusCredential getOrCreateDataBusCredentialIfNeeded(Long stackId) throws IOException {
        LOGGER.debug("Get or create databus credential for stack");
        Stack stack = stackService.get(stackId);
        Cluster cluster = clusterService.findOneByStackIdOrNotFoundError(stackId);
        cluster.getDatabusCredential();
        Telemetry telemetry = componentConfigProviderService.getTelemetry(stackId);
        if (cluster.getDatabusCredential() != null) {
            LOGGER.debug("Databus credential has been found for the stack");
            DataBusCredential dataBusCredential = new Json(cluster.getDatabusCredential()).get(DataBusCredential.class);
            if (isDataBusCredentialStillExist(telemetry, dataBusCredential, stack)) {
                LOGGER.debug("Databus credential exists both in the stack and on UMS side");
                return dataBusCredential;
            } else {
                LOGGER.debug("Databus credential exists on stack side but does not exists on UMS side, it will be updated ...");
            }
        } else {
            LOGGER.debug("Databus credential does not exist for the stack, it will be created ...");
        }
        Optional<AltusCredential> altusCredential = generateDatabusMachineUserForFluent(stack, telemetry, true);
        return storeDataBusCredential(altusCredential, stack);
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
                regionAwareInternalCrnGeneratorFactory.iam().getInternalCrnForServiceAsString(),
                () -> altusIAMService.doesMachineUserHasAccessKey(
                        ThreadBasedUserCrnProvider.getUserCrn(),
                        Crn.fromString(stack.getResourceCrn()).getAccountId(),
                        dataBusCredential.getMachineUserName(), dataBusCredential.getAccessKey(),
                        telemetry.isUseSharedAltusCredentialEnabled()));
    }

    // for datalake metering is not supported/required right now
    // TODO: monitoring do not need databus machine user - until fluent based metrics pushing is not supported
    public boolean isMeteringOrAnyDataBusBasedFeatureSupported(Stack stack, Telemetry telemetry) {
        return telemetry != null && (telemetry.isAnyDataBusBasedFeatureEnablred() || (telemetry.isMeteringFeatureEnabled()
                && !StackType.DATALAKE.equals(stack.getType())));
    }

    public List<UserManagementProto.MachineUser> getAllInternalMachineUsers(String accountId) {
        return ThreadBasedUserCrnProvider.doAsInternalActor(
                regionAwareInternalCrnGeneratorFactory.iam().getInternalCrnForServiceAsString(),
                () -> altusIAMService.getAllMachineUsersForAccount(accountId)
        );
    }

    public String getFluentDatabusMachineUserName(String clusterType, String resource) {
        return String.format(FLUENT_DATABUS_MACHINE_USER_NAME_PATTERN, clusterType, resource);
    }

    private boolean isDataBusCredentialAvailable(Stack stack) {
        return stack.getCluster() != null && StringUtils.isNotBlank(stack.getCluster().getDatabusCredential());
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