package com.sequenceiq.cloudbreak.core.flow2.stack.repair;

import java.util.Map;
import java.util.Optional;

import javax.inject.Inject;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.statemachine.StateContext;
import org.springframework.statemachine.action.Action;

import com.sequenceiq.cloudbreak.api.model.Status;
import com.sequenceiq.cloudbreak.cloud.event.Payload;
import com.sequenceiq.cloudbreak.cloud.event.Selectable;
import com.sequenceiq.cloudbreak.core.flow2.AbstractAction;
import com.sequenceiq.cloudbreak.core.flow2.stack.FlowMessageService;
import com.sequenceiq.cloudbreak.core.flow2.stack.Msg;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.StackFailureEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.resource.UnhealthyInstancesDetectionRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.resource.UnhealthyInstancesDetectionResult;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.cloudbreak.service.stack.repair.StackRepairService;

@Configuration
public class ManualStackRepairTriggerActions {

    @Inject
    private StackRepairService stackRepairService;

    @Inject
    private FlowMessageService flowMessageService;

    @Bean(name = "UNHEALTHY_INSTANCES_DETECTION_STATE")
    public Action detectUnhealthyInstancesAction() {
        return new AbstractStackRepairTriggerAction<StackEvent>(StackEvent.class) {

            @Override
            protected void doExecute(StackRepairTriggerContext context, StackEvent payload, Map<Object, Object> variables)
                    throws Exception {
                flowMessageService.fireEventAndLog(payload.getStackId(), Msg.STACK_REPAIR_DETECTION_STARTED, Status.UPDATE_IN_PROGRESS.name());
                sendEvent(context);
            }

            @Override
            protected Selectable createRequest(StackRepairTriggerContext context) {
                return new UnhealthyInstancesDetectionRequest(context.getStack().getId());
            }
        };
    }

    @Bean(name = "NOTIFY_STACK_REPAIR_SERVICE_STATE")
    public Action notifyStackRepairServiceAction() {
        return new AbstractStackRepairTriggerAction<UnhealthyInstancesDetectionResult>(UnhealthyInstancesDetectionResult.class) {

            @Override
            protected void doExecute(StackRepairTriggerContext context, UnhealthyInstancesDetectionResult payload, Map<Object, Object> variables)
                    throws Exception {
                stackRepairService.add(context.getStack(), payload.getUnhealthyInstanceIds());
                sendEvent(context);
            }

            @Override
            protected Selectable createRequest(StackRepairTriggerContext context) {
                return new StackEvent(ManualStackRepairTriggerEvent.REPAIR_SERVICE_NOTIFIED_EVENT.stringRepresentation(), context.getStack().getId());
            }
        };
    }

    private abstract class AbstractStackRepairTriggerAction<P extends Payload>
            extends AbstractAction<ManualStackRepairTriggerState, ManualStackRepairTriggerEvent, StackRepairTriggerContext, P> {

        @Inject
        private StackService stackService;

        protected AbstractStackRepairTriggerAction(Class<P> payloadClass) {
            super(payloadClass);
        }

        @Override
        protected StackRepairTriggerContext createFlowContext(
                String flowId, StateContext<ManualStackRepairTriggerState, ManualStackRepairTriggerEvent> stateContext, P payload) {
            Long stackId = payload.getStackId();
            Stack stack = stackService.getById(stackId);
            return new StackRepairTriggerContext(flowId, stack);
        }

        @Override
        protected Object getFailurePayload(P payload, Optional<StackRepairTriggerContext> flowContext, Exception ex) {
            return new StackFailureEvent(payload.getStackId(), ex);
        }
    }
}
