package com.sequenceiq.freeipa.flow.stack.update.handler;

import static com.sequenceiq.freeipa.flow.stack.update.UpdateUserDataEvents.UPDATE_USERDATA_FAILED_EVENT;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.CloudConnector;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.init.CloudPlatformConnectors;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.flow.reactor.api.handler.ExceptionCatcherEventHandler;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;
import com.sequenceiq.freeipa.converter.cloud.ResourceToCloudResourceConverter;
import com.sequenceiq.freeipa.flow.stack.update.event.UserDataUpdateFailed;
import com.sequenceiq.freeipa.flow.stack.update.event.UserDataUpdateOnProviderRequest;
import com.sequenceiq.freeipa.flow.stack.update.event.UserDataUpdateOnProviderResult;
import com.sequenceiq.freeipa.service.resource.ResourceService;

import reactor.bus.Event;

@Component
public class UpdateUserDataOnProviderHandler extends ExceptionCatcherEventHandler<UserDataUpdateOnProviderRequest> {
    private static final Logger LOGGER = LoggerFactory.getLogger(UpdateUserDataOnProviderHandler.class);

    @Inject
    private ResourceService resourceService;

    @Inject
    private ResourceToCloudResourceConverter resourceToCloudResourceConverter;

    @Inject
    private CloudPlatformConnectors cloudPlatformConnectors;

    @Override
    public String selector() {
        return EventSelectorUtil.selector(UserDataUpdateOnProviderRequest.class);
    }

    @Override
    protected Selectable defaultFailureEvent(Long resourceId, Exception e, Event<UserDataUpdateOnProviderRequest> event) {
        LOGGER.error("Updating user data on provider side has failed", e);
        return new UserDataUpdateFailed(UPDATE_USERDATA_FAILED_EVENT.event(), resourceId, e);
    }

    @Override
    protected Selectable doAccept(HandlerEvent<UserDataUpdateOnProviderRequest> event) {
        UserDataUpdateOnProviderRequest request = event.getData();
        try {
            LOGGER.info("Updating userData on cloud provider side...");
            CloudConnector<?> connector = cloudPlatformConnectors.get(request.getCloudContext().getPlatformVariant());
            AuthenticatedContext auth = connector.authentication().authenticate(request.getCloudContext(), request.getCloudCredential());
            CloudStack stack = request.getCloudStack();

            connector.resources().updateUserData(auth, stack, request.getCloudResources(), request.getUserData());
            return new UserDataUpdateOnProviderResult(request.getResourceId());
        } catch (Exception e) {
            LOGGER.error("Updating user data on provider side has failed", e);
            return new UserDataUpdateFailed(UPDATE_USERDATA_FAILED_EVENT.event(), request.getResourceId(), e);
        }
    }
}
