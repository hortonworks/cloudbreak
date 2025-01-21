package com.sequenceiq.cloudbreak.core.flow2.stack.update.userdata;

import static com.sequenceiq.cloudbreak.core.flow2.stack.update.userdata.UpdateUserDataEvents.UPDATE_USERDATA_FAILED_EVENT;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.statemachine.action.Action;

import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.converter.spi.ResourceToCloudResourceConverter;
import com.sequenceiq.cloudbreak.core.flow2.stack.StackContext;
import com.sequenceiq.cloudbreak.domain.Resource;
import com.sequenceiq.cloudbreak.dto.StackDtoDelegate;
import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.stack.userdata.UserDataUpdateFailed;
import com.sequenceiq.cloudbreak.reactor.api.event.stack.userdata.UserDataUpdateOnProviderRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.stack.userdata.UserDataUpdateOnProviderResult;
import com.sequenceiq.cloudbreak.reactor.api.event.stack.userdata.UserDataUpdateRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.stack.userdata.UserDataUpdateSuccess;
import com.sequenceiq.cloudbreak.service.image.userdata.UserDataService;
import com.sequenceiq.cloudbreak.service.resource.ResourceService;
import com.sequenceiq.common.api.type.CommonStatus;
import com.sequenceiq.common.api.type.InstanceGroupType;
import com.sequenceiq.common.api.type.ResourceType;

@Configuration
public class UserDataUpdateActions {
    private static final Logger LOGGER = LoggerFactory.getLogger(UserDataUpdateActions.class);

    @Inject
    private ResourceService resourceService;

    @Inject
    private ResourceToCloudResourceConverter resourceToCloudResourceConverter;

    @Inject
    private UserDataService userDataService;

    @Bean(name = "UPDATE_USERDATA_STATE")
    public AbstractUserDataUpdateAction<?> updateUserData() {
        return new AbstractUserDataUpdateAction<>(UserDataUpdateRequest.class) {
            @Override
            protected void doExecute(StackContext context, UserDataUpdateRequest payload, Map<Object, Object> variables) throws Exception {
                LOGGER.info("Recreate userdata for new instances");
                sendEvent(context, new UserDataUpdateRequest(context.getStack().getId(), payload.getOldTunnel(), payload.isModifyProxyConfig()));
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
                StackDtoDelegate stack = context.getStack();
                Map<InstanceGroupType, String> userData;
                try {
                    userData = userDataService.getUserData(stack.getId());
                } catch (CloudbreakServiceException e) {
                    return new UserDataUpdateFailed(stack.getId(), e);
                }
                List<CloudResource> cloudResources = getCloudResources(stack.getId());
                return new UserDataUpdateOnProviderRequest(context.getCloudContext(), context.getCloudCredential(), context.getCloudStack(),
                        cloudResources, userData);
            }

            private List<CloudResource> getCloudResources(Long stackId) {
                List<Resource> resources = resourceService.findAllByResourceStatusAndResourceTypeAndStackId(CommonStatus.CREATED,
                        ResourceType.CLOUDFORMATION_STACK, stackId);
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
                sendEvent(context);
            }

            @Override
            protected Selectable createRequest(StackContext context) {
                return new StackEvent(UpdateUserDataEvents.UPDATE_USERDATA_FINISHED_EVENT.selector(), context.getStack().getId());
            }

            @Override
            protected Object getFailurePayload(UserDataUpdateOnProviderResult payload, Optional<StackContext> flowContext, Exception ex) {
                return new UserDataUpdateFailed(UPDATE_USERDATA_FAILED_EVENT.event(), payload.getResourceId(), ex);
            }
        };
    }

    @Bean(name = "UPDATE_USERDATA_FAILED_STATE")
    public Action<?, ?> handleUpdateUserDataFailure() {
        return new UserDataUpdateFailureHandlerAction();
    }
}
