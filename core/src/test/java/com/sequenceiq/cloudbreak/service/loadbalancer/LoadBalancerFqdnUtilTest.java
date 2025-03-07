package com.sequenceiq.cloudbreak.service.loadbalancer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.domain.stack.loadbalancer.LoadBalancer;
import com.sequenceiq.cloudbreak.service.publicendpoint.GatewayPublicEndpointManagementService;
import com.sequenceiq.cloudbreak.service.stack.LoadBalancerPersistenceService;
import com.sequenceiq.common.api.type.LoadBalancerType;

@ExtendWith(MockitoExtension.class)
class LoadBalancerFqdnUtilTest {

    private static final String PUBLIC_DNS = "public.dns";

    private static final String PUBLIC_FQDN = "public.fqdn";

    private static final String PUBLIC_IP = "public.ip";

    private static final String PRIVATE_DNS = "private.dns";

    private static final String PRIVATE_FQDN = "private.fqdn";

    private static final String PRIVATE_IP = "private.ip";

    private static final long STACK_ID = 123L;

    @Mock
    private LoadBalancerPersistenceService loadBalancerPersistenceService;

    @Mock
    private GatewayPublicEndpointManagementService gatewayPublicEndpointManagementService;

    @InjectMocks
    private LoadBalancerFqdnUtil underTest;

    @Test
    void testGetLoadBalancerUserFacingFQDN() {
        Set<LoadBalancer> loadBalancers = createLoadBalancers();

        when(loadBalancerPersistenceService.findByStackId(0L)).thenReturn(loadBalancers);
        when(gatewayPublicEndpointManagementService.isPemEnabled()).thenReturn(true);

        String fqdn = underTest.getLoadBalancerUserFacingFQDN(0L);
        assertEquals(PUBLIC_FQDN, fqdn);
    }

    @Test
    void testGetLoadBalancerUserFacingFQDNNoLBs() {
        Set<LoadBalancer> loadBalancers = Set.of();

        when(loadBalancerPersistenceService.findByStackId(0L)).thenReturn(loadBalancers);

        String fqdn = underTest.getLoadBalancerUserFacingFQDN(0L);
        assertNull(fqdn);
    }

    @Test
    void testGetLoadBalancerUserFacingFQDNPrivateOnly() {
        Set<LoadBalancer> loadBalancers = createPrivateOnlyLoadBalancer();

        when(loadBalancerPersistenceService.findByStackId(0L)).thenReturn(loadBalancers);
        when(gatewayPublicEndpointManagementService.isPemEnabled()).thenReturn(true);

        String fqdn = underTest.getLoadBalancerUserFacingFQDN(0L);
        assertEquals(PRIVATE_FQDN, fqdn);
    }

    @Test
    void testGetLoadBalancerUserFacingFQDNPublicMissingAll() {
        Set<LoadBalancer> loadBalancers = createLoadBalancers();
        LoadBalancer publicLoadBalancer = loadBalancers.stream()
                .filter(lb -> LoadBalancerType.PUBLIC.equals(lb.getType()))
                .findFirst().get();
        publicLoadBalancer.setFqdn(null);
        publicLoadBalancer.setIp(null);
        publicLoadBalancer.setDns(null);

        when(loadBalancerPersistenceService.findByStackId(0L)).thenReturn(loadBalancers);
        when(gatewayPublicEndpointManagementService.isPemEnabled()).thenReturn(true);

        String fqdn = underTest.getLoadBalancerUserFacingFQDN(0L);
        assertEquals(PRIVATE_FQDN, fqdn);
    }

    @Test
    void testGetLoadBalancerUserFacingFQDNPublicMissingFQDN() {
        Set<LoadBalancer> loadBalancers = createLoadBalancers();
        LoadBalancer publicLoadBalancer = loadBalancers.stream()
                .filter(lb -> LoadBalancerType.PUBLIC.equals(lb.getType()))
                .findFirst().get();
        publicLoadBalancer.setFqdn(null);

        when(loadBalancerPersistenceService.findByStackId(0L)).thenReturn(loadBalancers);
        when(gatewayPublicEndpointManagementService.isPemEnabled()).thenReturn(true);

        String fqdn = underTest.getLoadBalancerUserFacingFQDN(0L);
        assertEquals(PUBLIC_DNS, fqdn);
    }

    @Test
    void testGetLoadBalancerUserFacingFQDNNoPublicPrivateMissingFQDN() {
        Set<LoadBalancer> loadBalancers = createPrivateOnlyLoadBalancer();
        LoadBalancer privateLoadBalancer = loadBalancers.stream()
                .filter(lb -> LoadBalancerType.PRIVATE.equals(lb.getType()))
                .findFirst().get();
        privateLoadBalancer.setFqdn(null);
        privateLoadBalancer.setIp(null);

        when(loadBalancerPersistenceService.findByStackId(0L)).thenReturn(loadBalancers);
        when(gatewayPublicEndpointManagementService.isPemEnabled()).thenReturn(true);

        String fqdn = underTest.getLoadBalancerUserFacingFQDN(0L);
        assertEquals(PRIVATE_DNS, fqdn);
    }

    @Test
    void testGetLoadBalancerUserFacingFQDNNoPublicPrivateMissingFQDNNoDNS() {
        Set<LoadBalancer> loadBalancers = createPrivateOnlyLoadBalancer();
        LoadBalancer privateLoadBalancer = loadBalancers.stream()
                .filter(lb -> LoadBalancerType.PRIVATE.equals(lb.getType()))
                .findFirst().get();
        privateLoadBalancer.setFqdn(null);
        privateLoadBalancer.setDns(null);

        when(loadBalancerPersistenceService.findByStackId(0L)).thenReturn(loadBalancers);
        when(gatewayPublicEndpointManagementService.isPemEnabled()).thenReturn(true);

        String fqdn = underTest.getLoadBalancerUserFacingFQDN(0L);
        assertEquals(PRIVATE_IP, fqdn);
    }

    @Test
    void testGetLoadBalancerUserFacingFQDNIPOnlyMissingFQDN() {
        Set<LoadBalancer> loadBalancers = createLoadBalancers();
        LoadBalancer publicLoadBalancer = loadBalancers.stream()
                .filter(lb -> LoadBalancerType.PUBLIC.equals(lb.getType()))
                .findFirst().get();
        publicLoadBalancer.setFqdn(null);
        publicLoadBalancer.setDns(null);

        when(loadBalancerPersistenceService.findByStackId(0L)).thenReturn(loadBalancers);
        when(gatewayPublicEndpointManagementService.isPemEnabled()).thenReturn(true);

        String fqdn = underTest.getLoadBalancerUserFacingFQDN(0L);
        assertEquals(PUBLIC_IP, fqdn);
    }

    @Test
    void testGetLoadBalancerUserFacingFQDNNoFqdnSet() {
        Set<LoadBalancer> loadBalancers = createLoadBalancers();
        LoadBalancer publicLoadBalancer = loadBalancers.stream()
                .filter(lb -> LoadBalancerType.PUBLIC.equals(lb.getType()))
                .findFirst().get();
        publicLoadBalancer.setFqdn(null);
        publicLoadBalancer.setIp(null);
        publicLoadBalancer.setDns(null);

        LoadBalancer privateLoadBalancer = loadBalancers.stream()
                .filter(lb -> LoadBalancerType.PRIVATE.equals(lb.getType()))
                .findFirst().get();
        privateLoadBalancer.setFqdn(null);
        privateLoadBalancer.setDns(null);
        privateLoadBalancer.setIp(null);

        when(loadBalancerPersistenceService.findByStackId(0L)).thenReturn(loadBalancers);

        String fqdn = underTest.getLoadBalancerUserFacingFQDN(0L);
        assertNull(fqdn);
    }

    @Test
    void testGetLoadBalancersForStack() {
        LoadBalancer lb1 = mock(LoadBalancer.class);
        LoadBalancer lb2 = mock(LoadBalancer.class);
        Set<LoadBalancer> loadBalancers = Set.of(lb1, lb2);

        when(loadBalancerPersistenceService.findByStackId(STACK_ID)).thenReturn(loadBalancers);

        Set<LoadBalancer> result = underTest.getLoadBalancersForStack(STACK_ID);

        verify(loadBalancerPersistenceService).findByStackId(STACK_ID);
        assertThat(result).isEqualTo(loadBalancers);
    }

    static Stream<Arguments> pemEnabledTest() {
        return Stream.of(
                arguments(Boolean.FALSE, "dns.cloudera"),
                arguments(Boolean.TRUE, "fqdn.cloudera")
        );
    }

    @ParameterizedTest
    @MethodSource("pemEnabledTest")
    void testGetLoadBalancerUserFacingFQDNUsingPemEnabled(boolean pemEnabled, String fqdn) {
        LoadBalancer lb1 = new LoadBalancer();
        lb1.setType(LoadBalancerType.PUBLIC);
        lb1.setFqdn("fqdn.cloudera");
        lb1.setDns("dns.cloudera");

        Set<LoadBalancer> loadBalancers = Set.of(lb1);
        when(loadBalancerPersistenceService.findByStackId(STACK_ID)).thenReturn(loadBalancers);
        when(gatewayPublicEndpointManagementService.isPemEnabled()).thenReturn(pemEnabled);

        String result = underTest.getLoadBalancerUserFacingFQDN(STACK_ID);

        assertThat(result).isEqualTo(fqdn);
    }

    private Set<LoadBalancer> createLoadBalancers(boolean createPrivate, boolean createPublic, boolean createOutbound) {
        Set<LoadBalancer> loadBalancers = new HashSet<>();
        if (createPrivate) {
            LoadBalancer privateLoadBalancer = new LoadBalancer();
            privateLoadBalancer.setType(LoadBalancerType.PRIVATE);
            privateLoadBalancer.setFqdn(PRIVATE_FQDN);
            privateLoadBalancer.setDns(PRIVATE_DNS);
            privateLoadBalancer.setIp(PRIVATE_IP);
            loadBalancers.add(privateLoadBalancer);
        }
        if (createPublic) {
            LoadBalancer publicLoadBalancer = new LoadBalancer();
            publicLoadBalancer.setType(LoadBalancerType.PUBLIC);
            publicLoadBalancer.setFqdn(PUBLIC_FQDN);
            publicLoadBalancer.setDns(PUBLIC_DNS);
            publicLoadBalancer.setIp(PUBLIC_IP);
            loadBalancers.add(publicLoadBalancer);
        }
        if (createOutbound) {
            LoadBalancer outboundLoadBalancer = new LoadBalancer();
            outboundLoadBalancer.setType(LoadBalancerType.OUTBOUND);
            loadBalancers.add(outboundLoadBalancer);
        }
        return loadBalancers;
    }

    private Set<LoadBalancer> createLoadBalancers() {
        return createLoadBalancers(true, true, false);
    }

    private Set<LoadBalancer> createPrivateOnlyLoadBalancer() {
        return createLoadBalancers(true, false, false);
    }
}
