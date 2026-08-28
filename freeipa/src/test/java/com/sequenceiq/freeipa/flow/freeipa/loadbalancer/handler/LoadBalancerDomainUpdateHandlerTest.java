package com.sequenceiq.freeipa.flow.freeipa.loadbalancer.handler;

import static com.sequenceiq.freeipa.service.config.FreeIpaDomainUtils.getKdcHost;
import static com.sequenceiq.freeipa.service.config.FreeIpaDomainUtils.getKerberosHost;
import static com.sequenceiq.freeipa.service.config.FreeIpaDomainUtils.getLdapHost;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.PemDnsEntryCreateOrUpdateException;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;
import com.sequenceiq.freeipa.api.v1.dns.model.AddDnsCnameRecordRequest;
import com.sequenceiq.freeipa.client.FreeIpaClient;
import com.sequenceiq.freeipa.client.FreeIpaClientException;
import com.sequenceiq.freeipa.entity.FreeIpa;
import com.sequenceiq.freeipa.entity.LoadBalancer;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.flow.freeipa.common.FailureType;
import com.sequenceiq.freeipa.flow.freeipa.loadbalancer.event.LoadBalancerCreationFailureEvent;
import com.sequenceiq.freeipa.flow.freeipa.loadbalancer.event.update.LoadBalancerDomainUpdateRequest;
import com.sequenceiq.freeipa.flow.freeipa.loadbalancer.event.update.LoadBalancerDomainUpdateSuccess;
import com.sequenceiq.freeipa.service.freeipa.FreeIpaClientFactory;
import com.sequenceiq.freeipa.service.freeipa.FreeIpaService;
import com.sequenceiq.freeipa.service.freeipa.dns.DnsRecordService;
import com.sequenceiq.freeipa.service.loadbalancer.FreeIpaLoadBalancerDomainService;
import com.sequenceiq.freeipa.service.loadbalancer.FreeIpaLoadBalancerService;
import com.sequenceiq.freeipa.service.stack.StackService;

@ExtendWith(MockitoExtension.class)
class LoadBalancerDomainUpdateHandlerTest {

    private static final Long STACK_ID = 1L;

    private static final String DOMAIN = "test.example.com";

    private static final String LB_FQDN = "lb.test.example.com";

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
    private FreeIpaLoadBalancerDomainService loadBalancerDomainService;

    @Mock
    private FreeIpaClient freeIpaClient;

    @InjectMocks
    private LoadBalancerDomainUpdateHandler underTest;

    private HandlerEvent<LoadBalancerDomainUpdateRequest> handlerEvent;

    private Stack stack;

    private FreeIpa freeIpa;

    private LoadBalancer loadBalancer;

    @BeforeEach
    void setUp() {
        LoadBalancerDomainUpdateRequest request = new LoadBalancerDomainUpdateRequest(STACK_ID);
        handlerEvent = new HandlerEvent<>(new Event<>(request));
        stack = new Stack();
        freeIpa = new FreeIpa();
        freeIpa.setDomain(DOMAIN);
        loadBalancer = new LoadBalancer();
        loadBalancer.setFqdn(LB_FQDN);
    }

    @Test
    void selector() {
        assertEquals(EventSelectorUtil.selector(LoadBalancerDomainUpdateRequest.class), underTest.selector());
    }

    @Test
    void doAcceptCreatesCnameRecordsAndRegistersLbDomain() throws Exception {
        when(stackService.getByIdWithListsInTransaction(STACK_ID)).thenReturn(stack);
        when(freeIpaService.findByStack(stack)).thenReturn(freeIpa);
        when(freeIpaClientFactory.getFreeIpaClientForStack(stack)).thenReturn(freeIpaClient);
        when(freeIpaLoadBalancerService.getByStackId(STACK_ID)).thenReturn(loadBalancer);

        Selectable result = underTest.doAccept(handlerEvent);

        assertThat(result).isInstanceOf(LoadBalancerDomainUpdateSuccess.class);
        ArgumentCaptor<Set<AddDnsCnameRecordRequest>> captor = ArgumentCaptor.forClass(Set.class);
        verify(dnsRecordService).addOrUpdateMultipleDnsCnameRecords(eq(freeIpa), eq(freeIpaClient), captor.capture());
        Set<AddDnsCnameRecordRequest> requests = captor.getValue();
        assertEquals(3, requests.size());
        Set<String> cnames = requests.stream().map(AddDnsCnameRecordRequest::getCname).collect(Collectors.toSet());
        assertEquals(Set.of(getKdcHost(), getKerberosHost(), getLdapHost()), cnames);
        requests.forEach(r -> {
            assertEquals(DOMAIN, r.getDnsZone());
            assertEquals(LB_FQDN, r.getTargetFqdn());
            assertThat(r.isForce()).isTrue();
        });
        verify(loadBalancerDomainService).registerLbDomain(STACK_ID);
    }

    @Test
    void doAcceptReturnsFailureEventOnFreeIpaClientException() throws Exception {
        when(stackService.getByIdWithListsInTransaction(STACK_ID)).thenReturn(stack);
        when(freeIpaService.findByStack(stack)).thenReturn(freeIpa);
        when(freeIpaClientFactory.getFreeIpaClientForStack(stack)).thenReturn(freeIpaClient);
        when(freeIpaLoadBalancerService.getByStackId(STACK_ID)).thenReturn(loadBalancer);
        doThrow(new FreeIpaClientException("dns error")).when(dnsRecordService)
                .addOrUpdateMultipleDnsCnameRecords(eq(freeIpa), eq(freeIpaClient), any());

        Selectable result = underTest.doAccept(handlerEvent);

        assertThat(result).isInstanceOf(LoadBalancerCreationFailureEvent.class);
        verify(loadBalancerDomainService, org.mockito.Mockito.never()).registerLbDomain(any());
    }

    @Test
    void doAcceptReturnsFailureEventOnPemDnsEntryException() throws Exception {
        when(stackService.getByIdWithListsInTransaction(STACK_ID)).thenReturn(stack);
        when(freeIpaService.findByStack(stack)).thenReturn(freeIpa);
        when(freeIpaClientFactory.getFreeIpaClientForStack(stack)).thenReturn(freeIpaClient);
        when(freeIpaLoadBalancerService.getByStackId(STACK_ID)).thenReturn(loadBalancer);
        doThrow(new PemDnsEntryCreateOrUpdateException("pem failure")).when(loadBalancerDomainService).registerLbDomain(STACK_ID);

        Selectable result = underTest.doAccept(handlerEvent);

        assertThat(result).isInstanceOf(LoadBalancerCreationFailureEvent.class);
    }

    @Test
    void defaultFailureEvent() {
        Exception e = new Exception("failure");
        Selectable result = underTest.defaultFailureEvent(STACK_ID, e, handlerEvent.getEvent());
        assertThat(result).isInstanceOf(LoadBalancerCreationFailureEvent.class);
        LoadBalancerCreationFailureEvent failureEvent = (LoadBalancerCreationFailureEvent) result;
        assertEquals(FailureType.ERROR, failureEvent.getFailureType());
        assertEquals(e, failureEvent.getException());
    }
}
