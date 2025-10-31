package com.sequenceiq.cloudbreak.core.flow2.cluster.update.publicdns;

import com.sequenceiq.cloudbreak.core.flow2.restart.FillInMemoryStateStoreRestartAction;
import com.sequenceiq.flow.core.FlowState;
import com.sequenceiq.flow.core.RestartAction;

public enum UpdatePublicDnsEntriesFlowState implements FlowState {

    INIT_STATE,
    UPDATE_PUBLIC_DNS_ENTRIES_IN_PEM_STATE,
    UPDATE_PUBLIC_DNS_ENTRIES_FINISHED_STATE,
    UPDATE_PUBLIC_DNS_ENTRIES_FAILED_STATE,
    FINAL_STATE;

    @Override
    public Class<? extends RestartAction> restartAction() {
        return FillInMemoryStateStoreRestartAction.class;
    }
}
