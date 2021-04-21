package com.sequenceiq.cloudbreak.core.flow2.validate.kerberosconfig.handler;

import static com.sequenceiq.cloudbreak.core.flow2.validate.kerberosconfig.config.KerberosConfigValidationEvent.VALIDATE_KERBEROS_CONFIG_FAILED_EVENT;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.core.flow2.validate.kerberosconfig.event.StartBindUserCreationEvent;
import com.sequenceiq.cloudbreak.domain.view.StackView;
import com.sequenceiq.cloudbreak.reactor.api.event.StackFailureEvent;
import com.sequenceiq.cloudbreak.retry.RetryException;
import com.sequenceiq.cloudbreak.service.stack.StackViewService;
import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.flow.reactor.api.handler.ExceptionCatcherEventHandler;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;

import reactor.bus.Event;

@Component
public class StartBindUserCreationHandler extends ExceptionCatcherEventHandler<StartBindUserCreationEvent> {

    private static final Logger LOGGER = LoggerFactory.getLogger(StartBindUserCreationHandler.class);

    @Inject
    private StackViewService stackViewService;

    @Inject
    private StartBindUserCreationService startBindUserCreationService;

    @Override
    public String selector() {
        return EventSelectorUtil.selector(StartBindUserCreationEvent.class);
    }

    @Override
    protected Selectable defaultFailureEvent(Long resourceId, Exception e, Event<StartBindUserCreationEvent> event) {
        LOGGER.error("Starting bind user creation failed unexpectedly", e);
        return new StackFailureEvent(VALIDATE_KERBEROS_CONFIG_FAILED_EVENT.event(), event.getData().getResourceId(), e);
    }

    @Override
    protected Selectable doAccept(HandlerEvent<StartBindUserCreationEvent> event) {
        StackView stackView = stackViewService.getById(event.getData().getResourceId());
        try {
            return startBindUserCreationService.startBindUserCreation(stackView);
        } catch (RetryException e) {
            return new StackFailureEvent(VALIDATE_KERBEROS_CONFIG_FAILED_EVENT.event(), stackView.getId(), e);
        }
    }
}
