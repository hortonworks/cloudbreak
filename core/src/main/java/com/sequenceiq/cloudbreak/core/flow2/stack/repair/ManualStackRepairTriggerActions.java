package com.sequenceiq.cloudbreak.core.flow2.stack.repair;

import com.sequenceiq.cloudbreak.cloud.event.Selectable;
import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.resource.UnhealthyInstancesDetectionRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.resource.UnhealthyInstancesDetectionResult;
import com.sequenceiq.cloudbreak.service.stack.repair.StackRepairService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.statemachine.action.Action;

import javax.inject.Inject;
import java.util.Map;

@Configuration
public class ManualStackRepairTriggerActions {

    @Inject
    private StackRepairService stackRepairService;

    @Bean(name = "UNHEALTHY_INSTANCES_DETECTION_STATE")
    public Action detectUnhealthyInstancesAction() {
        return new AbstractStackRepairTriggerAction<StackEvent>(StackEvent.class) {

            @Override
            protected void doExecute(StackRepairTriggerContext context, StackEvent payload, Map<Object, Object> variables)
                    throws Exception {
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
                StackRepairNotificationRequest stackRepairNotificationRequest =
                        new StackRepairNotificationRequest(context.getStack(), payload.getUnhealthyInstanceIds());
                stackRepairService.add(stackRepairNotificationRequest);
                sendEvent(context);
            }

            @Override
            protected Selectable createRequest(StackRepairTriggerContext context) {
                return new StackEvent(ManualStackRepairTriggerEvent.REPAIR_SERVICE_NOTIFIED_EVENT.stringRepresentation(), context.getStack().getId());
            }
        };
    }
}
