package com.sequenceiq.environment.environment.poller;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.dyngr.core.AttemptMaker;
import com.dyngr.core.AttemptResult;
import com.dyngr.core.AttemptResults;
import com.dyngr.exception.PollerStoppedException;
import com.sequenceiq.cloudbreak.cloud.scheduler.PollGroup;
import com.sequenceiq.environment.environment.flow.DatalakeMultipleFlowsResultEvaluator;
import com.sequenceiq.environment.environment.service.sdx.SdxService;
import com.sequenceiq.environment.store.EnvironmentInMemoryStateStore;
import com.sequenceiq.flow.api.model.FlowIdentifier;
import com.sequenceiq.flow.api.model.operation.OperationProgressStatus;
import com.sequenceiq.flow.api.model.operation.OperationView;

@Component
public class SdxPollerProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(SdxPollerProvider.class);

    private final SdxService sdxService;

    private final DatalakeMultipleFlowsResultEvaluator multipleFlowsResultEvaluator;

    public SdxPollerProvider(SdxService sdxService,
            DatalakeMultipleFlowsResultEvaluator multipleFlowsResultEvaluator) {
        this.sdxService = sdxService;
        this.multipleFlowsResultEvaluator = multipleFlowsResultEvaluator;
    }

    public AttemptMaker<Void> startStopSdxClustersPoller(Long envId, List<FlowIdentifier> flowIdentifiers) {
        return () -> collectSdxResults(flowIdentifiers, envId);
    }

    private AttemptResult<Void> collectSdxResults(List<FlowIdentifier> flowIdentifiers, Long envId) {
        if (PollGroup.CANCELLED.equals(EnvironmentInMemoryStateStore.get(envId))) {
            String message = "Sdx polling cancelled in inmemory store, id: " + envId;
            LOGGER.info(message);
            throw new PollerStoppedException(message);
        }
        if (multipleFlowsResultEvaluator.allFinished(flowIdentifiers)) {
            return AttemptResults.justFinish();
        } else {
            return AttemptResults.justContinue();
        }
    }

    public AttemptResult<Void> upgradeCcmPoller(Long envId, String datalakeCrn) {
        if (PollGroup.CANCELLED.equals(EnvironmentInMemoryStateStore.get(envId))) {
            String message = "SDX polling cancelled in inmemory store, environment id: " + envId;
            LOGGER.info(message);
            throw new PollerStoppedException(message);
        }
        OperationView operation = sdxService.getOperation(datalakeCrn, false);
        OperationProgressStatus progressStatus = operation.getProgressStatus();
        switch (progressStatus) {
            case CANCELLED:
                return AttemptResults.breakFor("SDX Upgrade CCM cancelled for datalake CRN " + datalakeCrn);
            case FAILED:
                return AttemptResults.breakFor("SDX Upgrade CCM failed for environment CRN " + datalakeCrn);
            case FINISHED:
                return AttemptResults.justFinish();
            case RUNNING:
                return AttemptResults.justContinue();
            default:
                return AttemptResults.breakFor("SDX Upgrade CCM is in ambiguous state " + progressStatus + " for environment CRN " + datalakeCrn);
        }
    }

}
