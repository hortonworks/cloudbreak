package com.sequenceiq.cloudbreak.core.flow2.restart;

import static com.sequenceiq.cloudbreak.common.type.CloudConstants.GCP;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.service.TransactionService.TransactionExecutionException;
import com.sequenceiq.cloudbreak.common.service.TransactionService.TransactionRuntimeExecutionException;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;
import com.sequenceiq.cloudbreak.view.StackView;
import com.sequenceiq.flow.core.FlowLogService;
import com.sequenceiq.flow.core.RestartContext;

@Component("DisableOnGCPRestartAction")
public class DisableOnGCPRestartAction extends FillInMemoryStateStoreRestartAction {

    @Inject
    private StackDtoService stackDtoService;

    @Inject
    private FlowLogService flowLogService;

    @Override
    public void restart(RestartContext restartContext, Object payload) {
        StackView stack = stackDtoService.getStackViewById(restartContext.getResourceId());
        if (stack.getPlatformVariant().equals(GCP)) {
            if (restartContext.getFlowId() != null) {
                try {
                    flowLogService.terminate(restartContext.getResourceId(), restartContext.getFlowId(), "Flow restart is disabled on GCP");
                } catch (TransactionExecutionException e) {
                    throw new TransactionRuntimeExecutionException(e);
                }
            }
        } else {
            super.restart(restartContext, payload);
        }
    }
}
