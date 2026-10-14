package com.sequenceiq.freeipa.converter.stack;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.structuredevent.event.cdp.freeipa.FreeIpaStackDetails;
import com.sequenceiq.common.api.type.Tunnel;
import com.sequenceiq.freeipa.converter.image.ImageEntityToImageDetailsConverter;
import com.sequenceiq.freeipa.converter.instance.InstanceGroupToInstanceGroupDetailsConverter;
import com.sequenceiq.freeipa.entity.CrossRealmTrust;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.entity.util.TrustRelationshipType;
import com.sequenceiq.freeipa.service.crossrealm.CrossRealmTrustService;
import com.sequenceiq.freeipa.service.image.ImageService;
import com.sequenceiq.freeipa.service.loadbalancer.FreeIpaLoadBalancerService;
import com.sequenceiq.freeipa.service.stack.instance.InstanceGroupService;

@ExtendWith(MockitoExtension.class)
class StackToStackDetailsConverterTest {

    private static final Long STACK_ID = 1L;

    @Mock
    private ImageService imageService;

    @Mock
    private InstanceGroupService instanceGroupService;

    @Mock
    private FreeIpaLoadBalancerService freeIpaLoadBalancerService;

    @Mock
    private InstanceGroupToInstanceGroupDetailsConverter instanceGroupToInstanceGroupDetailsConverter;

    @Mock
    private ImageEntityToImageDetailsConverter imageEntityToImageDetailsConverter;

    @Mock
    private CrossRealmTrustService crossRealmTrustService;

    @InjectMocks
    private StackToStackDetailsConverter underTest;

    private Stack stack;

    @BeforeEach
    void setUp() {
        stack = new Stack();
        stack.setId(STACK_ID);
        stack.setName("freeipa");
        stack.setTunnel(Tunnel.CCMV2_JUMPGATE);
        when(instanceGroupService.findByStackId(STACK_ID)).thenReturn(Set.of());
        when(freeIpaLoadBalancerService.findByStackId(STACK_ID)).thenReturn(Optional.empty());
    }

    @ParameterizedTest
    @EnumSource(TrustRelationshipType.class)
    void testConvertWithTrust(TrustRelationshipType trustRelationshipType) {
        CrossRealmTrust crossRealmTrust = new CrossRealmTrust();
        crossRealmTrust.setTrustRelationshipType(trustRelationshipType);
        when(crossRealmTrustService.getByStackIdIfExists(STACK_ID)).thenReturn(Optional.of(crossRealmTrust));

        FreeIpaStackDetails result = underTest.convert(stack);

        assertEquals(trustRelationshipType.name(), result.getTrustRelationshipType());
        assertEquals(STACK_ID, result.getId());
        assertEquals("freeipa", result.getName());
        assertEquals("DISABLED", result.getLoadBalancerType());
    }

    @Test
    void testConvertWithoutTrust() {
        when(crossRealmTrustService.getByStackIdIfExists(STACK_ID)).thenReturn(Optional.empty());

        FreeIpaStackDetails result = underTest.convert(stack);

        assertNull(result.getTrustRelationshipType());
        assertEquals(STACK_ID, result.getId());
        assertEquals("freeipa", result.getName());
        assertEquals("DISABLED", result.getLoadBalancerType());
    }
}
