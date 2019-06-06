package com.sequenceiq.redbeams.flow.redbeams.provision.action;

import com.sequenceiq.flow.core.CommonContext;
import com.sequenceiq.flow.core.FlowEvent;
import com.sequenceiq.flow.core.FlowParameters;
import com.sequenceiq.flow.core.FlowState;
import com.sequenceiq.redbeams.flow.redbeams.provision.event.allocate.AllocateDatabaseServerSuccess;
import com.sequenceiq.redbeams.flow.stack.AbstractRedbeamsAction;
import com.sequenceiq.redbeams.flow.stack.RedbeamsContext;
import com.sequenceiq.redbeams.flow.stack.RedbeamsEvent;

import java.util.Map;
import java.util.Optional;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.statemachine.StateContext;
import org.springframework.statemachine.action.Action;

@Configuration
public class RedbeamsProvisionActions {

    @Bean(name = "ALLOCATE_DATABASE_STATE")
    public Action<?, ?> allocateDatabase() {
        return new AbstractRedbeamsAction<>(RedbeamsEvent.class) {
            @Override
            protected RedbeamsContext createFlowContext(FlowParameters flowParameters, StateContext<FlowState, FlowEvent> stateContext,
                                                        RedbeamsEvent payload) {
                // Get the cloud context and the cloud credentials
                return new RedbeamsContext(flowParameters, null, null);
            }

            @Override
            protected void doExecute(CommonContext context, RedbeamsEvent payload, Map<Object, Object> variables) throws Exception {
                sendEvent(context);
            }

            @Override
            protected Object getFailurePayload(RedbeamsEvent payload, Optional<CommonContext> flowContext, Exception ex) {
                return null;
            }
        };
    }

    @Bean(name = "REDBEAMS_PROVISION_FINISHED_STATE")
    public Action<?, ?> provisionFinished() {
        return new AbstractRedbeamsAction<>(AllocateDatabaseServerSuccess.class) {
            @Override
            protected RedbeamsContext createFlowContext(FlowParameters flowParameters, StateContext<FlowState, FlowEvent> stateContext,
                                                        AllocateDatabaseServerSuccess payload) {
                // Get the cloud context and the cloud credentials
                return new RedbeamsContext(flowParameters, null, null);
            }

            @Override
            protected void doExecute(CommonContext context, AllocateDatabaseServerSuccess payload, Map<Object, Object> variables) throws Exception {
                sendEvent(context);
            }

            @Override
            protected Object getFailurePayload(AllocateDatabaseServerSuccess payload, Optional<CommonContext> flowContext, Exception ex) {
                return null;
            }
        };
    }
}
