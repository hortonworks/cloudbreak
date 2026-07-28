package com.sequenceiq.freeipa.flow.freeipa.rollingvscale.handler;

import java.util.List;
import java.util.Optional;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.CloudConnector;
import com.sequenceiq.cloudbreak.cloud.UpdateType;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.init.CloudPlatformConnectors;
import com.sequenceiq.cloudbreak.cloud.model.CloudResourceStatus;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.flow.reactor.api.handler.ExceptionCatcherEventHandler;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;
import com.sequenceiq.freeipa.flow.freeipa.rollingvscale.event.FreeIpaRollingVerticalScaleFailureEvent;
import com.sequenceiq.freeipa.flow.freeipa.rollingvscale.event.RollingVerticalScaleResizeRequest;
import com.sequenceiq.freeipa.flow.freeipa.rollingvscale.event.RollingVerticalScaleResizeResult;

@Component
public class RollingVerticalScaleResizeHandler extends ExceptionCatcherEventHandler<RollingVerticalScaleResizeRequest> {

    private static final Logger LOGGER = LoggerFactory.getLogger(RollingVerticalScaleResizeHandler.class);

    @Inject
    private CloudPlatformConnectors cloudPlatformConnectors;

    @Override
    public String selector() {
        return EventSelectorUtil.selector(RollingVerticalScaleResizeRequest.class);
    }

    @Override
    protected Selectable defaultFailureEvent(Long resourceId, Exception e, Event<RollingVerticalScaleResizeRequest> event) {
        return new FreeIpaRollingVerticalScaleFailureEvent(resourceId, e);
    }

    @Override
    protected Selectable doAccept(HandlerEvent<RollingVerticalScaleResizeRequest> event) {
        RollingVerticalScaleResizeRequest request = event.getData();
        LOGGER.info("Resizing instance for rolling vertical scale (idempotent: resize to same type is a no-op on restart), group: {}",
                request.getGroup());
        try {
            CloudConnector connector = cloudPlatformConnectors.get(request.getCloudContext().getPlatformVariant());
            AuthenticatedContext ac = connector.authentication().authenticate(request.getCloudContext(), request.getCloudCredential());
            List<CloudResourceStatus> result = connector.resources().update(
                    ac, request.getCloudStack(), request.getCloudResources(),
                    UpdateType.VERTICAL_SCALE, Optional.ofNullable(request.getGroup()));
            LOGGER.info("Resize completed with status: {}", result);
            return new RollingVerticalScaleResizeResult(request.getResourceId());
        } catch (Exception e) {
            LOGGER.error("Failed to resize instance during rolling vertical scale, group: {}", request.getGroup(), e);
            throw new CloudbreakServiceException("Failed to resize instance during rolling vertical scale", e);
        }
    }
}
