package com.sequenceiq.freeipa.flow.freeipa.rebuild.handler;

import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.flow.reactor.api.handler.ExceptionCatcherEventHandler;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;
import com.sequenceiq.freeipa.client.FreeIpaClient;
import com.sequenceiq.freeipa.client.FreeIpaClientException;
import com.sequenceiq.freeipa.client.model.IpaServer;
import com.sequenceiq.freeipa.entity.FreeIpa;
import com.sequenceiq.freeipa.entity.InstanceMetaData;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.flow.freeipa.rebuild.event.cleanup.FreeIpaCleanupAfterRestoreFailed;
import com.sequenceiq.freeipa.flow.freeipa.rebuild.event.cleanup.FreeIpaCleanupAfterRestoreRequest;
import com.sequenceiq.freeipa.flow.freeipa.rebuild.event.cleanup.FreeIpaCleanupAfterRestoreSuccess;
import com.sequenceiq.freeipa.service.freeipa.FreeIpaClientFactory;
import com.sequenceiq.freeipa.service.freeipa.FreeIpaClientRetryService;
import com.sequenceiq.freeipa.service.freeipa.FreeIpaService;
import com.sequenceiq.freeipa.service.freeipa.cleanup.CleanupService;
import com.sequenceiq.freeipa.service.stack.StackService;

@Component
public class FreeIpaCleanupAfterRestoreHandler extends ExceptionCatcherEventHandler<FreeIpaCleanupAfterRestoreRequest> {

    private static final Logger LOGGER = LoggerFactory.getLogger(FreeIpaCleanupAfterRestoreHandler.class);

    @Inject
    private CleanupService cleanupService;

    @Inject
    private StackService stackService;

    @Inject
    private FreeIpaClientFactory freeIpaClientFactory;

    @Inject
    private FreeIpaService freeIpaService;

    @Inject
    private FreeIpaClientRetryService retryService;

    @Override
    public String selector() {
        return EventSelectorUtil.selector(FreeIpaCleanupAfterRestoreRequest.class);
    }

    @Override
    protected Selectable defaultFailureEvent(Long resourceId, Exception e, Event<FreeIpaCleanupAfterRestoreRequest> event) {
        return new FreeIpaCleanupAfterRestoreFailed(resourceId, e);
    }

    @Override
    protected Selectable doAccept(HandlerEvent<FreeIpaCleanupAfterRestoreRequest> event) {
        FreeIpaCleanupAfterRestoreRequest request = event.getData();
        Stack stack = stackService.getByIdWithListsInTransaction(request.getResourceId());
        FreeIpa freeIpa = freeIpaService.findByStackId(request.getResourceId());
        InstanceMetaData pgw = stack.getPrimaryGatewayAndThrowExceptionIfEmpty();
        try {
            Set<String> serversToCleanup = fetchAllServers(stack, pgw);
            LOGGER.info("Primary gateway: [{}] Servers to cleanup: {}", pgw, serversToCleanup);
            if (!serversToCleanup.isEmpty()) {
                cleanupService.removeServers(stack.getId(), serversToCleanup);
                cleanupService.removeDnsEntries(stack.getId(), serversToCleanup, Set.of(), freeIpa.getDomain(), stack.getEnvironmentCrn());
            }
            return new FreeIpaCleanupAfterRestoreSuccess(request.getResourceId());
        } catch (FreeIpaClientException e) {
            return new FreeIpaCleanupAfterRestoreFailed(request.getResourceId(), e);
        }
    }

    private Set<String> fetchAllServers(Stack stack, InstanceMetaData pgw) throws FreeIpaClientException {
        Set<IpaServer> allServers = retryService.retryWhenRetryableWithValue(() -> {
            FreeIpaClient client = freeIpaClientFactory.getFreeIpaClientForStack(stack);
            return client.findAllServers();
        });
        return allServers.stream()
                .map(IpaServer::getFqdn)
                .filter(fqdn -> !Objects.equals(pgw.getDiscoveryFQDN(), fqdn))
                .collect(Collectors.toSet());
    }
}
