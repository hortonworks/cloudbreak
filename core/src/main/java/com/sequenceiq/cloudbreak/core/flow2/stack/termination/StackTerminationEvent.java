package com.sequenceiq.cloudbreak.core.flow2.stack.termination;

import com.sequenceiq.cloudbreak.cloud.event.resource.TerminateStackResult;
import com.sequenceiq.cloudbreak.reactor.api.event.recipe.CcmKeyDeregisterSuccess;
import com.sequenceiq.cloudbreak.reactor.api.event.recipe.ClusterProxyDeregisterSuccess;
import com.sequenceiq.cloudbreak.reactor.api.event.recipe.StackPreTerminationFailed;
import com.sequenceiq.cloudbreak.reactor.api.event.recipe.StackPreTerminationSuccess;
import com.sequenceiq.cloudbreak.reactor.api.event.stack.consumption.AttachedVolumeConsumptionCollectionUnschedulingFailed;
import com.sequenceiq.cloudbreak.reactor.api.event.stack.consumption.AttachedVolumeConsumptionCollectionUnschedulingSuccess;
import com.sequenceiq.cloudbreak.reactor.api.event.stack.encryption.DeleteUserdataSecretsFailed;
import com.sequenceiq.cloudbreak.reactor.api.event.stack.encryption.DeleteUserdataSecretsFinished;
import com.sequenceiq.flow.core.FlowEvent;
import com.sequenceiq.flow.event.EventSelectorUtil;

public enum StackTerminationEvent implements FlowEvent {
    TERMINATION_EVENT("STACK_TERMINATE_TRIGGER_EVENT"),
    RECOVERY_TERMINATION_EVENT("STACK_RECOVERY_TERMINATION_EVENT"),
    PRE_TERMINATION_FAILED_EVENT(EventSelectorUtil.selector(StackPreTerminationFailed.class)),
    PRE_TERMINATION_FINISHED_EVENT(EventSelectorUtil.selector(StackPreTerminationSuccess.class)),
    DELETE_USERDATA_SECRETS_FINISHED_EVENT(EventSelectorUtil.selector(DeleteUserdataSecretsFinished.class)),
    DELETE_USERDATA_SECRETS_FAILED_EVENT(EventSelectorUtil.selector(DeleteUserdataSecretsFailed.class)),
    CLUSTER_PROXY_DEREGISTER_SUCCEEDED_EVENT(EventSelectorUtil.selector(ClusterProxyDeregisterSuccess.class)),
    CLUSTER_PROXY_DEREGISTER_FAILED_EVENT("CLUSTER_PROXY_DEREGISTER_FAILED_EVENT"),
    CCM_KEY_DEREGISTER_SUCCEEDED_EVENT(EventSelectorUtil.selector(CcmKeyDeregisterSuccess.class)),
    CCM_KEY_DEREGISTER_FAILED_EVENT("CCM_KEY_DEREGISTER_FAILED_EVENT"),
    ATTACHED_VOLUME_CONSUMPTION_COLLECTION_UNSCHEDULING_FINISHED_EVENT(EventSelectorUtil.selector(AttachedVolumeConsumptionCollectionUnschedulingSuccess.class)),
    ATTACHED_VOLUME_CONSUMPTION_COLLECTION_UNSCHEDULING_FAILED_EVENT(EventSelectorUtil.selector(AttachedVolumeConsumptionCollectionUnschedulingFailed.class)),
    TERMINATION_FINISHED_EVENT(EventSelectorUtil.selector(TerminateStackResult.class)),
    TERMINATION_FAILED_EVENT(EventSelectorUtil.failureSelector(TerminateStackResult.class)),
    TERMINATION_FINALIZED_EVENT("TERMINATESTACKFINALIZED"),
    STACK_TERMINATION_FAIL_HANDLED_EVENT("TERMINATIONFAILHANDLED");

    private final String event;

    StackTerminationEvent(String event) {
        this.event = event;
    }

    @Override
    public String event() {
        return event;
    }
}
