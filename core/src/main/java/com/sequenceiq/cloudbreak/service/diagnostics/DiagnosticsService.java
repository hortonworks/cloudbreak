package com.sequenceiq.cloudbreak.service.diagnostics;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.auth.crn.Crn;
import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.common.service.TransactionService;
import com.sequenceiq.cloudbreak.controller.validation.diagnostics.DiagnosticsCollectionValidator;
import com.sequenceiq.cloudbreak.converter.v4.diagnostics.FlowLogsToListDiagnosticsCollectionResponseConverter;
import com.sequenceiq.cloudbreak.core.flow2.diagnostics.config.DiagnosticsCollectionFlowConfig;
import com.sequenceiq.cloudbreak.core.flow2.cmdiagnostics.event.CmDiagnosticsCollectionEvent;
import com.sequenceiq.cloudbreak.core.flow2.cmdiagnostics.event.CmDiagnosticsCollectionStateSelectors;
import com.sequenceiq.cloudbreak.core.flow2.diagnostics.event.DiagnosticsCollectionEvent;
import com.sequenceiq.cloudbreak.core.flow2.diagnostics.event.DiagnosticsCollectionStateSelectors;
import com.sequenceiq.cloudbreak.core.flow2.service.ReactorNotifier;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.cloudbreak.service.ComponentConfigProviderService;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.cloudbreak.telemetry.DataBusEndpointProvider;
import com.sequenceiq.cloudbreak.telemetry.converter.CmDiagnosticsDataToParameterConverter;
import com.sequenceiq.cloudbreak.telemetry.converter.DiagnosticsDataToParameterConverter;
import com.sequenceiq.common.api.diagnostics.BaseCmDiagnosticsCollectionRequest;
import com.sequenceiq.common.api.diagnostics.BaseDiagnosticsCollectionRequest;
import com.sequenceiq.common.api.diagnostics.ListDiagnosticsCollectionResponse;
import com.sequenceiq.common.api.telemetry.model.Telemetry;
import com.sequenceiq.common.model.diagnostics.CmDiagnosticsParameters;
import com.sequenceiq.common.model.diagnostics.DiagnosticParameters;
import com.sequenceiq.flow.api.model.FlowIdentifier;
import com.sequenceiq.flow.core.Flow2Handler;
import com.sequenceiq.flow.core.FlowConstants;
import com.sequenceiq.flow.domain.FlowLog;
import com.sequenceiq.flow.service.flowlog.FlowLogDBService;

import reactor.bus.Event;
import reactor.rx.Promise;

@Service
public class DiagnosticsService {

    private static final Logger LOGGER = LoggerFactory.getLogger(DiagnosticsService.class);

    @Value("${info.app.version:}")
    private String version;

    @Inject
    private StackService stackService;

    @Inject
    private ReactorNotifier reactorNotifier;

    @Inject
    private DiagnosticsDataToParameterConverter diagnosticsDataToParameterConverter;

    @Inject
    private CmDiagnosticsDataToParameterConverter cmDiagnosticsDataToParameterConverter;

    @Inject
    private ComponentConfigProviderService componentConfigProviderService;

    @Inject
    private DiagnosticsCollectionValidator diagnosticsCollectionValidator;

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

    public ListDiagnosticsCollectionResponse getDiagnosticsCollections(String stackCrn) {
        List<FlowLog> flowLogs = flowLogDBService.getLatestFlowLogsByCrnAndType(stackCrn, DiagnosticsCollectionFlowConfig.class);
        return flowLogsToListDiagnosticsCollectionResponseConverter.convert(flowLogs);
    }

    public void cancelDiagnosticsCollections(String stackCrn) {
        List<FlowLog> flowLogs = flowLogDBService.getLatestFlowLogsByCrnAndType(stackCrn, DiagnosticsCollectionFlowConfig.class);
        flowLogs.stream()
                .filter(f -> !f.getFinalized())
                .forEach(cancelFlow());
    }

    private Consumer<FlowLog> cancelFlow() {
        return f -> {
            try {
                flow2Handler.cancelFlow(f.getResourceId(), f.getFlowId());
            } catch (TransactionService.TransactionExecutionException e) {
                String errorMessage = String.format("Transaction error during cancelling diagnostics flow [stack_id: %s] [flow_id: %s]",
                        f.getResourceId(), f.getFlowId());
                LOGGER.debug(errorMessage, e);
                throw new CloudbreakServiceException(errorMessage);
            }
        };
    }

    public FlowIdentifier startDiagnosticsCollection(BaseDiagnosticsCollectionRequest request, String stackCrn,
            String userCrn, String uuid) {
        Stack stack = stackService.getByCrn(stackCrn);
        MDCBuilder.buildMdcContext(stack);
        LOGGER.debug("Starting diagnostics collection for Stack. Crn: '{}'", stack.getResourceCrn());
        Telemetry telemetry = componentConfigProviderService.getTelemetry(stack.getId());
        diagnosticsCollectionValidator.validate(request, stack, telemetry);
        String clusterVersion = version;
        if (stack.getCluster() != null && stack.getCluster().getBlueprint() != null
                && StringUtils.isNotBlank(stack.getCluster().getBlueprint().getStackVersion())) {
            clusterVersion = stack.getCluster().getBlueprint().getStackVersion();
        }
        String accountId = Crn.fromString(stack.getResourceCrn()).getAccountId();
        boolean useDbusCnameEndpoint = entitlementService.useDataBusCNameEndpointEnabled(accountId);
        String databusEndpoint = dataBusEndpointProvider.getDataBusEndpoint(telemetry.getDatabusEndpoint(), useDbusCnameEndpoint);
        DiagnosticParameters parameters = diagnosticsDataToParameterConverter.convert(request, telemetry,
                StringUtils.upperCase(stack.getType().getResourceType()), clusterVersion,
                accountId, stack.getRegion(), databusEndpoint);
        // TODO: use DiagnosticParameters builder for UUID + decrease parameters in the converter above
        parameters.setUuid(uuid);
        DiagnosticsCollectionEvent diagnosticsCollectionEvent = DiagnosticsCollectionEvent.builder()
                .withAccepted(new Promise<>())
                .withResourceId(stack.getId())
                .withResourceCrn(stack.getResourceCrn())
                .withSelector(DiagnosticsCollectionStateSelectors.START_DIAGNOSTICS_SALT_VALIDATION_EVENT.selector())
                .withParameters(parameters)
                .withHosts(parameters.getHosts())
                .withHostGroups(parameters.getHostGroups())
                .withExcludeHosts(parameters.getExcludeHosts())
                .build();
        return reactorNotifier.notify(diagnosticsCollectionEvent, getFlowHeaders(userCrn));
    }

    public FlowIdentifier startDiagnosticsCollection(BaseDiagnosticsCollectionRequest request, String stackCrn,
            String userCrn) {
        return startDiagnosticsCollection(request, stackCrn, userCrn, null);
    }

    public FlowIdentifier startCmDiagnostics(BaseCmDiagnosticsCollectionRequest request, String stackCrn, String userCrn) {
        Stack stack = stackService.getByCrn(stackCrn);
        MDCBuilder.buildMdcContext(stack);
        LOGGER.debug("Starting CM based diagnostics collection for Stack. Crn: '{}'", stack.getResourceCrn());
        Telemetry telemetry = componentConfigProviderService.getTelemetry(stack.getId());
        diagnosticsCollectionValidator.validate(request, stack, telemetry);
        CmDiagnosticsParameters parameters = cmDiagnosticsDataToParameterConverter.convert(request, telemetry, stack.getName(), stack.getRegion());
        CmDiagnosticsCollectionEvent diagnosticsCollectionEvent = CmDiagnosticsCollectionEvent.builder()
                .withAccepted(new Promise<>())
                .withResourceId(stack.getId())
                .withResourceCrn(stack.getResourceCrn())
                .withSelector(CmDiagnosticsCollectionStateSelectors.START_CM_DIAGNOSTICS_INIT_EVENT.selector())
                .withParameters(parameters)
                .build();
        return reactorNotifier.notify(diagnosticsCollectionEvent, getFlowHeaders(userCrn));
    }

    private Event.Headers getFlowHeaders(String userCrn) {
        return new Event.Headers(Map.of(FlowConstants.FLOW_TRIGGER_USERCRN, userCrn));
    }
}
