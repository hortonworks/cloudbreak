package com.sequenceiq.freeipa.flow.freeipa.diagnostics.handler;

import static com.sequenceiq.freeipa.flow.freeipa.diagnostics.event.DiagnosticsCollectionHandlerSelectors.ENSURE_MACHINE_USER_EVENT;
import static com.sequenceiq.freeipa.flow.freeipa.diagnostics.event.DiagnosticsCollectionStateSelectors.START_DIAGNOSTICS_COLLECTION_EVENT;

import java.util.Map;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.cloudera.thunderhead.service.common.usage.UsageProto;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.common.api.telemetry.model.DataBusCredential;
import com.sequenceiq.common.api.telemetry.model.DiagnosticsDestination;
import com.sequenceiq.common.model.diagnostics.DiagnosticParameters;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;
import com.sequenceiq.freeipa.flow.freeipa.diagnostics.event.DiagnosticsCollectionEvent;
import com.sequenceiq.freeipa.service.AltusMachineUserService;

@Component
public class DiagnosticsEnsureMachineUserHandler extends AbstractDiagnosticsOperationHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(DiagnosticsEnsureMachineUserHandler.class);

    @Inject
    private AltusMachineUserService altusMachineUserService;

    @Override
    public Selectable executeOperation(HandlerEvent<DiagnosticsCollectionEvent> event) throws Exception {
        DiagnosticsCollectionEvent data = event.getData();
        Long resourceId = data.getResourceId();
        String resourceCrn = data.getResourceCrn();
        DiagnosticParameters parameters = data.getParameters();
        Map<String, Object> parameterMap = parameters.toMap();
        try {
            LOGGER.debug("Diagnostics collection ensure machine user operation started. resourceCrn: '{}', parameters: '{}'",
                    resourceCrn, parameterMap);
            if (DiagnosticsDestination.SUPPORT.equals(parameters.getDestination())) {
                LOGGER.debug("Generating databus credential if required for diagnostics support destination.");
                DataBusCredential credential = altusMachineUserService.getOrCreateDataBusCredentialIfNeeded(resourceId);
                parameters.setSupportBundleDbusAccessKey(credential.getAccessKey());
                parameters.setSupportBundleDbusPrivateKey(credential.getPrivateKey());
            }
            return DiagnosticsCollectionEvent.builder()
                    .withResourceCrn(resourceCrn)
                    .withResourceId(resourceId)
                    .withSelector(START_DIAGNOSTICS_COLLECTION_EVENT.selector())
                    .withParameters(parameters)
                    .build();
        } catch (Exception e) {
            throw new CloudbreakServiceException(e);
        }
    }

    @Override
    public UsageProto.CDPVMDiagnosticsFailureType.Value getFailureType() {
        return UsageProto.CDPVMDiagnosticsFailureType.Value.UMS_RESOURCE_CHECK_FAILURE;
    }

    @Override
    public String getOperationName() {
        return "UMS resource check";
    }

    @Override
    public String selector() {
        return ENSURE_MACHINE_USER_EVENT.selector();
    }
}
