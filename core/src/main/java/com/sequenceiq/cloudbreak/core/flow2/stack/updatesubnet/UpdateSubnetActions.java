package com.sequenceiq.cloudbreak.core.flow2.stack.updatesubnet;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import javax.inject.Inject;

import org.springframework.context.annotation.Bean;
import org.springframework.statemachine.StateContext;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.event.Payload;
import com.sequenceiq.cloudbreak.cloud.event.resource.UpdateStackResult;
import com.sequenceiq.cloudbreak.core.flow.context.UpdateAllowedSubnetsContext;
import com.sequenceiq.cloudbreak.core.flow2.AbstractAction;
import com.sequenceiq.cloudbreak.core.flow2.PayloadConverter;
import com.sequenceiq.cloudbreak.domain.SecurityRule;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.cloudbreak.service.cluster.flow.UpdateSubnetService;
import com.sequenceiq.cloudbreak.service.stack.StackService;

@Component
public class UpdateSubnetActions {

    private static final String MODIFIAD_SUBNETS = "MODIFIAD_SUBNETS";

    @Inject
    private UpdateSubnetService updateSubnetService;

    @Bean(name = "UPDATE_SUBNET_STATE")
    public AbstractUpdateSubnetAction updateSubnetAction() {
        return new AbstractUpdateSubnetAction<UpdateAllowedSubnetsContext>(UpdateAllowedSubnetsContext.class) {

            @Override
            protected void doExecute(UpdateSubnetContext context, UpdateAllowedSubnetsContext payload, Map<Object, Object> variables) throws Exception {
                Map<String, Set<SecurityRule>> modifiedSubnets = updateSubnetService.updateSubnet(context.getStack(), payload.getAllowedSecurityRules());
                variables.put(MODIFIAD_SUBNETS, modifiedSubnets);
            }
        };
    }

    @Bean(name = "UPDATE_SUBNET_FINISHED_STATE")
    public AbstractUpdateSubnetAction updateSubnetFinishedAction() {
        return new AbstractUpdateSubnetAction<UpdateStackResult>(UpdateStackResult.class) {

            @Override
            protected void doExecute(UpdateSubnetContext context, UpdateStackResult payload, Map<Object, Object> variables) throws Exception {
                updateSubnetService.finalizeUpdateSubnet(context.getStack(), (Map<String, Set<SecurityRule>>) variables.get(MODIFIAD_SUBNETS));
            }
        };
    }

    @Bean(name = "UPDATE_SUBNET_FAILED_STATE")
    public AbstractUpdateSubnetAction updateSubnetFailedAction() {
        return new AbstractUpdateSubnetAction<UpdateSubnetFailedPayload>(UpdateSubnetFailedPayload.class) {
            @Override
            protected void doExecute(UpdateSubnetContext context, UpdateSubnetFailedPayload payload, Map<Object, Object> variables) throws Exception {
                updateSubnetService.handlerUpdateSubnetFailure(context.getStack(), payload.getException());
            }

            @Override
            protected void initPayloadConverterMap(List<PayloadConverter<UpdateSubnetFailedPayload>> payloadConverters) {
                payloadConverters.add(new PayloadConverter<UpdateSubnetFailedPayload>() {
                    @Override
                    public boolean canConvert(Class sourceClass) {
                        return UpdateStackResult.class.isAssignableFrom(sourceClass);
                    }

                    @Override
                    public UpdateSubnetFailedPayload convert(Object payload) {
                        UpdateStackResult result = (UpdateStackResult) payload;
                        return new UpdateSubnetFailedPayload(result.getStackId(), result.getException());
                    }
                });
            }
        };
    }

    private abstract class AbstractUpdateSubnetAction<P extends Payload> extends AbstractAction<UpdateSubnetState, UpdateSubnetEvent, UpdateSubnetContext, P> {

        @Inject
        private StackService stackService;

        protected AbstractUpdateSubnetAction(Class<P> payloadClass) {
            super(payloadClass);
        }

        @Override
        protected UpdateSubnetContext createFlowContext(String flowId, StateContext<UpdateSubnetState, UpdateSubnetEvent> stateContext, P payload) {
            Stack stack = stackService.getById(payload.getStackId());
            MDCBuilder.buildMdcContext(stack);
            return new UpdateSubnetContext(flowId, stack);
        }

        @Override
        protected Object getFailurePayload(P payload, Optional<UpdateSubnetContext> flowContext, Exception ex) {
            return new UpdateSubnetFailedPayload(payload.getStackId(), ex);
        }
    }
}

