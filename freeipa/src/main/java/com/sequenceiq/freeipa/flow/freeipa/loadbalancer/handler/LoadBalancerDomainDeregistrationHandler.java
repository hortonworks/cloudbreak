package com.sequenceiq.freeipa.flow.freeipa.loadbalancer.handler;

import static com.sequenceiq.freeipa.service.config.FreeIpaDomainUtils.getIpaCaHostFqdn;
import static com.sequenceiq.freeipa.service.config.FreeIpaDomainUtils.getKdcHost;
import static com.sequenceiq.freeipa.service.config.FreeIpaDomainUtils.getKerberosHost;
import static com.sequenceiq.freeipa.service.config.FreeIpaDomainUtils.getLdapHost;

import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
import com.sequenceiq.freeipa.api.v1.dns.model.AddDnsCnameRecordRequest;
import com.sequenceiq.freeipa.client.FreeIpaClient;
import com.sequenceiq.freeipa.client.FreeIpaClientException;
import com.sequenceiq.freeipa.client.FreeIpaClientExceptionUtil;
import com.sequenceiq.freeipa.client.FreeIpaClientRunnable;
import com.sequenceiq.freeipa.entity.FreeIpa;
import com.sequenceiq.freeipa.entity.LoadBalancer;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.flow.freeipa.common.FailureType;
import com.sequenceiq.freeipa.flow.freeipa.loadbalancer.event.LoadBalancerDeletionFailureEvent;
import com.sequenceiq.freeipa.flow.freeipa.loadbalancer.event.deletion.LoadBalancerDeregistrationSuccess;
import com.sequenceiq.freeipa.flow.freeipa.loadbalancer.event.deletion.LoadBalancerDomainDeregistrationRequest;
import com.sequenceiq.freeipa.service.freeipa.FreeIpaClientFactory;
import com.sequenceiq.freeipa.service.freeipa.FreeIpaService;
import com.sequenceiq.freeipa.service.freeipa.dns.DnsRecordService;
import com.sequenceiq.freeipa.service.loadbalancer.FreeIpaLoadBalancerDomainService;
import com.sequenceiq.freeipa.service.loadbalancer.FreeIpaLoadBalancerService;
import com.sequenceiq.freeipa.service.stack.StackService;

@Component
public class LoadBalancerDomainDeregistrationHandler extends ExceptionCatcherEventHandler<LoadBalancerDomainDeregistrationRequest> {

    private static final Logger LOGGER = LoggerFactory.getLogger(LoadBalancerDomainDeregistrationHandler.class);

    @Inject
    private StackService stackService;

    @Inject
    private FreeIpaService freeIpaService;

    @Inject
    private FreeIpaClientFactory freeIpaClientFactory;

    @Inject
    private DnsRecordService dnsRecordService;

    @Inject
    private FreeIpaLoadBalancerDomainService freeIpaLoadBalancerDomainService;

    @Inject
    private FreeIpaLoadBalancerService loadBalancerService;

    @Override
    public String selector() {
        return EventSelectorUtil.selector(LoadBalancerDomainDeregistrationRequest.class);
    }

    @Override
    protected Selectable defaultFailureEvent(Long resourceId, Exception e, Event<LoadBalancerDomainDeregistrationRequest> event) {
        return new LoadBalancerDeletionFailureEvent(resourceId, FailureType.ERROR, e);
    }

    @Override
    protected Selectable doAccept(HandlerEvent<LoadBalancerDomainDeregistrationRequest> event) {
        Long stackId = event.getData().getResourceId();
        Stack stack = stackService.getByIdWithListsInTransaction(stackId);
        FreeIpa freeIpa = freeIpaService.findByStack(stack);

        try {
            FreeIpaClient freeIpaClient = freeIpaClientFactory.getFreeIpaClientForStack(stack);
            Set<AddDnsCnameRecordRequest> requests = Stream.of(getKdcHost(), getKerberosHost(), getLdapHost())
                    .map(cname -> getAddDnsCnameRecordRequest(cname, freeIpa.getDomain()))
                    .collect(Collectors.toSet());

            dnsRecordService.addOrUpdateMultipleDnsCnameRecords(freeIpa, freeIpaClient, requests);
            deleteLoadBalancerDnsRecord(stackId, freeIpa, freeIpaClient);
            freeIpaLoadBalancerDomainService.deregisterLbDomain(stackId);
            return new LoadBalancerDeregistrationSuccess(stackId);
        } catch (FreeIpaClientException | PemDnsEntryCreateOrUpdateException e) {
            LOGGER.error("Failed to deregister FreeIPA load balancer domain for stack {}", stackId, e);
            return new LoadBalancerDeletionFailureEvent(stackId, FailureType.ERROR, e);
        }
    }

    private void deleteLoadBalancerDnsRecord(Long stackId, FreeIpa freeIpa, FreeIpaClient freeIpaClient) throws FreeIpaClientException {
        Optional<LoadBalancer> loadBalancer = loadBalancerService.findByStackId(stackId);
        if (loadBalancer.isPresent()) {
            String endpoint = loadBalancer.get().getEndpoint();
            FreeIpaClientRunnable runnable = () -> freeIpaClient.deleteDnsRecord(endpoint, freeIpa.getDomain());
            FreeIpaClientExceptionUtil.ignoreNotFoundException(runnable,
                    "DNS record [{}] not found in zone [{}], nothing to delete", endpoint, freeIpa.getDomain());
        }
    }

    private static AddDnsCnameRecordRequest getAddDnsCnameRecordRequest(String cname, String domain) {
        AddDnsCnameRecordRequest request = new AddDnsCnameRecordRequest();
        request.setDnsZone(domain);
        request.setCname(cname);
        request.setTargetFqdn(getIpaCaHostFqdn(domain));
        request.setForce(true);
        return request;
    }
}
