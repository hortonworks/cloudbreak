package com.sequenceiq.redbeams.flow.redbeams.changeinstancetype.handler;

import static com.sequenceiq.redbeams.flow.redbeams.changeinstancetype.event.ChangeInstanceTypeHandlerSelectors.VALIDATE_CHANGE_INSTANCE_TYPE_HANDLER;
import static com.sequenceiq.redbeams.flow.redbeams.changeinstancetype.event.ChangeInstanceTypeStateSelectors.CHANGE_INSTANCE_TYPE_VALIDATION_FINISHED_EVENT;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.flow.reactor.api.handler.ExceptionCatcherEventHandler;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;
import com.sequenceiq.redbeams.flow.redbeams.changeinstancetype.event.ChangeInstanceTypeEvent;
import com.sequenceiq.redbeams.flow.redbeams.changeinstancetype.event.ChangeInstanceTypeHandlerRequest;
import com.sequenceiq.redbeams.flow.redbeams.changeinstancetype.event.ChangeInstanceTypeValidationFailedEvent;
import com.sequenceiq.redbeams.service.validation.ChangeInstanceTypeValidator;

@Component
public class ValidateChangeInstanceTypeHandler extends ExceptionCatcherEventHandler<ChangeInstanceTypeHandlerRequest> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ValidateChangeInstanceTypeHandler.class);

    @Inject
    private ChangeInstanceTypeValidator changeInstanceTypeValidator;

    @Override
    public String selector() {
        return VALIDATE_CHANGE_INSTANCE_TYPE_HANDLER.event();
    }

    @Override
    protected Selectable defaultFailureEvent(Long resourceId, Exception e, Event<ChangeInstanceTypeHandlerRequest> event) {
        LOGGER.warn("Validation of changing the instance type failed for the database server with id {}", resourceId, e);
        return new ChangeInstanceTypeValidationFailedEvent(resourceId, e);
    }

    @Override
    protected Selectable doAccept(HandlerEvent<ChangeInstanceTypeHandlerRequest> event) {
        ChangeInstanceTypeHandlerRequest request = event.getData();
        changeInstanceTypeValidator.validate(request.getDatabaseStack(), request.getLiveState(), request.getTargetInstanceType(), request.getEnvironmentCrn(),
                request.getCloudContext().getPlatform().value(), request.getCloudContext().getLocation().getRegion().getRegionName());
        return new ChangeInstanceTypeEvent(CHANGE_INSTANCE_TYPE_VALIDATION_FINISHED_EVENT.event(), request.getResourceId(),
                request.getTargetInstanceType(), request.getLiveState());
    }
}
