package com.sequenceiq.freeipa.flow.freeipa.migration.handler;

import java.util.Set;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.PemDnsEntryCreateOrUpdateException;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.flow.reactor.api.handler.ExceptionCatcherEventHandler;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;
import com.sequenceiq.freeipa.client.FreeIpaClient;
import com.sequenceiq.freeipa.client.FreeIpaClientException;
import com.sequenceiq.freeipa.entity.FreeIpa;
import com.sequenceiq.freeipa.entity.LoadBalancer;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.flow.freeipa.common.FailureType;
import com.sequenceiq.freeipa.flow.freeipa.migration.event.MultiAzMigrationInitFailedEvent;
import com.sequenceiq.freeipa.flow.freeipa.migration.event.MultiAzMigrationLbDnsUpdateHandlerRequest;
import com.sequenceiq.freeipa.flow.freeipa.migration.event.MultiAzMigrationLbDnsUpdateResult;
import com.sequenceiq.freeipa.service.freeipa.FreeIpaClientFactory;
import com.sequenceiq.freeipa.service.freeipa.FreeIpaService;
import com.sequenceiq.freeipa.service.freeipa.dns.DnsRecordService;
import com.sequenceiq.freeipa.service.loadbalancer.FreeIpaLoadBalancerDomainService;
import com.sequenceiq.freeipa.service.loadbalancer.FreeIpaLoadBalancerService;
import com.sequenceiq.freeipa.service.stack.StackService;

@Component
public class MultiAzMigrationLbDnsUpdateHandler extends ExceptionCatcherEventHandler<MultiAzMigrationLbDnsUpdateHandlerRequest> {

    private static final Logger LOGGER = LoggerFactory.getLogger(MultiAzMigrationLbDnsUpdateHandler.class);

    @Inject
    private StackService stackService;

    @Inject
    private FreeIpaService freeIpaService;

    @Inject
    private FreeIpaClientFactory freeIpaClientFactory;

    @Inject
    private FreeIpaLoadBalancerService freeIpaLoadBalancerService;

    @Inject
    private DnsRecordService dnsRecordService;

    @Inject
    private FreeIpaLoadBalancerDomainService freeIpaLoadBalancerDomainService;

    @Override
    public String selector() {
        return EventSelectorUtil.selector(MultiAzMigrationLbDnsUpdateHandlerRequest.class);
    }

    @Override
    protected Selectable defaultFailureEvent(Long resourceId, Exception e, Event<MultiAzMigrationLbDnsUpdateHandlerRequest> event) {
        LOGGER.warn("Exception during multi-AZ migration load balancer DNS A record update for stack {}: ", resourceId, e);
        return new MultiAzMigrationInitFailedEvent(resourceId, e);
    }

    @Override
    public Selectable doAccept(HandlerEvent<MultiAzMigrationLbDnsUpdateHandlerRequest> event) {
        Long stackId = event.getData().getResourceId();
        String operationId = event.getData().getOperationId();
        Stack stack = stackService.getByIdWithListsInTransaction(stackId);
        FreeIpa freeIpa = freeIpaService.findByStack(stack);

        try {
            FreeIpaClient freeIpaClient = freeIpaClientFactory.getFreeIpaClientForStack(stack);
            LoadBalancer loadBalancer = freeIpaLoadBalancerService.getByStackId(stackId);
            String dnsZone = freeIpa.getDomain();
            String hostname = loadBalancer.getEndpoint();
            Set<String> ips = loadBalancer.getIp();

            // Reverse records are left to registerLbDomain below, which resolves a conflicting PTR instead of failing on it.
            dnsRecordService.reconcileDnsARecord(freeIpa, freeIpaClient, dnsZone, hostname, ips, stack.getEnvironmentCrn());
            LOGGER.info("Reconciled FreeIPA DNS A record for stack {} in zone [{}] with hostname [{}] against LB IPs {}", stackId, dnsZone, hostname, ips);
            freeIpaLoadBalancerDomainService.registerLbDomain(stackId, freeIpaClient);
            LOGGER.info("Registered load balancer domain for stack {} with LB IPs {} in PEM", stackId, ips);
            return new MultiAzMigrationLbDnsUpdateResult(stackId, operationId);
        } catch (FreeIpaClientException | PemDnsEntryCreateOrUpdateException e) {
            LOGGER.error("Failed to update FreeIPA load balancer DNS records.", e);
            return new MultiAzMigrationInitFailedEvent(stackId, FailureType.ERROR, e);
        }
    }

}
