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
import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.auth.altus.model.AltusCredential;
import com.sequenceiq.cloudbreak.auth.altus.model.CdpAccessKeyType;
import com.sequenceiq.cloudbreak.auth.altus.model.MachineUserRequest;
import com.sequenceiq.cloudbreak.auth.altus.service.AltusIAMService;
import com.sequenceiq.cloudbreak.auth.crn.Crn;
import com.sequenceiq.cloudbreak.cloud.aws.common.AwsConstants;
import com.sequenceiq.cloudbreak.cloud.model.Image;
import com.sequenceiq.cloudbreak.cmtemplate.configproviders.meteringv2.DatabusCredentialProvider;
import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.service.CloudbreakRuntimeException;
import com.sequenceiq.cloudbreak.service.ComponentConfigProviderService;
import com.sequenceiq.cloudbreak.service.cluster.ClusterService;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;
import com.sequenceiq.cloudbreak.telemetry.TelemetryFeatureService;
import com.sequenceiq.cloudbreak.template.views.DatabusCredentialView;
import com.sequenceiq.cloudbreak.view.ClusterView;
import com.sequenceiq.cloudbreak.view.StackView;
import com.sequenceiq.common.api.telemetry.model.DataBusCredential;
import com.sequenceiq.common.api.telemetry.model.MonitoringCredential;
import com.sequenceiq.common.api.telemetry.model.Telemetry;

@Service
public class AltusMachineUserService implements DatabusCredentialProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(AltusMachineUserService.class);

    private static final String FLUENT_DATABUS_MACHINE_USER_NAME_PATTERN = "%s-fluent-databus-uploader-%s";

    private static final String MONITORING_MACHINE_USER_NAME_PATTERN = "%s-monitoring-%s";

    private final AltusIAMService altusIAMService;

    private final StackDtoService stackDtoService;

    private final ClusterService clusterService;

    private final ComponentConfigProviderService componentConfigProviderService;

    private final TelemetryFeatureService telemetryFeatureService;

    public AltusMachineUserService(
            AltusIAMService altusIAMService,
            StackDtoService stackDtoService,
            ClusterService clusterService,
            ComponentConfigProviderService componentConfigProviderService,
            EntitlementService entitlementService,
            TelemetryFeatureService telemetryFeatureService) {
        this.altusIAMService = altusIAMService;
        this.stackDtoService = stackDtoService;
        this.clusterService = clusterService;
        this.componentConfigProviderService = componentConfigProviderService;
        this.telemetryFeatureService = telemetryFeatureService;
    }

    /**
     * Generate machine user for fluentd - databus communication
     */
    public Optional<AltusCredential> generateDatabusMachineUserForFluent(StackView stack, Telemetry telemetry, CdpAccessKeyType cdpAccessKeyType) {
        return ThreadBasedUserCrnProvider.doAsInternalActor(
                () -> altusIAMService.generateDatabusMachineUserWithAccessKey(
                        new MachineUserRequest()
                                .setName(getFluentDatabusMachineUserName(stack))
                                .setAccountId(Crn.safeFromString(stack.getResourceCrn()).getAccountId())
                                .setActorCrn(ThreadBasedUserCrnProvider.getUserCrn())
                                .setCdpAccessKeyType(cdpAccessKeyType),
                        telemetry.isUseSharedAltusCredentialEnabled()));
    }

    public Optional<AltusCredential> generateMonitoringMachineUser(StackView stack, Telemetry telemetry, CdpAccessKeyType cdpAccessKeyType) {
        String accountId = Crn.safeFromString(stack.getResourceCrn()).getAccountId();
        if (telemetry.isComputeMonitoringEnabled()) {
            return ThreadBasedUserCrnProvider.doAsInternalActor(
                    () -> altusIAMService.generateMonitoringMachineUserWithAccessKey(new MachineUserRequest()
                                    .setName(getMonitoringMachineUserName(stack))
                                    .setAccountId(accountId)
                                    .setActorCrn(ThreadBasedUserCrnProvider.getUserCrn())
                                    .setCdpAccessKeyType(cdpAccessKeyType),
                            telemetry.isUseSharedAltusCredentialEnabled()));
        }
        return Optional.empty();
    }

    public void clearFluentMachineUser(StackView stack, ClusterView cluster, Telemetry telemetry) {
        if (isDataBusCredentialAvailable(cluster)) {
            String machineUserName = getFluentDatabusMachineUserName(stack);
            ThreadBasedUserCrnProvider.doAsInternalActor(
                    () -> altusIAMService.clearMachineUser(machineUserName, Crn.safeFromString(stack.getResourceCrn()).getAccountId(),
                            telemetry.isUseSharedAltusCredentialEnabled()));
        }
    }

    public void clearMonitoringMachineUser(StackView stack, ClusterView cluster, Telemetry telemetry) {
        String accountId = Crn.safeFromString(stack.getResourceCrn()).getAccountId();
        if (telemetry.isComputeMonitoringEnabled() || isMonitoringCredentialAvailable(cluster)) {
            String machineUserName = getMonitoringMachineUserName(stack);
            ThreadBasedUserCrnProvider.doAsInternalActor(
                    () -> altusIAMService.clearMachineUser(machineUserName, accountId));
        }
    }

    /**
     * Delete machine user with access keys (and unassign databus role if required) by provided machine user name and account id
     */
    public void cleanupMachineUser(String machineUserName, String accountId) {
        ThreadBasedUserCrnProvider.doAsInternalActor(
                () -> altusIAMService.clearMachineUser(machineUserName, accountId)
        );
    }

    public DataBusCredential getOrCreateDataBusCredentialIfNeeded(Long stackId) throws IOException {
        LOGGER.debug("Get or create databus credential for stack");
        StackDto stackDto = stackDtoService.getById(stackId);
        return getDataBusCredentialIfNeededByStack(stackDto);
    }

    private DataBusCredential getDataBusCredentialIfNeededByStack(StackDto stackDto) throws IOException {
        StackView stack = stackDto.getStack();
        ClusterView cluster = stackDto.getCluster();
        cluster.getDatabusCredential();
        Telemetry telemetry = componentConfigProviderService.getTelemetry(stackDto.getId());
        if (cluster.getDatabusCredential() != null) {
            LOGGER.debug("Databus credential has been found for the stack");
            DataBusCredential credential = new Json(cluster.getDatabusCredential()).get(DataBusCredential.class);
            if (isCredentialExist(telemetry, Crn.safeFromString(stack.getResourceCrn()).getAccountId(), credential.getMachineUserName(),
                    credential.getAccessKey())) {
                LOGGER.debug("Databus credential exists both in the stack and on UMS side");
                return credential;
            } else {
                LOGGER.debug("Databus credential exists on stack side but does not exists on UMS side, it will be updated ...");
            }
        } else {
            LOGGER.debug("Databus credential does not exist for the stack, it will be created ...");
        }
        CdpAccessKeyType cdpAccessKeyType = getCdpAccessKeyType(stackDto);
        Optional<AltusCredential> altusCredential = generateDatabusMachineUserForFluent(stack, telemetry, cdpAccessKeyType);
        return storeDataBusCredential(altusCredential, stack, cdpAccessKeyType);
    }

    public CdpAccessKeyType getCdpAccessKeyType(StackDto stackDto) {
        if (AwsConstants.AwsVariant.AWS_NATIVE_GOV_VARIANT.variant().value().equals(stackDto.getPlatformVariant())) {
            Optional<Image> image = componentConfigProviderService.findImage(stackDto.getId());
            if (image.isEmpty()) {
                throw new CloudbreakRuntimeException("ECDSA is mandatory on AWS gov deployment, but we could not get image package versions, " +
                        "so we can't decide if the selected image supports it.");
            } else {
                if (telemetryFeatureService.isECDSAAccessKeyTypeSupported(image.get().getPackageVersions())) {
                    return CdpAccessKeyType.ECDSA;
                } else {
                    throw new CloudbreakRuntimeException("ECDSA is mandatory on AWS gov deployment, " +
                            "but the image contains packages which can't support ECDSA key");
                }
            }
        } else {
            return CdpAccessKeyType.ED25519;
        }
    }

    /**
     * Store databus access / secret keypair and machine user name in the cluster if altus credential exists
     *
     * @param altusCredential dto for databus access/private key
     * @param stack           component will be attached to this stack
     * @return domain object that holds databus credential
     */
    public DataBusCredential storeDataBusCredential(Optional<AltusCredential> altusCredential, StackView stack, CdpAccessKeyType cdpAccessKeyType) {
        if (altusCredential.isPresent()) {
            DataBusCredential dataBusCredential = getDataBusCredential(altusCredential.get(), stack, cdpAccessKeyType);
            String databusCredentialJsonString = new Json(dataBusCredential).getValue();
            if (stack.getClusterId() != null) {
                clusterService.updateDatabusCredentialByClusterId(stack.getClusterId(), databusCredentialJsonString);
            }
            return dataBusCredential;
        }
        return null;
    }

    public DataBusCredential getDataBusCredential(AltusCredential altusCredential, StackView stack, CdpAccessKeyType cdpAccessKeyType) {
        DataBusCredential dataBusCredential = new DataBusCredential();
        dataBusCredential.setMachineUserName(getFluentDatabusMachineUserName(stack));
        dataBusCredential.setAccessKey(altusCredential.getAccessKey());
        dataBusCredential.setPrivateKey(altusCredential.getPrivateKey() != null ? new String(altusCredential.getPrivateKey()) : null);
        dataBusCredential.setAccessKeyType(cdpAccessKeyType.getValue());
        return dataBusCredential;
    }

    public MonitoringCredential storeMonitoringCredential(Optional<AltusCredential> altusCredential, StackView stack, CdpAccessKeyType cdpAccessKeyType) {
        if (altusCredential.isPresent()) {
            MonitoringCredential monitoringCredential = new MonitoringCredential();
            monitoringCredential.setMachineUserName(getMonitoringMachineUserName(stack));
            monitoringCredential.setAccessKey(altusCredential.get().getAccessKey());
            monitoringCredential.setPrivateKey(altusCredential.get().getPrivateKey() != null ? new String(altusCredential.get().getPrivateKey()) : null);
            monitoringCredential.setAccessKeyType(cdpAccessKeyType.getValue());
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
                () -> altusIAMService.doesMachineUserHasAccessKey(
                        ThreadBasedUserCrnProvider.getUserCrn(),
                        accountId,
                        machineUserName,
                        accessKey,
                        telemetry.isUseSharedAltusCredentialEnabled()));
    }

    public boolean isAnyMonitoringFeatureSupported(Telemetry telemetry) {
        return telemetry != null && telemetry.isComputeMonitoringEnabled() && telemetry.isMonitoringFeatureEnabled();
    }

    public List<UserManagementProto.MachineUser> getAllInternalMachineUsers(String accountId) {
        return ThreadBasedUserCrnProvider.doAsInternalActor(
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

    @Override
    public DatabusCredentialView getOrCreateDatabusCredential(String crn) throws IOException {
        LOGGER.debug("Get or create databus credential for stack");
        StackDto stackDto = stackDtoService.getByCrn(crn);
        return new DatabusCredentialView(getDataBusCredentialIfNeededByStack(stackDto));
    }
}