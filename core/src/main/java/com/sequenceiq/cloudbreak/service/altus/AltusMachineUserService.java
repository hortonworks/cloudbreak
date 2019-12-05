package com.sequenceiq.cloudbreak.service.altus;

import java.util.Optional;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.StackType;
import com.sequenceiq.cloudbreak.auth.altus.Crn;
import com.sequenceiq.cloudbreak.auth.altus.model.AltusCredential;
import com.sequenceiq.cloudbreak.auth.altus.service.AltusIAMService;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.common.api.telemetry.model.Telemetry;

@Component
public class AltusMachineUserService {

    private static final String FLUENT_DATABUS_MACHINE_USER_NAME_PATTERN = "%s-fluent-databus-uploader-%s";

    private final AltusIAMService altusIAMService;

    public AltusMachineUserService(AltusIAMService altusIAMService) {
        this.altusIAMService = altusIAMService;
    }

    /**
     * Generate machine user for fluentd - databus communication
     */
    public Optional<AltusCredential> generateDatabusMachineUserForFluent(Stack stack, Telemetry telemetry) {
        if (isMeteringOrDeploymentReportingSupported(stack, telemetry)) {
            return altusIAMService.generateMachineUserWithAccessKey(
                    getFluentDatabusMachineUserName(stack), stack.getCreator().getUserCrn(),
                    telemetry.isUseSharedAltusCredentialEnabled());
        }
        return Optional.empty();
    }

    /**
     * Delete machine user with its access keys (and unassign databus role if required)
     */
    public void clearFluentMachineUser(Stack stack, Telemetry telemetry) {
        if (isMeteringOrDeploymentReportingSupported(stack, telemetry)) {
            String machineUserName = getFluentDatabusMachineUserName(stack);
            String userCrn = stack.getCreator().getUserCrn();
            altusIAMService.clearMachineUser(machineUserName, userCrn, telemetry.isUseSharedAltusCredentialEnabled());
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