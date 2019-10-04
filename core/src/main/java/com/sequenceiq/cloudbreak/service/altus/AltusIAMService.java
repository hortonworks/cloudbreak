package com.sequenceiq.cloudbreak.service.altus;

import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.cloudera.thunderhead.service.usermanagement.UserManagementProto;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.StackType;
import com.sequenceiq.cloudbreak.auth.altus.Crn;
import com.sequenceiq.cloudbreak.auth.altus.GrpcUmsClient;
import com.sequenceiq.cloudbreak.auth.altus.model.AltusCredential;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.common.api.telemetry.model.Telemetry;

@Service
public class AltusIAMService {

    private static final Logger LOGGER = LoggerFactory.getLogger(AltusIAMService.class);

    private static final String FLUENT_DATABUS_MACHINE_USER_NAME_PATTERN = "%s-fluent-databus-uploader-%s";

    private final GrpcUmsClient umsClient;

    public AltusIAMService(GrpcUmsClient umsClient) {
        this.umsClient = umsClient;
    }

    /**
     * Generate machine user for fluentd - databus communication
     */
    public Optional<AltusCredential> generateDatabusMachineUserForFluent(Stack stack, Telemetry telemetry) {
        if (isMeteringOrDeploymentReportingSupported(stack, telemetry)) {
            return Optional.of(umsClient.createMachineUserAndGenerateKeys(
                    getFluentDatabusMachineUserName(stack),
                    stack.getCreator().getUserCrn(),
                    umsClient.getBuiltInDatabusRoleCrn(),
                    UserManagementProto.AccessKeyType.Value.ED25519));
        }
        return Optional.empty();
    }

    /**
     * Delete machine user with its access keys (and unassign databus role if required)
     */
    public void clearFluentMachineUser(Stack stack, Telemetry telemetry) {
        if (isMeteringOrDeploymentReportingSupported(stack, telemetry)) {
            try {
                String machineUserName = getFluentDatabusMachineUserName(stack);
                String userCrn = stack.getCreator().getUserCrn();
                umsClient.clearMachineUserWithAccessKeysAndRole(machineUserName, userCrn, umsClient.getBuiltInDatabusRoleCrn());
            } catch (Exception e) {
                LOGGER.warn("Cluster Databus resource cleanup failed (fluent - databus user). It is not a fatal issue, "
                        + "but note that you could have remaining UMS resources for your account", e);
            }
        }
    }

    // for datalake metering is not supported/required right now
    private boolean isMeteringOrDeploymentReportingSupported(Stack stack, Telemetry telemetry) {
        return telemetry != null && (telemetry.isReportDeploymentLogsFeatureEnabled() || (telemetry.isMeteringFeatureEnabled()
                && !StackType.DATALAKE.equals(stack.getType())));
    }

    private String getFluentDatabusMachineUserName(Stack stack) {
        String clusterType = "cb";
        if (StackType.DATALAKE.equals(stack.getType())) {
            clusterType = "datalake";
        } else if (StackType.WORKLOAD.equals(stack.getType())) {
            clusterType = "datahub";
        }
        return String.format(FLUENT_DATABUS_MACHINE_USER_NAME_PATTERN, clusterType,
                Crn.fromString(stack.getResourceCrn()).getResource());
    }
}
