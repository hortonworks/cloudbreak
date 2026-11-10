package com.sequenceiq.redbeams.flow.redbeams.changeinstancetype.handler;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.cloudbreak.cloud.CloudConnector;
import com.sequenceiq.cloudbreak.cloud.ResourceConnector;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.init.CloudPlatformConnectors;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.flow.reactor.api.handler.ExceptionCatcherEventHandler;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;
import com.sequenceiq.redbeams.flow.redbeams.changeinstancetype.event.ChangeInstanceTypeFailedEvent;
import com.sequenceiq.redbeams.flow.redbeams.changeinstancetype.event.ChangeInstanceTypeHandlerRequest;

abstract class AbstractChangeInstanceTypeHandler extends ExceptionCatcherEventHandler<ChangeInstanceTypeHandlerRequest> {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractChangeInstanceTypeHandler.class);

    @Inject
    private CloudPlatformConnectors cloudPlatformConnectors;

    @Override
    protected Selectable defaultFailureEvent(Long resourceId, Exception e, Event<ChangeInstanceTypeHandlerRequest> event) {
        LOGGER.warn("{} failed for the database server with id {}", getClass().getSimpleName(), resourceId, e);
        return new ChangeInstanceTypeFailedEvent(resourceId, e);
    }

    @Override
    protected Selectable doAccept(HandlerEvent<ChangeInstanceTypeHandlerRequest> event) {
        ChangeInstanceTypeHandlerRequest request = event.getData();
        try {
            CloudContext cloudContext = request.getCloudContext();
            CloudConnector connector = cloudPlatformConnectors.get(cloudContext.getPlatformVariant());
            AuthenticatedContext ac = connector.authentication().authenticate(cloudContext, request.getCloudCredential());
            return doAccept(request, connector.resources(), ac);
        } catch (Exception e) {
            return defaultFailureEvent(request.getResourceId(), e, event.getEvent());
        }
    }

    protected abstract Selectable doAccept(ChangeInstanceTypeHandlerRequest request, ResourceConnector resourceConnector, AuthenticatedContext ac)
            throws Exception;
}
