package com.sequenceiq.cloudbreak.service.upgrade.sync.operationresult;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class CmSyncOperationSummaryService {

    private static final Logger LOGGER = LoggerFactory.getLogger(CmSyncOperationSummaryService.class);

    @Inject
    private CmSyncOperationResultEvaluatorService cmSyncOperationResultEvaluatorService;

    public CmSyncOperationSummary evaluate(CmSyncOperationResult cmSyncOperationResult) {
        if (cmSyncOperationResult.isEmpty()) {
            String message = "CM sync could not be carried out, most probably the CM server is down. Please make sure the CM server is running.";
            LOGGER.debug(message);
            return CmSyncOperationSummary.ofError(message);
        } else {
            CmSyncOperationSummary.Builder cmSyncOperationSummaryBuilder =
                    cmSyncOperationResultEvaluatorService.evaluateCmRepoSync(cmSyncOperationResult.getCmRepoSyncOperationResult());
            cmSyncOperationSummaryBuilder.merge(
                    cmSyncOperationResultEvaluatorService.evaluateParcelSync(cmSyncOperationResult.getCmParcelSyncOperationResult()));
            return cmSyncOperationSummaryBuilder.build();
        }
    }

}
