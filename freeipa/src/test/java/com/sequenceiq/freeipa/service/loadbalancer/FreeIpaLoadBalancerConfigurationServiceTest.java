package com.sequenceiq.freeipa.service.loadbalancer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.freeipa.entity.FreeIpa;
import com.sequenceiq.freeipa.entity.LoadBalancer;
import com.sequenceiq.freeipa.service.freeipa.FreeIpaService;

@ExtendWith(MockitoExtension.class)
class FreeIpaLoadBalancerConfigurationServiceTest {

    private static final Map<String, String> TARGETS = Map.of("53", "TCP_UDP", "88", "TCP_UDP", "749", "TCP", "4444", "UDP");

    private static final long STACK_ID = 1L;

    @InjectMocks
    private FreeIpaLoadBalancerConfigurationService underTest;

    @Mock
    private LoadBalancerTargets loadBalancerTargets;

    @Mock
    private FreeIpaService freeIpaService;

    @Test
    void testShouldCreateLoadBalancerConfiguration() {
        when(loadBalancerTargets.getTargets()).thenReturn(TARGETS);
        FreeIpa freeIpa = new FreeIpa();
        freeIpa.setDomain("cloudera.site");
        when(freeIpaService.findByStackId(STACK_ID)).thenReturn(freeIpa);

        LoadBalancer actual = underTest.createLoadBalancerConfiguration(STACK_ID, "freeipaStackName");

        assertEquals(STACK_ID, actual.getStackId());
        assertEquals(TARGETS.size(), actual.getTargetGroups().size());
        assertTrue(TARGETS.entrySet().stream()
                .allMatch(entry -> actual.getTargetGroups().stream()
                        .anyMatch(targetGroup ->
                                targetGroup.getTrafficPort().equals(Integer.parseInt(entry.getKey())) &&
                                        targetGroup.getProtocol().equals(entry.getValue()))));
        assertEquals("freeipaStackName" + "-lb", actual.getEndpoint());
        assertEquals("freeipaStackName" + "-lb.cloudera.site", actual.getFqdn());
    }

}