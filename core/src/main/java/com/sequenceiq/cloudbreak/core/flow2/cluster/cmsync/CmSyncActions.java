package com.sequenceiq.cloudbreak.core.flow2.cluster.cmsync;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.statemachine.action.Action;

import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.core.flow2.stack.AbstractStackFailureAction;
import com.sequenceiq.cloudbreak.core.flow2.stack.StackFailureContext;
import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.StackFailureEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.orchestration.CmSyncTriggerEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.resource.CmSyncRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.resource.CmSyncResult;

@Configuration
public class CmSyncActions {
    private static final Logger LOGGER = LoggerFactory.getLogger(CmSyncActions.class);

    @Bean(name = "CM_SYNC_STATE")
    public Action<?, ?> cmSync() {
        return new AbstractCmSyncAction<>(CmSyncTriggerEvent.class) {
            @Override
            protected void doExecute(CmSyncContext context, CmSyncTriggerEvent payload, Map<Object, Object> variables) {
                CmSyncRequest cmSyncRequest = new CmSyncRequest(context.getStack().getId(), payload.getCandidateImageUuids(), context.getFlowTriggerUserCrn());
                sendEvent(context, cmSyncRequest.selector(), cmSyncRequest);
            }
        };
    }

    @Bean(name = "CM_SYNC_FINISHED_STATE")
    public Action<?, ?> finishCmSync() {
        return new AbstractCmSyncAction<>(CmSyncResult.class) {

            @Override
            protected Selectable createRequest(CmSyncContext context) {
                return new StackEvent(CmSyncEvent.CM_SYNC_FINALIZED_EVENT.event(), context.getStack().getId());
            }

            @Override
            protected void doExecute(CmSyncContext context, CmSyncResult payload, Map<Object, Object> variables) {
                sendEvent(context);
            }
        };
    }

    @Bean(name = "CM_SYNC_FAILED_STATE")
    public Action<?, ?> cmSyncFailedAction() {
        return new AbstractStackFailureAction<CmSyncState, CmSyncEvent>() {
            @Override
            protected void doExecute(StackFailureContext context, StackFailureEvent payload, Map<Object, Object> variables) {
                LOGGER.warn("Error during executing syncing Cloudera Manager and parcels versions from CM.", payload.getException());
                sendEvent(context);
            }

            @Override
            protected Selectable createRequest(StackFailureContext context) {
                return new StackEvent(CmSyncEvent.CM_SYNC_FAIL_HANDLED_EVENT.event(), context.getStackView().getId());
            }
        };
    }
}
