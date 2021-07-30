package com.sequenceiq.cloudbreak.service.upgrade.sync.operationresult;

import javax.inject.Inject;

import org.springframework.stereotype.Service;

@Service
public class CmSyncOperationSummaryService {

    @Inject
    private CmSyncOperationResultEvaluatorService cmSyncOperationResultEvaluatorService;

    public CmSyncOperationSummary evaluate(CmSyncOperationResult cmSyncOperationResult) {
        if (cmSyncOperationResult.isEmpty()) {
            String message = "CM sync could not be carried out, most probably the CM server is down. Please make sure the CM server is running.";
            return CmSyncOperationSummary.ofError(message);
        }
        CmSyncOperationSummary.Builder cmSyncOperationSummaryBuilder = CmSyncOperationSummary.builder();
        cmSyncOperationResultEvaluatorService.evaluateCmRepoSync(cmSyncOperationResult.getCmRepoSyncOperationResult(), cmSyncOperationSummaryBuilder);
        cmSyncOperationResultEvaluatorService.evaluateParcelSync(cmSyncOperationResult.getCmParcelSyncOperationResult(), cmSyncOperationSummaryBuilder);
        return cmSyncOperationSummaryBuilder.build();
    }

}
