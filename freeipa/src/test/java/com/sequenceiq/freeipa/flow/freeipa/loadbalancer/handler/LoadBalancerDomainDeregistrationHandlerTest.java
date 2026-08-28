package com.sequenceiq.freeipa.flow.freeipa.loadbalancer.handler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.googlecode.jsonrpc4j.JsonRpcClientException;
import com.sequenceiq.cloudbreak.PemDnsEntryCreateOrUpdateException;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;
import com.sequenceiq.freeipa.client.FreeIpaClient;
import com.sequenceiq.freeipa.client.FreeIpaClientException;
import com.sequenceiq.freeipa.client.FreeIpaErrorCodes;
import com.sequenceiq.freeipa.entity.FreeIpa;
import com.sequenceiq.freeipa.entity.LoadBalancer;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.flow.freeipa.loadbalancer.event.LoadBalancerDeletionFailureEvent;
import com.sequenceiq.freeipa.flow.freeipa.loadbalancer.event.deletion.LoadBalancerDeregistrationSuccess;
import com.sequenceiq.freeipa.flow.freeipa.loadbalancer.event.deletion.LoadBalancerDomainDeregistrationRequest;
import com.sequenceiq.freeipa.service.freeipa.FreeIpaClientFactory;
import com.sequenceiq.freeipa.service.freeipa.FreeIpaService;
import com.sequenceiq.freeipa.service.freeipa.dns.DnsRecordService;
import com.sequenceiq.freeipa.service.loadbalancer.FreeIpaLoadBalancerDomainService;
import com.sequenceiq.freeipa.service.loadbalancer.FreeIpaLoadBalancerService;
import com.sequenceiq.freeipa.service.stack.StackService;

@ExtendWith(MockitoExtension.class)
class LoadBalancerDomainDeregistrationHandlerTest {

    private static final Long STACK_ID = 1L;

    private static final String DOMAIN = "test.example.com";

    private static final String ENDPOINT = "lb";

    @Mock
    private StackService stackService;

    @Mock
    private FreeIpaService freeIpaService;

    @Mock
    private FreeIpaClientFactory freeIpaClientFactory;

    @Mock
    private DnsRecordService dnsRecordService;

    @Mock
    private FreeIpaLoadBalancerDomainService loadBalancerDomainService;

    @Mock
    private FreeIpaLoadBalancerService loadBalancerService;

    @Mock
    private FreeIpaClient freeIpaClient;

    @InjectMocks
    private LoadBalancerDomainDeregistrationHandler underTest;

    private Stack stack;

    private FreeIpa freeIpa;

    @BeforeEach
    void setUp() {
        stack = new Stack();
        freeIpa = new FreeIpa();
        freeIpa.setDomain(DOMAIN);
    }

    @Test
    void doAcceptDeregistersLbDomainAndReturnsSuccess() throws Exception {
        LoadBalancerDomainDeregistrationRequest request = mock(LoadBalancerDomainDeregistrationRequest.class);
        when(request.getResourceId()).thenReturn(STACK_ID);
        when(stackService.getByIdWithListsInTransaction(STACK_ID)).thenReturn(stack);
        when(freeIpaService.findByStack(stack)).thenReturn(freeIpa);
        when(freeIpaClientFactory.getFreeIpaClientForStack(stack)).thenReturn(freeIpaClient);
        LoadBalancer loadBalancer = new LoadBalancer();
        loadBalancer.setEndpoint(ENDPOINT);
        loadBalancer.setIp(Set.of("10.0.0.5"));
        when(loadBalancerService.findByStackId(STACK_ID)).thenReturn(Optional.of(loadBalancer));

        Selectable result = underTest.doAccept(new HandlerEvent<>(Event.wrap(request)));

        assertThat(result).isInstanceOf(LoadBalancerDeregistrationSuccess.class);
        verify(dnsRecordService).addOrUpdateMultipleDnsCnameRecords(eq(freeIpa), eq(freeIpaClient), any());
        verify(freeIpaClient).deleteDnsRecord(ENDPOINT, DOMAIN);
        verify(loadBalancerDomainService).deregisterLbDomain(STACK_ID);
    }

    @Test
    void doAcceptSkipsDnsDeleteWhenNoLoadBalancerExists() throws Exception {
        LoadBalancerDomainDeregistrationRequest request = mock(LoadBalancerDomainDeregistrationRequest.class);
        when(request.getResourceId()).thenReturn(STACK_ID);
        when(stackService.getByIdWithListsInTransaction(STACK_ID)).thenReturn(stack);
        when(freeIpaService.findByStack(stack)).thenReturn(freeIpa);
        when(freeIpaClientFactory.getFreeIpaClientForStack(stack)).thenReturn(freeIpaClient);
        when(loadBalancerService.findByStackId(STACK_ID)).thenReturn(Optional.empty());

        Selectable result = underTest.doAccept(new HandlerEvent<>(Event.wrap(request)));

        assertThat(result).isInstanceOf(LoadBalancerDeregistrationSuccess.class);
        verify(freeIpaClient, org.mockito.Mockito.never()).deleteDnsRecord(any(), any());
        verify(loadBalancerDomainService).deregisterLbDomain(STACK_ID);
    }

    @Test
    void doAcceptIgnoresNotFoundOnDnsDeleteWhenRetriedAfterSuccessfulDeletion() throws Exception {
        LoadBalancerDomainDeregistrationRequest request = mock(LoadBalancerDomainDeregistrationRequest.class);
        when(request.getResourceId()).thenReturn(STACK_ID);
        when(stackService.getByIdWithListsInTransaction(STACK_ID)).thenReturn(stack);
        when(freeIpaService.findByStack(stack)).thenReturn(freeIpa);
        when(freeIpaClientFactory.getFreeIpaClientForStack(stack)).thenReturn(freeIpaClient);
        LoadBalancer loadBalancer = new LoadBalancer();
        loadBalancer.setEndpoint(ENDPOINT);
        loadBalancer.setIp(Set.of("10.0.0.5"));
        when(loadBalancerService.findByStackId(STACK_ID)).thenReturn(Optional.of(loadBalancer));
        FreeIpaClientException notFound = new FreeIpaClientException("not found",
                new JsonRpcClientException(FreeIpaErrorCodes.NOT_FOUND.getValue(), "not found", null));
        doThrow(notFound).when(freeIpaClient).deleteDnsRecord(ENDPOINT, DOMAIN);

        Selectable result = underTest.doAccept(new HandlerEvent<>(Event.wrap(request)));

        assertThat(result).isInstanceOf(LoadBalancerDeregistrationSuccess.class);
        verify(loadBalancerDomainService).deregisterLbDomain(STACK_ID);
    }

    @Test
    void doAcceptReturnsFailureEventOnPemDnsException() throws Exception {
        LoadBalancerDomainDeregistrationRequest request = mock(LoadBalancerDomainDeregistrationRequest.class);
        when(request.getResourceId()).thenReturn(STACK_ID);
        when(stackService.getByIdWithListsInTransaction(STACK_ID)).thenReturn(stack);
        when(freeIpaService.findByStack(stack)).thenReturn(freeIpa);
        when(freeIpaClientFactory.getFreeIpaClientForStack(stack)).thenReturn(freeIpaClient);
        when(loadBalancerService.findByStackId(STACK_ID)).thenReturn(Optional.empty());
        doThrow(new PemDnsEntryCreateOrUpdateException("dns error")).when(loadBalancerDomainService).deregisterLbDomain(STACK_ID);

        Selectable result = underTest.doAccept(new HandlerEvent<>(Event.wrap(request)));

        assertThat(result).isInstanceOf(LoadBalancerDeletionFailureEvent.class);
    }
}
