package com.sequenceiq.freeipa.flow.stack.update.action;

import static com.sequenceiq.freeipa.flow.freeipa.common.FailureType.ERROR;
import static com.sequenceiq.freeipa.flow.stack.update.UpdateUserDataEvents.UPDATE_USERDATA_FAILED_EVENT;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.statemachine.action.Action;

import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.freeipa.converter.cloud.ResourceToCloudResourceConverter;
import com.sequenceiq.freeipa.entity.Resource;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.flow.stack.StackContext;
import com.sequenceiq.freeipa.flow.stack.StackEvent;
import com.sequenceiq.freeipa.flow.stack.update.UpdateUserDataEvents;
import com.sequenceiq.freeipa.flow.stack.update.event.UserDataUpdateFailed;
import com.sequenceiq.freeipa.flow.stack.update.event.UserDataUpdateOnProviderRequest;
import com.sequenceiq.freeipa.flow.stack.update.event.UserDataUpdateOnProviderResult;
import com.sequenceiq.freeipa.flow.stack.update.event.UserDataUpdateRequest;
import com.sequenceiq.freeipa.flow.stack.update.event.UserDataUpdateSuccess;
import com.sequenceiq.freeipa.service.operation.OperationService;
import com.sequenceiq.freeipa.service.resource.ResourceService;

@Configuration
public class UserDataUpdateActions {
    private static final Logger LOGGER = LoggerFactory.getLogger(UserDataUpdateActions.class);

    @Inject
    private ResourceService resourceService;

    @Inject
    private ResourceToCloudResourceConverter resourceToCloudResourceConverter;

    @Inject
    private OperationService operationService;

    @Bean(name = "UPDATE_USERDATA_STATE")
    public AbstractUserDataUpdateAction<?> updateUserData() {
        return new AbstractUserDataUpdateAction<>(UserDataUpdateRequest.class) {
            @Override
            protected void doExecute(StackContext context, UserDataUpdateRequest payload, Map<Object, Object> variables) throws Exception {
                LOGGER.info("Update userdata for new freeipa instances");
                setOperationId(variables, payload.getOperationId());
                setChainedAction(variables, payload.isChained());
                setFinalChain(variables, payload.isFinalFlow());
                sendEvent(context,
                        UserDataUpdateRequest.builder()
                                .withStackId(context.getStack().getId())
                                .withOldTunnel(payload.getOldTunnel())
                                .withModifyProxyConfig(payload.isModifyProxyConfig())
                                .build());
            }

            @Override
            protected Object getFailurePayload(UserDataUpdateRequest payload, Optional<StackContext> flowContext, Exception ex) {
                return new UserDataUpdateFailed(payload.getResourceId(), ex, ERROR);
            }
        };
    }

    @Bean(name = "UPDATE_USERDATA_ON_PROVIDER_STATE")
    public AbstractUserDataUpdateAction<?> updateUserDataOnProviderSide() {
        return new AbstractUserDataUpdateAction<>(UserDataUpdateSuccess.class) {
            @Override
            protected void doExecute(StackContext context, UserDataUpdateSuccess payload, Map<Object, Object> variables) {
                sendEvent(context);
            }

            @Override
            protected Selectable createRequest(StackContext context) {
                Stack stack = context.getStack();
                String userData = stack.getImage().getUserdataWrapper();
                List<CloudResource> cloudResources = getCloudResources(stack.getId());
                return new UserDataUpdateOnProviderRequest(context.getCloudContext(), context.getCloudCredential(), context.getCloudStack(),
                        cloudResources, userData);
            }

            @Override
            protected Object getFailurePayload(UserDataUpdateSuccess payload, Optional<StackContext> flowContext, Exception ex) {
                return new UserDataUpdateFailed(UPDATE_USERDATA_FAILED_EVENT.event(), payload.getResourceId(), ex, ERROR);
            }

            private List<CloudResource> getCloudResources(Long stackId) {
                List<Resource> resources = resourceService.findAllByStackId(stackId);
                return resources.stream()
                        .map(r -> resourceToCloudResourceConverter.convert(r))
                        .collect(Collectors.toList());
            }
        };
    }

    @Bean(name = "UPDATE_USERDATA_FINISHED_STATE")
    public AbstractUserDataUpdateAction<?> finishUserDataUpdate() {
        return new AbstractUserDataUpdateAction<>(UserDataUpdateOnProviderResult.class) {
            @Override
            protected void doExecute(StackContext context, UserDataUpdateOnProviderResult payload, Map<Object, Object> variables) {
                if (isOperationIdSet(variables) && (!isChainedAction(variables) || isFinalChain(variables))) {
                    operationService.completeOperation(context.getStack().getAccountId(), getOperationId(variables), Set.of(), Set.of());
                }
                sendEvent(context);
            }

            @Override
            protected Selectable createRequest(StackContext context) {
                return new StackEvent(UpdateUserDataEvents.UPDATE_USERDATA_FINISHED_EVENT.selector(), context.getStack().getId());
            }

            @Override
            protected Object getFailurePayload(UserDataUpdateOnProviderResult payload, Optional<StackContext> flowContext, Exception ex) {
                return new UserDataUpdateFailed(UPDATE_USERDATA_FAILED_EVENT.event(), payload.getResourceId(), ex, ERROR);
            }
        };
    }

    @Bean(name = "UPDATE_USERDATA_FAILED_STATE")
    public Action<?, ?> handleUpdateUserDataFailure() {
        return new UserDataUpdateFailureHandlerAction();
    }
}
