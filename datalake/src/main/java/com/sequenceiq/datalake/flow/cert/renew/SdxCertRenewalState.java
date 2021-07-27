package com.sequenceiq.datalake.flow.cert.renew;

import com.sequenceiq.datalake.flow.FillInMemoryStateStoreRestartAction;
import com.sequenceiq.flow.core.FlowState;
import com.sequenceiq.flow.core.RestartAction;

public enum SdxCertRenewalState implements FlowState {
    INIT_STATE,
    START_CERT_RENEWAL_STATE,
    CERT_RENEWAL_IN_PROGRESS_STATE,
    CERT_RENEWAL_FINISHED_STATE,
    CERT_RENEWAL_FAILED_STATE,
    FINAL_STATE;

    @Override
    public Class<? extends RestartAction> restartAction() {
        return FillInMemoryStateStoreRestartAction.class;
    }
}
