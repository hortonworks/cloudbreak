package com.sequenceiq.cloudbreak.controller.mapper;

import static com.sequenceiq.cloudbreak.event.ResourceEvent.COMMON_BAD_REQUEST_NOTIFICATION_PATTERN;

import java.util.List;

import javax.inject.Inject;
import javax.ws.rs.core.Response;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.cloudbreak.common.exception.ExceptionResponse;
import com.sequenceiq.cloudbreak.exception.mapper.BaseExceptionMapper;
import com.sequenceiq.cloudbreak.service.StackUnderOperationService;
import com.sequenceiq.cloudbreak.structuredevent.event.CloudbreakEventService;

abstract class SendNotificationExceptionMapper<E extends Throwable> extends BaseExceptionMapper<E> {

    private static final Logger LOGGER = LoggerFactory.getLogger(SendNotificationExceptionMapper.class);

    @Inject
    private StackUnderOperationService stackUnderOperationService;

    @Inject
    private CloudbreakEventService eventService;

    @Override
    public Response toResponse(E exception) {
        Long stackId = stackUnderOperationService.get();
        Response response = super.toResponse(exception);
        if (stackId != null) {
            String message = "";
            try {
                message = response.readEntity(ExceptionResponse.class).getMessage();
            } catch (RuntimeException e) {
                LOGGER.error("Can't read entity for mapping", e);
            }
            eventService.fireCloudbreakEvent(stackId, "BAD_REQUEST", COMMON_BAD_REQUEST_NOTIFICATION_PATTERN, List.of(message));
        }
        return response;
    }
}
