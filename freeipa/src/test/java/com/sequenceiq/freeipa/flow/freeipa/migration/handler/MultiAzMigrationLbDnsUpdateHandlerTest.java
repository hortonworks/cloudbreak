package com.sequenceiq.freeipa.flow.freeipa.migration.handler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.PemDnsEntryCreateOrUpdateException;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;
import com.sequenceiq.freeipa.client.FreeIpaClient;
import com.sequenceiq.freeipa.client.FreeIpaClientException;
import com.sequenceiq.freeipa.entity.FreeIpa;
import com.sequenceiq.freeipa.entity.LoadBalancer;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.flow.freeipa.migration.event.MultiAzMigrationInitFailedEvent;
import com.sequenceiq.freeipa.flow.freeipa.migration.event.MultiAzMigrationLbDnsUpdateHandlerRequest;
import com.sequenceiq.freeipa.flow.freeipa.migration.event.MultiAzMigrationLbDnsUpdateResult;
import com.sequenceiq.freeipa.service.freeipa.FreeIpaClientFactory;
import com.sequenceiq.freeipa.service.freeipa.FreeIpaService;
import com.sequenceiq.freeipa.service.freeipa.dns.DnsRecordService;
import com.sequenceiq.freeipa.service.loadbalancer.FreeIpaLoadBalancerDomainService;
import com.sequenceiq.freeipa.service.loadbalancer.FreeIpaLoadBalancerService;
import com.sequenceiq.freeipa.service.stack.StackService;

@ExtendWith(MockitoExtension.class)
class MultiAzMigrationLbDnsUpdateHandlerTest {

    private static final long STACK_ID = 1L;

    private static final String OPERATION_ID = "op-1";

    private static final String ENV_CRN = "env-crn";

    private static final String DOMAIN = "example.com";

    private static final String ENDPOINT = "freeipa-lb";

    @Mock
    private StackService stackService;

    @Mock
    private FreeIpaService freeIpaService;

    @Mock
    private FreeIpaClientFactory freeIpaClientFactory;

    @Mock
    private FreeIpaLoadBalancerService freeIpaLoadBalancerService;

    @Mock
    private DnsRecordService dnsRecordService;

    @Mock
    private FreeIpaLoadBalancerDomainService freeIpaLoadBalancerDomainService;

    @InjectMocks
    private MultiAzMigrationLbDnsUpdateHandler underTest;

    @Test
    void testSelector() {
        assertEquals(EventSelectorUtil.selector(MultiAzMigrationLbDnsUpdateHandlerRequest.class), underTest.selector());
    }

    @Test
    void testDefaultFailureEvent() {
        Exception e = new Exception("test");

        Selectable result = underTest.defaultFailureEvent(STACK_ID, e,
                new Event<>(new MultiAzMigrationLbDnsUpdateHandlerRequest(STACK_ID, OPERATION_ID)));

        assertThat(result).isInstanceOf(MultiAzMigrationInitFailedEvent.class);
        MultiAzMigrationInitFailedEvent failure = (MultiAzMigrationInitFailedEvent) result;
        assertEquals(STACK_ID, failure.getResourceId());
        assertEquals(e, failure.getException());
    }

    @Test
    void testDoAcceptReconcilesDnsARecord() throws Exception {
        Stack stack = stack();
        FreeIpa freeIpa = freeIpa();
        FreeIpaClient client = mock(FreeIpaClient.class);
        LoadBalancer loadBalancer = new LoadBalancer();
        loadBalancer.setEndpoint(ENDPOINT);
        Set<String> ips = Set.of("10.0.0.1", "10.0.1.1");
        loadBalancer.setIp(ips);

        when(stackService.getByIdWithListsInTransaction(STACK_ID)).thenReturn(stack);
        when(freeIpaService.findByStack(stack)).thenReturn(freeIpa);
        when(freeIpaClientFactory.getFreeIpaClientForStack(stack)).thenReturn(client);
        when(freeIpaLoadBalancerService.getByStackId(STACK_ID)).thenReturn(loadBalancer);

        Selectable result = underTest.doAccept(new HandlerEvent<>(new Event<>(
                new MultiAzMigrationLbDnsUpdateHandlerRequest(STACK_ID, OPERATION_ID))));

        assertThat(result).isInstanceOf(MultiAzMigrationLbDnsUpdateResult.class);
        MultiAzMigrationLbDnsUpdateResult dnsResult = (MultiAzMigrationLbDnsUpdateResult) result;
        assertEquals(STACK_ID, dnsResult.getResourceId());
        assertEquals(OPERATION_ID, dnsResult.getOperationId());

        verify(dnsRecordService).reconcileDnsARecord(freeIpa, client, DOMAIN, ENDPOINT, ips, ENV_CRN);
        verify(freeIpaLoadBalancerDomainService).registerLbDomain(STACK_ID, client);
    }

    @Test
    void testDoAcceptReturnsFailedEventOnPemDnsEntryCreateOrUpdateException() throws Exception {
        Stack stack = stack();
        FreeIpa freeIpa = freeIpa();
        FreeIpaClient client = mock(FreeIpaClient.class);
        LoadBalancer loadBalancer = new LoadBalancer();
        loadBalancer.setEndpoint(ENDPOINT);
        loadBalancer.setIp(Set.of("10.0.0.1"));
        PemDnsEntryCreateOrUpdateException pemException = new PemDnsEntryCreateOrUpdateException("boom");

        when(stackService.getByIdWithListsInTransaction(STACK_ID)).thenReturn(stack);
        when(freeIpaService.findByStack(stack)).thenReturn(freeIpa);
        when(freeIpaClientFactory.getFreeIpaClientForStack(stack)).thenReturn(client);
        when(freeIpaLoadBalancerService.getByStackId(STACK_ID)).thenReturn(loadBalancer);
        doThrow(pemException).when(freeIpaLoadBalancerDomainService).registerLbDomain(STACK_ID, client);

        Selectable result = underTest.doAccept(new HandlerEvent<>(new Event<>(
                new MultiAzMigrationLbDnsUpdateHandlerRequest(STACK_ID, OPERATION_ID))));

        assertThat(result).isInstanceOf(MultiAzMigrationInitFailedEvent.class);
        MultiAzMigrationInitFailedEvent failure = (MultiAzMigrationInitFailedEvent) result;
        assertEquals(STACK_ID, failure.getResourceId());
        assertEquals(pemException, failure.getException());
    }

    @Test
    void testDoAcceptReturnsFailedEventOnFreeIpaClientException() throws FreeIpaClientException {
        Stack stack = stack();
        FreeIpa freeIpa = freeIpa();
        FreeIpaClientException clientException = new FreeIpaClientException("boom");

        when(stackService.getByIdWithListsInTransaction(STACK_ID)).thenReturn(stack);
        when(freeIpaService.findByStack(stack)).thenReturn(freeIpa);
        doThrow(clientException).when(freeIpaClientFactory).getFreeIpaClientForStack(stack);

        Selectable result = underTest.doAccept(new HandlerEvent<>(new Event<>(
                new MultiAzMigrationLbDnsUpdateHandlerRequest(STACK_ID, OPERATION_ID))));

        assertThat(result).isInstanceOf(MultiAzMigrationInitFailedEvent.class);
        MultiAzMigrationInitFailedEvent failure = (MultiAzMigrationInitFailedEvent) result;
        assertEquals(STACK_ID, failure.getResourceId());
        assertEquals(clientException, failure.getException());
    }

    private Stack stack() {
        Stack stack = new Stack();
        stack.setId(STACK_ID);
        stack.setEnvironmentCrn(ENV_CRN);
        return stack;
    }

    private FreeIpa freeIpa() {
        FreeIpa freeIpa = new FreeIpa();
        freeIpa.setDomain(DOMAIN);
        return freeIpa;
    }
}
