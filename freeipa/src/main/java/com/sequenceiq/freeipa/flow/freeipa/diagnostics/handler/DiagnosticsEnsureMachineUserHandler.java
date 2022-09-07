package com.sequenceiq.freeipa.flow.freeipa.diagnostics.handler;

import static com.sequenceiq.freeipa.flow.freeipa.diagnostics.event.DiagnosticsCollectionHandlerSelectors.ENSURE_MACHINE_USER_EVENT;
import static com.sequenceiq.freeipa.flow.freeipa.diagnostics.event.DiagnosticsCollectionStateSelectors.START_DIAGNOSTICS_COLLECTION_EVENT;

import java.util.Map;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.cloudera.thunderhead.service.common.usage.UsageProto;
import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.auth.altus.model.CdpAccessKeyType;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.telemetry.TelemetryFeatureService;
import com.sequenceiq.cloudbreak.telemetry.UMSSecretKeyFormatter;
import com.sequenceiq.common.api.telemetry.model.DataBusCredential;
import com.sequenceiq.common.api.telemetry.model.DiagnosticsDestination;
import com.sequenceiq.common.model.diagnostics.DiagnosticParameters;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.image.Image;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.flow.freeipa.diagnostics.event.DiagnosticsCollectionEvent;
import com.sequenceiq.freeipa.service.AltusMachineUserService;
import com.sequenceiq.freeipa.service.image.ImageService;
import com.sequenceiq.freeipa.service.stack.StackService;

@Component
public class DiagnosticsEnsureMachineUserHandler extends AbstractDiagnosticsOperationHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(DiagnosticsEnsureMachineUserHandler.class);

    @Inject
    private StackService stackService;

    @Inject
    private ImageService imageService;

    @Inject
    private AltusMachineUserService altusMachineUserService;

    @Inject
    private EntitlementService entitlementService;

    @Inject
    private TelemetryFeatureService telemetryFeatureService;

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
                CdpAccessKeyType cdpAccessKeyType = getCdpAccessKeyType(resourceId);
                DataBusCredential credential = altusMachineUserService.getOrCreateDataBusCredentialIfNeeded(resourceId, cdpAccessKeyType);
                parameters.setSupportBundleDbusAccessKey(credential.getAccessKey());
                parameters.setSupportBundleDbusPrivateKey(UMSSecretKeyFormatter.formatSecretKey(credential.getAccessKeyType(), credential.getPrivateKey()));
                parameters.setSupportBundleDbusAccessKeyType(credential.getAccessKeyType());
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

    private CdpAccessKeyType getCdpAccessKeyType(Long resourceId) {
        Stack stack = stackService.getStackById(resourceId);
        Image image = imageService.getImageForStack(stack);
        if (!entitlementService.isECDSABasedAccessKeyEnabled(stack.getAccountId())) {
            return CdpAccessKeyType.ED25519;
        }
        return telemetryFeatureService.isECDSAAccessKeyTypeSupported(image.getPackageVersions()) ? CdpAccessKeyType.ECDSA : CdpAccessKeyType.ED25519;
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
