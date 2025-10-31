package com.sequenceiq.datalake.flow.update.publicdns;

import com.sequenceiq.datalake.flow.FillInMemoryStateStoreRestartAction;
import com.sequenceiq.flow.core.FlowState;
import com.sequenceiq.flow.core.RestartAction;

public enum DatalakeUpdatePublicDnsEntriesFlowState implements FlowState {

    INIT_STATE,
    DATALAKE_UPDATE_PUBLIC_DNS_ENTRIES_STATE,
    DATALAKE_UPDATE_PUBLIC_DNS_ENTRIES_FINISHED_STATE,
    FINAL_STATE,
    DATALAKE_UPDATE_PUBLIC_DNS_ENTRIES_FAILED_STATE;

    private final Class<? extends RestartAction> restartAction = FillInMemoryStateStoreRestartAction.class;

    @Override
    public Class<? extends RestartAction> restartAction() {
        return FillInMemoryStateStoreRestartAction.class;
    }
}
