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
import com.sequenceiq.freeipa.flow.freeipa.binduser.create.event.CreateLdapBindUserEvent;
import com.sequenceiq.freeipa.ldap.LdapConfig;
import com.sequenceiq.freeipa.ldap.LdapConfigService;
import com.sequenceiq.freeipa.ldap.v1.LdapConfigV1Service;
import com.sequenceiq.freeipa.service.stack.StackService;

import reactor.bus.Event;

@Component
public class LdapBindUserCreationHandler extends ExceptionCatcherEventHandler<CreateLdapBindUserEvent> {

    private static final Logger LOGGER = LoggerFactory.getLogger(LdapBindUserCreationHandler.class);

    private static final String SELECTOR = EventSelectorUtil.selector(CreateLdapBindUserEvent.class);

    @Inject
    private StackService stackService;

    @Inject
    private LdapConfigV1Service ldapConfigV1Service;

    @Inject
    private LdapConfigService ldapConfigService;

    @Override
    public String selector() {
        return SELECTOR;
    }

    @Override
    protected Selectable defaultFailureEvent(Long resourceId, Exception e, Event<CreateLdapBindUserEvent> event) {
        CreateBindUserEvent eventData = event.getData();
        String failureMsg = String.format("LDAP bind user creation failed for %s with %s", eventData.getSuffix(), e.getMessage());
        return new CreateBindUserFailureEvent(CreateBindUserFlowEvent.CREATE_BIND_USER_FAILED_EVENT.event(), eventData, failureMsg, e);
    }

    @Override
    protected Selectable doAccept(HandlerEvent<CreateLdapBindUserEvent> event) {
        CreateBindUserEvent data = event.getData();
        Optional<LdapConfig> ldapConfig = ldapConfigService.find(data.getEnvironmentCrn(), data.getAccountId(), data.getSuffix());
        if (ldapConfig.isPresent()) {
            LOGGER.info("LDAP configuration already exists: {}", ldapConfig.get());
            return new CreateBindUserEvent(CreateBindUserFlowEvent.CREATE_LDAP_BIND_USER_FINISHED_EVENT.event(), data);
        } else {
            return createLdapBindUSer(event.getEvent(), data);
        }
    }

    private Selectable createLdapBindUSer(Event<CreateLdapBindUserEvent> event, CreateBindUserEvent data) {
        Stack stack = stackService.getByEnvironmentCrnAndAccountId(data.getEnvironmentCrn(), data.getAccountId());
        MDCBuilder.buildMdcContext(stack);
        LOGGER.info("Create LDAP bind user for [{}]", data.getSuffix());
        try {
            ldapConfigV1Service.createNewLdapConfig(data.getEnvironmentCrn(), data.getSuffix(), stack, true);
            return new CreateBindUserEvent(CreateBindUserFlowEvent.CREATE_LDAP_BIND_USER_FINISHED_EVENT.event(), data);
        } catch (FreeIpaClientException e) {
            LOGGER.error("Couldn't create LDAP bind user: {}", data, e);
            return defaultFailureEvent(data.getResourceId(), e, event);
        }
    }
}
