package com.sequenceiq.cloudbreak.service.upgrade.sync.operationresult;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class CmSyncOperationSummaryService {

    private static final Logger LOGGER = LoggerFactory.getLogger(CmSyncOperationSummaryService.class);

    @Inject
    private CmSyncOperationResultEvaluatorService cmSyncOperationResultEvaluatorService;

    public CmSyncOperationStatus evaluate(CmSyncOperationResult cmSyncOperationResult) {
        if (cmSyncOperationResult.isEmpty()) {
            String message = "CM sync could not be carried out, most probably the CM server is down. Please make sure the CM server is running.";
            LOGGER.debug(message);
            return CmSyncOperationStatus.ofError(message);
        } else {
            CmSyncOperationStatus.Builder cmSyncOperationStatusBuilder =
                    cmSyncOperationResultEvaluatorService.evaluateCmRepoSync(cmSyncOperationResult.getCmRepoSyncOperationResult());
            cmSyncOperationStatusBuilder.merge(
                    cmSyncOperationResultEvaluatorService.evaluateParcelSync(cmSyncOperationResult.getCmParcelSyncOperationResult()));
            return cmSyncOperationStatusBuilder.build();
        }
    }

}
