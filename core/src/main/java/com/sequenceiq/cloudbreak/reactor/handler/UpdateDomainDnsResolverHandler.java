package com.sequenceiq.cloudbreak.reactor.handler;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.domain.stack.DnsResolverType;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.cloudbreak.reactor.api.event.StackFailureEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.stack.UpdateDomainDnsResolverRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.stack.UpdateDomainDnsResolverResult;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;
import com.sequenceiq.cloudbreak.service.stack.TargetedUpscaleSupportService;
import com.sequenceiq.cloudbreak.view.StackView;
import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.flow.reactor.api.handler.ExceptionCatcherEventHandler;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;

@Component
public class UpdateDomainDnsResolverHandler extends ExceptionCatcherEventHandler<UpdateDomainDnsResolverRequest> {

    private static final Logger LOGGER = LoggerFactory.getLogger(UpdateDomainDnsResolverHandler.class);

    @Inject
    private TargetedUpscaleSupportService targetedUpscaleSupportService;

    @Inject
    private StackDtoService stackDtoService;

    @Override
    public String selector() {
        return EventSelectorUtil.selector(UpdateDomainDnsResolverRequest.class);
    }

    @Override
    protected Selectable defaultFailureEvent(Long resourceId, Exception e, Event<UpdateDomainDnsResolverRequest> event) {
        return new StackFailureEvent(EventSelectorUtil.failureSelector(UpdateDomainDnsResolverResult.class), resourceId, e);
    }

    @Override
    protected Selectable doAccept(HandlerEvent<UpdateDomainDnsResolverRequest> updateDomainDnsResolverRequestHandlerEvent) {
        UpdateDomainDnsResolverRequest event = updateDomainDnsResolverRequestHandlerEvent.getData();
        try {
            StackDto stackDto = stackDtoService.getById(event.getResourceId());
            StackView stack = stackDto.getStack();
            DnsResolverType actualDnsResolverType = targetedUpscaleSupportService.getActualDnsResolverType(stackDto);
            if (!actualDnsResolverType.equals(stack.getDomainDnsResolver())) {
                LOGGER.debug("New value of domainDnsResolver field for stack {} is {}", stack.getResourceCrn(), actualDnsResolverType);
                stackDtoService.updateDomainDnsResolver(stack.getId(), actualDnsResolverType);
                LOGGER.debug("domainDnsResolver field of stack {} has been successfully updated!", stack.getResourceCrn());
            } else {
                LOGGER.debug("Currently set and actual domainDnsResolverType is the same, not need for update, value {}", actualDnsResolverType);
            }
        } catch (Exception e) {
            LOGGER.debug("We couldn't update domainDnsResolver field of stack, so we move on with current value of the field.", e);
        }
        return new UpdateDomainDnsResolverResult(event.getResourceId());
    }
}
