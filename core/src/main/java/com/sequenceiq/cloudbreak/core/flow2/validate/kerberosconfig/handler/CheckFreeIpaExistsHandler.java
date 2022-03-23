package com.sequenceiq.cloudbreak.core.flow2.validate.kerberosconfig.handler;

import javax.inject.Inject;
import javax.ws.rs.NotFoundException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.crn.RegionAwareInternalCrnGeneratorFactory;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.core.flow2.validate.kerberosconfig.config.KerberosConfigValidationEvent;
import com.sequenceiq.cloudbreak.core.flow2.validate.kerberosconfig.event.CheckFreeIpaExistsEvent;
import com.sequenceiq.cloudbreak.core.flow2.validate.kerberosconfig.event.ValidateKerberosConfigEvent;
import com.sequenceiq.cloudbreak.domain.view.StackView;
import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.StackFailureEvent;
import com.sequenceiq.cloudbreak.service.stack.StackViewService;
import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.flow.reactor.api.handler.ExceptionCatcherEventHandler;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.FreeIpaV1Endpoint;

import reactor.bus.Event;

@Component
public class CheckFreeIpaExistsHandler extends ExceptionCatcherEventHandler<CheckFreeIpaExistsEvent> {

    private static final Logger LOGGER = LoggerFactory.getLogger(CheckFreeIpaExistsHandler.class);

    @Inject
    private FreeIpaV1Endpoint freeIpaV1Endpoint;

    @Inject
    private StackViewService stackViewService;

    @Inject
    private RegionAwareInternalCrnGeneratorFactory regionAwareInternalCrnGeneratorFactory;

    @Override
    public String selector() {
        return EventSelectorUtil.selector(CheckFreeIpaExistsEvent.class);
    }

    @Override
    protected Selectable defaultFailureEvent(Long resourceId, Exception e, Event<CheckFreeIpaExistsEvent> event) {
        LOGGER.error("Checking FreeIPA failed unexpectedly", e);
        return new StackFailureEvent(KerberosConfigValidationEvent.VALIDATE_KERBEROS_CONFIG_FAILED_EVENT.event(), event.getData().getResourceId(), e);
    }

    @Override
    protected Selectable doAccept(HandlerEvent<CheckFreeIpaExistsEvent> event) {
        CheckFreeIpaExistsEvent data = event.getData();
        if (doesFreeIpaExists(data)) {
            return new StackEvent(KerberosConfigValidationEvent.FREEIPA_EXISTS_EVENT.event(), data.getResourceId());
        } else {
            return new ValidateKerberosConfigEvent(KerberosConfigValidationEvent.VALIDATE_KERBEROS_CONFIG_EXISTS_EVENT.event(), data.getResourceId(), false);
        }
    }

    private boolean doesFreeIpaExists(CheckFreeIpaExistsEvent data) {
        StackView stack = stackViewService.getById(data.getResourceId());
        String accountId = ThreadBasedUserCrnProvider.getAccountId();
        try {
            ThreadBasedUserCrnProvider.doAsInternalActor(
                    regionAwareInternalCrnGeneratorFactory.iam().getInternalCrnForServiceAsString(),
                    () -> freeIpaV1Endpoint.describeInternal(stack.getEnvironmentCrn(), accountId));
            LOGGER.info("FreeIPA exists for env [{}] in account [{}]", stack.getEnvironmentCrn(), accountId);
            return true;
        } catch (NotFoundException e) {
            LOGGER.info("FreeIPA doesn't exists for env [{}] in account [{}]", stack.getEnvironmentCrn(), accountId);
            return false;
        }
    }
}
