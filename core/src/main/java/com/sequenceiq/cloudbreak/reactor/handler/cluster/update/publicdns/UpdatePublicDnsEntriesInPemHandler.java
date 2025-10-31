package com.sequenceiq.cloudbreak.reactor.handler.cluster.update.publicdns;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.update.publicdns.UpdatePublicDnsEntriesInPemFailed;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.update.publicdns.UpdatePublicDnsEntriesInPemFinished;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.update.publicdns.UpdatePublicDnsEntriesInPemRequest;
import com.sequenceiq.cloudbreak.service.publicendpoint.ClusterPublicEndpointManagementService;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;
import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.flow.reactor.api.handler.ExceptionCatcherEventHandler;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;

@Component
public class UpdatePublicDnsEntriesInPemHandler extends ExceptionCatcherEventHandler<UpdatePublicDnsEntriesInPemRequest> {

    private static final Logger LOGGER = LoggerFactory.getLogger(UpdatePublicDnsEntriesInPemHandler.class);

    @Inject
    private ClusterPublicEndpointManagementService clusterPublicEndpointManagementService;

    @Inject
    private StackDtoService stackDtoService;

    @Override
    protected Selectable defaultFailureEvent(Long resourceId, Exception e, Event<UpdatePublicDnsEntriesInPemRequest> event) {
        LOGGER.warn("Update public dns entries in PEM failed for stack with id: {} with exception: ", resourceId, e);
        return new UpdatePublicDnsEntriesInPemFailed(resourceId, e);
    }

    @Override
    protected Selectable doAccept(HandlerEvent<UpdatePublicDnsEntriesInPemRequest> event) {
        UpdatePublicDnsEntriesInPemRequest eventData = event.getData();
        Long stackId = eventData.getResourceId();
        LOGGER.debug("Handling update of public DNS entries for stack with id: {}", stackId);
        StackDto stackDto = stackDtoService.getById(stackId);
        LOGGER.info("Updating cluster public DNS entries for stack with id/name: {}/{}", stackId,  stackDto.getName());
        clusterPublicEndpointManagementService.refreshDnsEntries(stackDto);
        LOGGER.info("Successfully updated cluster public DNS entries for stack with id/name: {}/{}", stackId,  stackDto.getName());
        return new UpdatePublicDnsEntriesInPemFinished(stackId);
    }

    @Override
    public String selector() {
        return EventSelectorUtil.selector(UpdatePublicDnsEntriesInPemRequest.class);
    }
}
