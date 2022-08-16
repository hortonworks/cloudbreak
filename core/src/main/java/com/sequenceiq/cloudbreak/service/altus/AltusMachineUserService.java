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
import com.sequenceiq.cloudbreak.auth.altus.model.AltusCredential;
import com.sequenceiq.cloudbreak.auth.altus.service.AltusIAMService;
import com.sequenceiq.cloudbreak.auth.crn.Crn;
import com.sequenceiq.cloudbreak.auth.crn.RegionAwareInternalCrnGeneratorFactory;
import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.service.ComponentConfigProviderService;
import com.sequenceiq.cloudbreak.service.cluster.ClusterService;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;
import com.sequenceiq.cloudbreak.view.ClusterView;
import com.sequenceiq.cloudbreak.view.StackView;
import com.sequenceiq.common.api.telemetry.model.DataBusCredential;
import com.sequenceiq.common.api.telemetry.model.MonitoringCredential;
import com.sequenceiq.common.api.telemetry.model.Telemetry;

@Service
public class AltusMachineUserService {

    private static final Logger LOGGER = LoggerFactory.getLogger(AltusMachineUserService.class);

    private static final String FLUENT_DATABUS_MACHINE_USER_NAME_PATTERN = "%s-fluent-databus-uploader-%s";

    private static final String MONITORING_MACHINE_USER_NAME_PATTERN = "%s-monitoring-%s";

    private final AltusIAMService altusIAMService;

    private final StackDtoService stackDtoService;

    private final ClusterService clusterService;

    private final ComponentConfigProviderService componentConfigProviderService;

    private final RegionAwareInternalCrnGeneratorFactory regionAwareInternalCrnGeneratorFactory;

    public AltusMachineUserService(
            AltusIAMService altusIAMService,
            StackDtoService stackDtoService,
            ClusterService clusterService,
            ComponentConfigProviderService componentConfigProviderService,
            RegionAwareInternalCrnGeneratorFactory regionAwareInternalCrnGeneratorFactory) {
        this.altusIAMService = altusIAMService;
        this.stackDtoService = stackDtoService;
        this.clusterService = clusterService;
        this.componentConfigProviderService = componentConfigProviderService;
        this.regionAwareInternalCrnGeneratorFactory = regionAwareInternalCrnGeneratorFactory;
    }

    /**
     * Generate machine user for fluentd - databus communication
     */
    public Optional<AltusCredential> generateDatabusMachineUserForFluent(StackView stack, Telemetry telemetry, boolean forced) {
        if (isAnyDataBusBasedFeatureSupported(telemetry) || forced) {
            return ThreadBasedUserCrnProvider.doAsInternalActor(
                    regionAwareInternalCrnGeneratorFactory.iam().getInternalCrnForServiceAsString(),
                    () -> altusIAMService.generateDatabusMachineUserWithAccessKey(
                            getFluentDatabusMachineUserName(stack),
                            ThreadBasedUserCrnProvider.getUserCrn(),
                            Crn.fromString(stack.getResourceCrn()).getAccountId(),
                            telemetry.isUseSharedAltusCredentialEnabled()));
        }
        return Optional.empty();
    }

    public Optional<AltusCredential> generateMonitoringMachineUser(StackView stack, Telemetry telemetry, boolean forced) {
        if (isAnyMonitoringFeatureSupported(telemetry) || forced) {
            return ThreadBasedUserCrnProvider.doAsInternalActor(
                    regionAwareInternalCrnGeneratorFactory.iam().getInternalCrnForServiceAsString(),
                    () -> altusIAMService.generateMonitoringMachineUserWithAccessKey(
                            getMonitoringMachineUserName(stack),
                            ThreadBasedUserCrnProvider.getUserCrn(),
                            Crn.fromString(stack.getResourceCrn()).getAccountId(),
                            telemetry.isUseSharedAltusCredentialEnabled()));
        }
        return Optional.empty();
    }

    public Optional<AltusCredential> generateDatabusMachineUserForFluent(StackView stack, Telemetry telemetry) {
        return generateDatabusMachineUserForFluent(stack, telemetry, false);
    }

    public Optional<AltusCredential> generateMonitoringMachineUser(StackView stack, Telemetry telemetry) {
        return generateMonitoringMachineUser(stack, telemetry, false);
    }

    public void clearFluentMachineUser(StackView stack, ClusterView cluster, Telemetry telemetry) {
        if (isAnyDataBusBasedFeatureSupported(telemetry) || isDataBusCredentialAvailable(cluster)) {
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

    public void clearMonitoringMachineUser(StackView stack, ClusterView cluster, Telemetry telemetry) {
        if (isAnyMonitoringFeatureSupported(telemetry) || isMonitoringCredentialAvailable(cluster)) {
            String machineUserName = getMonitoringMachineUserName(stack);
            ThreadBasedUserCrnProvider.doAsInternalActor(
                    regionAwareInternalCrnGeneratorFactory.iam().getInternalCrnForServiceAsString(),
                    () -> altusIAMService.clearMachineUser(
                            machineUserName,
                            ThreadBasedUserCrnProvider.getUserCrn(),
                            Crn.fromString(stack.getResourceCrn()).getAccountId()));
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

    public DataBusCredential getOrCreateDataBusCredentialIfNeeded(Long stackId) throws IOException {
        LOGGER.debug("Get or create databus credential for stack");
        StackDto stackDto = stackDtoService.getById(stackId);
        StackView stack = stackDto.getStack();
        ClusterView cluster = stackDto.getCluster();
        cluster.getDatabusCredential();
        Telemetry telemetry = componentConfigProviderService.getTelemetry(stackId);
        if (cluster.getDatabusCredential() != null) {
            LOGGER.debug("Databus credential has been found for the stack");
            DataBusCredential credential = new Json(cluster.getDatabusCredential()).get(DataBusCredential.class);
            if (isCredentialExist(telemetry, Crn.fromString(stack.getResourceCrn()).getAccountId(), credential.getMachineUserName(),
                    credential.getAccessKey())) {
                LOGGER.debug("Databus credential exists both in the stack and on UMS side");
                return credential;
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
     *
     * @param altusCredential dto for databus access/private key
     * @param stack           component will be attached to this stack
     * @return domain object that holds databus credential
     */
    public DataBusCredential storeDataBusCredential(Optional<AltusCredential> altusCredential, StackView stack) {
        if (altusCredential.isPresent()) {
            DataBusCredential dataBusCredential = new DataBusCredential();
            dataBusCredential.setMachineUserName(getFluentDatabusMachineUserName(stack));
            dataBusCredential.setAccessKey(altusCredential.get().getAccessKey());
            dataBusCredential.setPrivateKey(altusCredential.get().getPrivateKey() != null ? new String(altusCredential.get().getPrivateKey()) : null);
            String databusCredentialJsonString = new Json(dataBusCredential).getValue();
            if (stack.getClusterId() != null) {
                clusterService.updateDatabusCredentialByClusterId(stack.getClusterId(), databusCredentialJsonString);
            }
            return dataBusCredential;
        }
        return null;
    }

    public MonitoringCredential storeMonitoringCredential(Optional<AltusCredential> altusCredential, StackView stack) {
        if (altusCredential.isPresent()) {
            MonitoringCredential monitoringCredential = new MonitoringCredential();
            monitoringCredential.setMachineUserName(getMonitoringMachineUserName(stack));
            monitoringCredential.setAccessKey(altusCredential.get().getAccessKey());
            monitoringCredential.setPrivateKey(altusCredential.get().getPrivateKey() != null ? new String(altusCredential.get().getPrivateKey()) : null);
            String monitoringCredentialJsonString = new Json(monitoringCredential).getValue();
            if (stack.getClusterId() != null) {
                clusterService.updateMonitoringCredentialByClusterId(stack.getClusterId(), monitoringCredentialJsonString);
            }
            return monitoringCredential;
        }
        return null;
    }

    public boolean isCredentialExist(Telemetry telemetry, String accountId, String machineUserName, String accessKey) {
        return ThreadBasedUserCrnProvider.doAsInternalActor(
                regionAwareInternalCrnGeneratorFactory.iam().getInternalCrnForServiceAsString(),
                () -> altusIAMService.doesMachineUserHasAccessKey(
                        ThreadBasedUserCrnProvider.getUserCrn(),
                        accountId,
                        machineUserName,
                        accessKey,
                        telemetry.isUseSharedAltusCredentialEnabled()));
    }

    public boolean isAnyDataBusBasedFeatureSupported(Telemetry telemetry) {
        return telemetry != null && telemetry.isAnyDataBusBasedFeatureEnablred();
    }

    public boolean isAnyMonitoringFeatureSupported(Telemetry telemetry) {
        return telemetry != null && telemetry.isComputeMonitoringEnabled() && telemetry.isMonitoringFeatureEnabled();
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

    public String getMonitoringMachineUserName(String clusterType, String resource) {
        return String.format(MONITORING_MACHINE_USER_NAME_PATTERN, clusterType, resource);
    }

    private boolean isDataBusCredentialAvailable(ClusterView cluster) {
        return cluster != null && StringUtils.isNotBlank(cluster.getDatabusCredential());
    }

    private boolean isMonitoringCredentialAvailable(ClusterView cluster) {
        return cluster != null && StringUtils.isNotBlank(cluster.getMonitoringCredential());
    }

    private String getFluentDatabusMachineUserName(StackView stack) {
        String clusterType = "cb";
        if (StackType.DATALAKE.equals(stack.getType())) {
            clusterType = "datalake";
        } else if (StackType.WORKLOAD.equals(stack.getType())) {
            clusterType = "datahub";
        }
        return getFluentDatabusMachineUserName(clusterType, Crn.fromString(stack.getResourceCrn()).getResource());
    }

    private String getMonitoringMachineUserName(StackView stack) {
        return getMonitoringMachineUserName(getClusterType(stack), Crn.fromString(stack.getResourceCrn()).getResource());
    }

    private String getClusterType(StackView stack) {
        if (StackType.DATALAKE.equals(stack.getType())) {
            return "datalake";
        } else if (StackType.WORKLOAD.equals(stack.getType())) {
            return "datahub";
        } else {
            return "cb";
        }
    }
}