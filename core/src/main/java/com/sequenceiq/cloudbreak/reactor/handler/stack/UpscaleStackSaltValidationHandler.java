package com.sequenceiq.cloudbreak.reactor.handler.stack;

import jakarta.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.cloudbreak.reactor.api.event.StackFailureEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.stack.UpscaleStackSaltValidationRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.stack.UpscaleStackSaltValidationResult;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;
import com.sequenceiq.cloudbreak.service.stack.YumLockCheckerService;
import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.flow.reactor.api.handler.ExceptionCatcherEventHandler;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;

@Component
public class UpscaleStackSaltValidationHandler extends ExceptionCatcherEventHandler<UpscaleStackSaltValidationRequest> {

    @Inject
    private StackDtoService stackDtoService;

    @Inject
    private YumLockCheckerService yumLockCheckerService;

    @Override
    protected Selectable defaultFailureEvent(Long resourceId, Exception e, Event<UpscaleStackSaltValidationRequest> event) {
        return new StackFailureEvent(EventSelectorUtil.failureSelector(UpscaleStackSaltValidationResult.class), resourceId, e);
    }

    @Override
    protected Selectable doAccept(HandlerEvent<UpscaleStackSaltValidationRequest> event) {
        UpscaleStackSaltValidationRequest request = event.getData();
        StackDto stackDto = stackDtoService.getById(request.getResourceId());
        yumLockCheckerService.validate(stackDto);
        return new UpscaleStackSaltValidationResult(request.getResourceId());
    }

    @Override
    public String selector() {
        return EventSelectorUtil.selector(UpscaleStackSaltValidationRequest.class);
    }
}
