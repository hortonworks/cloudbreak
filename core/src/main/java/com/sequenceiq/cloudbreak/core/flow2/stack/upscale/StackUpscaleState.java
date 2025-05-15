package com.sequenceiq.cloudbreak.core.flow2.stack.upscale;

import com.sequenceiq.cloudbreak.core.flow2.restart.FillInMemoryStateStoreRestartAction;
import com.sequenceiq.flow.core.FlowState;
import com.sequenceiq.flow.core.RestartAction;

public enum StackUpscaleState implements FlowState {
    INIT_STATE,
    UPSCALE_FAILED_STATE,
    UPDATE_DOMAIN_DNS_RESOLVER_STATE,
    UPSCALE_SALT_PREVALIDATION_STATE,
    UPSCALE_PREVALIDATION_STATE,
    UPSCALE_CREATE_USERDATA_SECRETS_STATE,
    ADD_INSTANCES_STATE,
    ADD_INSTANCES_FINISHED_STATE,
    UPSCALE_IMAGE_FALLBACK_STATE,
    EXTEND_METADATA_STATE,
    EXTEND_METADATA_FINISHED_STATE,
    UPSCALE_UPDATE_LOAD_BALANCERS_STATE,
    UPSCALE_UPDATE_USERDATA_SECRETS_STATE,
    UPSCALE_UPDATE_USERDATA_SECRETS_FINISHED_STATE,
    GATEWAY_TLS_SETUP_STATE,
    RE_REGISTER_WITH_CLUSTER_PROXY_STATE,
    BOOTSTRAP_NEW_NODES_STATE,
    EXTEND_HOST_METADATA_STATE,
    CLEANUP_FREEIPA_UPSCALE_STATE,
    EXTEND_HOST_METADATA_FINISHED_STATE,
    FINAL_STATE;

    private Class<? extends RestartAction> restartAction = FillInMemoryStateStoreRestartAction.class;

    StackUpscaleState() {

    }

    StackUpscaleState(Class<? extends RestartAction> restartAction) {
        this.restartAction = restartAction;
    }

    @Override
    public Class<? extends RestartAction> restartAction() {
        return restartAction;
    }
}
