package com.sequenceiq.freeipa.service.diagnostics;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.common.service.TransactionService;
import com.sequenceiq.cloudbreak.telemetry.DataBusEndpointProvider;
import com.sequenceiq.cloudbreak.telemetry.converter.DiagnosticsDataToParameterConverter;
import com.sequenceiq.common.api.diagnostics.ListDiagnosticsCollectionResponse;
import com.sequenceiq.common.model.diagnostics.DiagnosticParameters;
import com.sequenceiq.flow.api.model.FlowIdentifier;
import com.sequenceiq.flow.core.Flow2Handler;
import com.sequenceiq.flow.core.FlowConstants;
import com.sequenceiq.flow.domain.ClassValue;
import com.sequenceiq.flow.domain.FlowLog;
import com.sequenceiq.flow.service.flowlog.FlowLogDBService;
import com.sequenceiq.freeipa.api.v1.diagnostics.model.DiagnosticsCollectionRequest;
import com.sequenceiq.freeipa.converter.diagnostics.FlowLogsToListDiagnosticsCollectionResponseConverter;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.flow.freeipa.diagnostics.config.DiagnosticsCollectionFlowConfig;
import com.sequenceiq.freeipa.flow.freeipa.diagnostics.event.DiagnosticsCollectionEvent;
import com.sequenceiq.freeipa.flow.freeipa.diagnostics.event.DiagnosticsCollectionStateSelectors;
import com.sequenceiq.freeipa.service.freeipa.flow.FreeIpaFlowManager;
import com.sequenceiq.freeipa.service.stack.StackService;

import reactor.bus.Event;
import reactor.rx.Promise;

@Service
public class DiagnosticsService {

    private static final Logger LOGGER = LoggerFactory.getLogger(DiagnosticsService.class);

    private static final String FREEIPA_CLUSTER_TYPE = "FREEIPA";

    @Inject
    private StackService stackService;

    @Inject
    private FreeIpaFlowManager flowManager;

    @Inject
    private DiagnosticsCollectionValidator diagnosticsCollectionValidator;

    @Inject
    private DiagnosticsDataToParameterConverter diagnosticsDataToParameterConverter;

    @Inject
    private EntitlementService entitlementService;

    @Inject
    private DataBusEndpointProvider dataBusEndpointProvider;

    @Inject
    private FlowLogDBService flowLogDBService;

    @Inject
    private Flow2Handler flow2Handler;

    @Inject
    private FlowLogsToListDiagnosticsCollectionResponseConverter flowLogsToListDiagnosticsCollectionResponseConverter;

    public ListDiagnosticsCollectionResponse getDiagnosticsCollections(String environmentCrn) {
        List<FlowLog> flowLogs = flowLogDBService.getLatestFlowLogsByCrnAndType(environmentCrn, ClassValue.of(DiagnosticsCollectionFlowConfig.class));
        return flowLogsToListDiagnosticsCollectionResponseConverter.convert(flowLogs);
    }

    public void cancelDiagnosticsCollections(String environmentCrn) {
        List<FlowLog> flowLogs = flowLogDBService.getLatestFlowLogsByCrnAndType(environmentCrn, ClassValue.of(DiagnosticsCollectionFlowConfig.class));
        flowLogs.stream()
                .filter(f -> !f.getFinalized())
                .forEach(cancelFlow());
    }

    private Consumer<FlowLog> cancelFlow() {
        return f -> {
            try {
                flow2Handler.cancelFlow(f.getResourceId(), f.getFlowId());
            } catch (TransactionService.TransactionExecutionException e) {
                String errorMessage = String.format("Transaction error during cancelling diagnostics flow [freeipa_stack_id: %s] [flow_id: %s]",
                        f.getResourceId(), f.getFlowId());
                LOGGER.debug(errorMessage, e);
                throw new CloudbreakServiceException(errorMessage);
            }
        };
    }

    public FlowIdentifier startDiagnosticsCollection(DiagnosticsCollectionRequest request, String accountId, String userCrn) {
        Stack stack = stackService.getByEnvironmentCrnAndAccountIdWithListsAndMdcContext(request.getEnvironmentCrn(), accountId);
        diagnosticsCollectionValidator.validate(request, stack);
        LOGGER.debug("Starting diagnostics collection for FreeIpa. Crn: '{}'", stack.getResourceCrn());
        boolean useDbusCnameEndpoint = entitlementService.useDataBusCNameEndpointEnabled(stack.getAccountId());
        String databusEndpoint = dataBusEndpointProvider.getDataBusEndpoint(stack.getTelemetry().getDatabusEndpoint(), useDbusCnameEndpoint);
        DiagnosticParameters parameters = diagnosticsDataToParameterConverter.convert(request, stack.getTelemetry(), FREEIPA_CLUSTER_TYPE,
                stack.getAppVersion(), stack.getAccountId(), stack.getRegion(), databusEndpoint);
        DiagnosticsCollectionEvent diagnosticsCollectionEvent = DiagnosticsCollectionEvent.builder()
                .withAccepted(new Promise<>())
                .withResourceId(stack.getId())
                .withResourceCrn(stack.getResourceCrn())
                .withSelector(DiagnosticsCollectionStateSelectors.START_DIAGNOSTICS_SALT_VALIDATION_EVENT.selector())
                .withParameters(parameters)
                .build();
        return flowManager.notify(diagnosticsCollectionEvent, getFlowHeaders(userCrn));
    }

    private Event.Headers getFlowHeaders(String userCrn) {
        return new Event.Headers(Map.of(FlowConstants.FLOW_TRIGGER_USERCRN, userCrn));
    }
}
