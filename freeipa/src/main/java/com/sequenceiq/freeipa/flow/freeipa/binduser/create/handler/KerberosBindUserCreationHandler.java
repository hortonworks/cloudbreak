package com.sequenceiq.freeipa.flow.freeipa.binduser.create.handler;

import java.util.Optional;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.flow.reactor.api.handler.ExceptionCatcherEventHandler;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;
import com.sequenceiq.freeipa.client.FreeIpaClientException;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.flow.freeipa.binduser.create.event.CreateBindUserEvent;
import com.sequenceiq.freeipa.flow.freeipa.binduser.create.event.CreateBindUserFailureEvent;
import com.sequenceiq.freeipa.flow.freeipa.binduser.create.event.CreateBindUserFlowEvent;
import com.sequenceiq.freeipa.flow.freeipa.binduser.create.event.CreateKerberosBindUserEvent;
import com.sequenceiq.freeipa.kerberos.KerberosConfig;
import com.sequenceiq.freeipa.kerberos.KerberosConfigService;
import com.sequenceiq.freeipa.kerberos.v1.KerberosConfigV1Service;
import com.sequenceiq.freeipa.service.stack.StackService;

import reactor.bus.Event;

@Component
public class KerberosBindUserCreationHandler extends ExceptionCatcherEventHandler<CreateKerberosBindUserEvent> {

    private static final Logger LOGGER = LoggerFactory.getLogger(KerberosBindUserCreationHandler.class);

    private static final String SELECTOR = EventSelectorUtil.selector(CreateKerberosBindUserEvent.class);

    @Inject
    private StackService stackService;

    @Inject
    private KerberosConfigV1Service kerberosConfigV1Service;

    @Inject
    private KerberosConfigService kerberosConfigService;

    @Override
    public String selector() {
        return SELECTOR;
    }

    @Override
    protected Selectable defaultFailureEvent(Long resourceId, Exception e, Event<CreateKerberosBindUserEvent> event) {
        CreateBindUserEvent eventData = event.getData();
        String failureMsg = String.format("Kerberos bind user creation failed for %s with %s", eventData.getSuffix(), e.getMessage());
        return new CreateBindUserFailureEvent(CreateBindUserFlowEvent.CREATE_BIND_USER_FAILED_EVENT.event(), eventData, failureMsg, e);
    }

    @Override
    protected Selectable doAccept(HandlerEvent<CreateKerberosBindUserEvent> event) {
        CreateBindUserEvent data = event.getData();
        Optional<KerberosConfig> kerberosConfig = kerberosConfigService.find(data.getEnvironmentCrn(), data.getAccountId(), data.getSuffix());
        if (kerberosConfig.isPresent()) {
            LOGGER.info("Kerberos configuration already exist: {}", kerberosConfig.get());
            return new CreateBindUserEvent(CreateBindUserFlowEvent.CREATE_KERBEROS_BIND_USER_FINISHED_EVENT.event(), data);
        } else {
            return createKerberosBindUser(event.getEvent(), data);
        }
    }

    private Selectable createKerberosBindUser(Event<CreateKerberosBindUserEvent> event, CreateBindUserEvent data) {
        Stack stack = stackService.getByEnvironmentCrnAndAccountId(data.getEnvironmentCrn(), data.getAccountId());
        MDCBuilder.buildMdcContext(stack);
        LOGGER.info("Create Kerberos bind user for [{}]", data.getSuffix());
        try {
            kerberosConfigV1Service.createNewKerberosConfig(data.getEnvironmentCrn(), data.getSuffix(), stack, true);
            return new CreateBindUserEvent(CreateBindUserFlowEvent.CREATE_KERBEROS_BIND_USER_FINISHED_EVENT.event(), data);
        } catch (FreeIpaClientException e) {
            LOGGER.error("Couldn't create Kerberos bind user: {}", data, e);
            return defaultFailureEvent(data.getResourceId(), e, event);
        }
    }
}
