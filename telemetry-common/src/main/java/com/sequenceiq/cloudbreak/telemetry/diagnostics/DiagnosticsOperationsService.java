package com.sequenceiq.cloudbreak.telemetry.diagnostics;

import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.cloudera.thunderhead.service.common.usage.UsageProto;
import com.sequenceiq.cloudbreak.common.orchestration.Node;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorFailedException;
import com.sequenceiq.cloudbreak.orchestrator.host.TelemetryOrchestrator;
import com.sequenceiq.cloudbreak.orchestrator.metadata.OrchestratorMetadata;
import com.sequenceiq.cloudbreak.orchestrator.metadata.OrchestratorMetadataFilter;
import com.sequenceiq.cloudbreak.orchestrator.metadata.OrchestratorMetadataProvider;
import com.sequenceiq.cloudbreak.usage.UsageReporter;
import com.sequenceiq.common.api.telemetry.model.DiagnosticsDestination;
import com.sequenceiq.common.model.diagnostics.DiagnosticParameters;

@Component
public class DiagnosticsOperationsService {

    private static final Logger LOGGER = LoggerFactory.getLogger(DiagnosticsOperationsService.class);

    private static final Integer ERROR_MESSAGE_MAX_LENGTH = 1000;

    @Inject
    private TelemetryOrchestrator telemetryOrchestrator;

    @Inject
    private UsageReporter usageReporter;

    @Inject
    private OrchestratorMetadataProvider orchestratorMetadataProvider;

    public void init(Long stackId, DiagnosticParameters parameters) throws CloudbreakOrchestratorFailedException {
        executeDiagnosticsOperation(stackId, parameters, "init", (m, p) -> telemetryOrchestrator.initDiagnosticCollection(
                m.getGatewayConfigs(), m.getNodes(), p.toMap(), m.getExitCriteriaModel()));
    }

    public void collect(Long stackId, DiagnosticParameters parameters) throws CloudbreakOrchestratorFailedException {
        executeDiagnosticsOperation(stackId, parameters, "collect", (m, p) -> telemetryOrchestrator.executeDiagnosticCollection(
                m.getGatewayConfigs(), m.getNodes(), p.toMap(), m.getExitCriteriaModel()));
    }

    public void upload(Long stackId, DiagnosticParameters parameters) throws CloudbreakOrchestratorFailedException {
        executeDiagnosticsOperation(stackId, parameters, "upload", (m, p) -> telemetryOrchestrator.uploadCollectedDiagnostics(
                m.getGatewayConfigs(), m.getNodes(), p.toMap(), m.getExitCriteriaModel()));
    }

    public void cleanup(Long stackId, DiagnosticParameters parameters) throws CloudbreakOrchestratorFailedException {
        executeDiagnosticsOperation(stackId, parameters, "cleanup", (m, p) -> telemetryOrchestrator.cleanupCollectedDiagnostics(
                m.getGatewayConfigs(), m.getNodes(), p.toMap(), m.getExitCriteriaModel()));
    }

    public void vmPreflightCheck(Long stackId, DiagnosticParameters parameters) throws CloudbreakOrchestratorFailedException {
        executeDiagnosticsOperation(stackId, parameters, "vm preflight check", (m, p) -> telemetryOrchestrator.preFlightDiagnosticsCheck(
                m.getGatewayConfigs(), m.getNodes(), p.toMap(), m.getExitCriteriaModel()));
    }

    public DiagnosticParameters applyUnresponsiveHosts(Long stackId, DiagnosticParameters parameters)
            throws CloudbreakOrchestratorFailedException {
        Set<String> unresponsiveHosts = collectUnresponsiveHosts(stackId, parameters);
        LOGGER.debug("Diagnostics collection salt validation operation has been started. parameters: '{}' stack id: {} ", parameters.toMap(), stackId);
        if (CollectionUtils.isNotEmpty(unresponsiveHosts)) {
            if (parameters.getSkipUnresponsiveHosts()) {
                parameters.getExcludeHosts().addAll(unresponsiveHosts);
                LOGGER.debug("Diagnostics parameters has been updated with excluded hosts. parameters: '{}', stack id: {}", parameters.toMap(), stackId);
                return parameters;
            } else {
                throw new CloudbreakOrchestratorFailedException(
                        String.format("Some of the hosts are unresponsive, check the states of salt-minions on the following nodes: %s",
                                StringUtils.join(unresponsiveHosts, ',')));
            }
        } else {
            LOGGER.debug("Not found any unresponsive hosts.");
            return parameters;
        }
    }

    public void vmDiagnosticsReport(String resourceCrn, DiagnosticParameters parameters) {
        vmDiagnosticsReport(resourceCrn, parameters, null, null);
    }

    public void vmDiagnosticsReport(String resourceCrn, DiagnosticParameters parameters, UsageProto.CDPVMDiagnosticsFailureType.Value failureType,
            Exception exception) {
        if (parameters != null) {
            sendCdpDiagEvent(resourceCrn, parameters, failureType, exception);
        } else {
            LOGGER.debug("Skip sending diagnostics report as diagnostic parameter input is empty.");
        }
    }

    private void sendCdpDiagEvent(String resourceCrn, DiagnosticParameters parameters, UsageProto.CDPVMDiagnosticsFailureType.Value failureType,
            Exception exception) {
        UsageProto.CDPVMDiagnosticsEvent.Builder eventBuilder = UsageProto.CDPVMDiagnosticsEvent.newBuilder();
        if (exception == null) {
            eventBuilder.setResult(UsageProto.CDPVMDiagnosticsResult.Value.SUCCESSFUL);
        } else {
            eventBuilder.setFailureMessage(StringUtils.left(exception.getMessage(), ERROR_MESSAGE_MAX_LENGTH));
            eventBuilder.setResult(UsageProto.CDPVMDiagnosticsResult.Value.FAILED);
        }
        setIfNotNull(eventBuilder::setFailureType, failureType);
        setIfNotNull(eventBuilder::setUuid, parameters.getUuid());
        setIfNotNull(eventBuilder::setDescription, parameters.getDescription());
        setIfNotNull(eventBuilder::setAccountId, parameters.getAccountId());
        setIfNotNull(eventBuilder::setInputParameters, parameters.toMap().toString());
        setIfNotNull(eventBuilder::setCaseNumber, parameters.getIssue());
        setIfNotNull(eventBuilder::setResourceCrn, resourceCrn);
        UsageProto.CDPVMDiagnosticsDestination.Value dest = convertUsageDestination(parameters.getDestination());
        setIfNotNull(eventBuilder::setDestination, dest);
        usageReporter.cdpVmDiagnosticsEvent(eventBuilder.build());
    }

    private UsageProto.CDPVMDiagnosticsDestination.Value convertUsageDestination(DiagnosticsDestination destination) {
        switch (destination) {
            case LOCAL:
                return UsageProto.CDPVMDiagnosticsDestination.Value.LOCAL;
            case CLOUD_STORAGE:
                return UsageProto.CDPVMDiagnosticsDestination.Value.CLOUD_STORAGE;
            case SUPPORT:
                return UsageProto.CDPVMDiagnosticsDestination.Value.SUPPORT;
            case ENG:
                return UsageProto.CDPVMDiagnosticsDestination.Value.ENGINEERING;
            default:
                return UsageProto.CDPVMDiagnosticsDestination.Value.UNSET;
        }
    }

    private <T> void setIfNotNull(final Consumer<T> setter, final T value) {
        if (value != null) {
            setter.accept(value);
        }
    }

    private void executeDiagnosticsOperation(Long stackId, DiagnosticParameters parameters, String operationName, DiagnosticsOperationFunction func)
            throws CloudbreakOrchestratorFailedException {
        OrchestratorMetadata orchestratorMetadata = orchestratorMetadataProvider.getOrchestratorMetadata(stackId);
        OrchestratorMetadataFilter filter = createMetadataFilter(parameters);
        OrchestratorMetadata updatedOrchestratorMetadata = filter.apply(orchestratorMetadata);
        LOGGER.debug("Starting diagnostics {}. stackId: '{}'", operationName, stackId);
        if (CollectionUtils.isEmpty(updatedOrchestratorMetadata.getNodes())) {
            LOGGER.debug("Diagnostics {} has been skipped. (no target minions)", operationName);
        } else {
            func.apply(updatedOrchestratorMetadata, parameters);
        }
    }

    private Set<String> collectUnresponsiveHosts(Long stackId, DiagnosticParameters parameters) throws CloudbreakOrchestratorFailedException {
        Set<Node> allUnresponsiveNodes = collectUnresponsiveNodes(stackId);
        OrchestratorMetadataFilter filter = createMetadataFilter(parameters);
        return allUnresponsiveNodes.stream()
                .filter(filter::apply)
                .map(Node::getHostname).collect(Collectors.toSet());
    }

    private Set<Node> collectUnresponsiveNodes(Long stackId) throws CloudbreakOrchestratorFailedException {
        OrchestratorMetadata orchestratorMetadata = orchestratorMetadataProvider.getOrchestratorMetadata(stackId);
        return telemetryOrchestrator.collectUnresponsiveNodes(
                orchestratorMetadata.getGatewayConfigs(), orchestratorMetadata.getNodes(), orchestratorMetadata.getExitCriteriaModel());
    }

    private OrchestratorMetadataFilter createMetadataFilter(DiagnosticParameters parameters) {
        return OrchestratorMetadataFilter.Builder.newBuilder()
                .includeHosts(parameters.getHosts())
                .includeHostGroups(parameters.getHostGroups())
                .exlcudeHosts(parameters.getExcludeHosts())
                .build();
    }

}
