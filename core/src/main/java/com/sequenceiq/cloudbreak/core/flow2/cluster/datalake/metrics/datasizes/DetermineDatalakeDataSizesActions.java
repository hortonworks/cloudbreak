package com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.metrics.datasizes;

import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.DetailedStackStatus.DETERMINE_DATALAKE_DATA_SIZES_FAILED;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.DetailedStackStatus.DETERMINE_DATALAKE_DATA_SIZES_FINISHED;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.DetailedStackStatus.DETERMINE_DATALAKE_DATA_SIZES_IN_PROGRESS;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.metrics.datasizes.DetermineDatalakeDataSizesEvent.DETERMINE_DATALAKE_DATA_SIZES_FAILURE_HANDLED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.metrics.datasizes.DetermineDatalakeDataSizesEvent.DETERMINE_DATALAKE_DATA_SIZES_SUBMISSION_SUCCESS_EVENT;

import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.statemachine.action.Action;

import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.crn.RegionAwareInternalCrnGeneratorFactory;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.orchestrator.host.TelemetryOrchestrator;
import com.sequenceiq.cloudbreak.orchestrator.metadata.OrchestratorMetadata;
import com.sequenceiq.cloudbreak.orchestrator.metadata.OrchestratorMetadataProvider;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.datalakemetrics.datasizes.DetermineDatalakeDataSizesBaseEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.datalakemetrics.datasizes.DetermineDatalakeDataSizesFailureEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.datalakemetrics.datasizes.DetermineDatalakeDataSizesSubmissionEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.datalakemetrics.datasizes.GetDatalakeDataSizesRequest;
import com.sequenceiq.cloudbreak.service.StackUpdater;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.cloudbreak.util.CompressUtil;
import com.sequenceiq.sdx.api.endpoint.SdxEndpoint;

@Configuration
public class DetermineDatalakeDataSizesActions {
    @Inject
    private StackUpdater stackUpdater;

    @Inject
    private StackService stackService;

    @Inject
    private SdxEndpoint sdxEndpoint;

    @Inject
    private RegionAwareInternalCrnGeneratorFactory regionAwareInternalCrnGeneratorFactory;

    @Inject
    private OrchestratorMetadataProvider orchestratorMetadataProvider;

    @Inject
    private CompressUtil compressUtil;

    @Inject
    private TelemetryOrchestrator telemetryOrchestrator;

    @Bean(name = "DETERMINE_DATALAKE_DATA_SIZES_IN_PROGRESS_STATE")
    public Action<?, ?> determineDatalakeDataSizesAction() {
        return new AbstractDetermineDatalakeDataSizesAction<>(DetermineDatalakeDataSizesBaseEvent.class) {
            @Override
            protected void doExecute(DetermineDatalakeDataSizesContext context, DetermineDatalakeDataSizesBaseEvent payload, Map<Object, Object> variables) {
                stackUpdater.updateStackStatus(
                        context.getStackId(), DETERMINE_DATALAKE_DATA_SIZES_IN_PROGRESS, "Determination of datalake data sizes in progress"
                );
                performSaltUpdate(context.getStackId());
                sendEvent(context);
            }

            @Override
            protected Selectable createRequest(DetermineDatalakeDataSizesContext context) {
                return new GetDatalakeDataSizesRequest(context.getStackId(), context.getOperationId());
            }
        };
    }

    @Bean(name = "DETERMINE_DATALAKE_DATA_SIZES_SUBMISSION_STATE")
    public Action<?, ?> determineDatalakeDataSizesSubmissionAction() {
        return new AbstractDetermineDatalakeDataSizesAction<>(DetermineDatalakeDataSizesSubmissionEvent.class) {
            @Override
            protected void doExecute(DetermineDatalakeDataSizesContext context, DetermineDatalakeDataSizesSubmissionEvent payload,
                    Map<Object, Object> variables) {
                Stack stack = stackService.getById(context.getStackId());
                String initiatorUserCrn = ThreadBasedUserCrnProvider.getUserCrn();
                ThreadBasedUserCrnProvider.doAsInternalActor(
                        regionAwareInternalCrnGeneratorFactory.iam().getInternalCrnForServiceAsString(),
                        () -> sdxEndpoint.submitDatalakeDataSizesInternal(
                                stack.getResourceCrn(), context.getOperationId(), payload.getDataSizesResult(), initiatorUserCrn
                        )
                );

                stackUpdater.updateStackStatus(
                        context.getStackId(), DETERMINE_DATALAKE_DATA_SIZES_FINISHED, "Finished determination of datalake data sizes"
                );
                sendEvent(context, DETERMINE_DATALAKE_DATA_SIZES_SUBMISSION_SUCCESS_EVENT.event(), payload);
            }
        };
    }

    @Bean(name = "DETERMINE_DATALAKE_DATA_SIZES_FAILURE_STATE")
    public Action<?, ?> determineDatalakeDataSizesFailureAction() {
        return new AbstractDetermineDatalakeDataSizesAction<>(DetermineDatalakeDataSizesFailureEvent.class) {
            @Override
            protected void doExecute(DetermineDatalakeDataSizesContext context, DetermineDatalakeDataSizesFailureEvent payload, Map<Object, Object> variables) {
                getFlow(context.getFlowId()).setFlowFailed(payload.getException());
                stackUpdater.updateStackStatus(
                        context.getStackId(), DETERMINE_DATALAKE_DATA_SIZES_FAILED, "Determining the datalake data sizes failed"
                );
                sendEvent(context, DETERMINE_DATALAKE_DATA_SIZES_FAILURE_HANDLED_EVENT.event(), payload);
            }
        };
    }

    private void performSaltUpdate(Long stackId) {
        try {
            List<String> saltStateDefinitions = orchestratorMetadataProvider.getSaltStateDefinitionBaseFolders();
            List<String> filteredSaltComponents = List.of("/salt/datalake_metrics");
            byte[] currentSaltState = orchestratorMetadataProvider.getStoredStates(stackId);
            byte[] telemetrySaltStateConfigs = compressUtil.generateCompressedOutputFromFolders(saltStateDefinitions, filteredSaltComponents);
            updateSaltStateForComponents(stackId, telemetrySaltStateConfigs);
            if (currentSaltState != null) {
                byte[] newFullSaltState = compressUtil.updateCompressedOutputFolders(saltStateDefinitions, filteredSaltComponents, currentSaltState);
                orchestratorMetadataProvider.storeNewState(stackId, newFullSaltState);
            }
        } catch (Exception ex) {

        }
    }

    private void updateSaltStateForComponents(Long stackId, byte[] telemetrySaltStateConfigs) {
        try {
            List<String> componentNames = List.of("datalake_metrics");
            OrchestratorMetadata metadata = orchestratorMetadataProvider.getOrchestratorMetadata(stackId);
            telemetryOrchestrator.updatePartialSaltDefinition(telemetrySaltStateConfigs, componentNames, metadata.getGatewayConfigs(),
                    metadata.getExitCriteriaModel());
        } catch (Exception ex) {
        }
    }
}
