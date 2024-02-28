package com.sequenceiq.cloudbreak.conf;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;

@ExtendWith(MockitoExtension.class)
class LimitConfigurationTest {

    @Mock
    private EntitlementService entitlementService;

    @InjectMocks
    private LimitConfiguration underTest;

    @Test
    public void testCorrectPrimaryGatewayRequirementIsReturned() {
        PrimaryGatewayRequirement gatewayRequirement1 = new PrimaryGatewayRequirement();
        gatewayRequirement1.setNodeCount(100);
        PrimaryGatewayRequirement gatewayRequirement2 = new PrimaryGatewayRequirement();
        gatewayRequirement2.setNodeCount(200);
        underTest.setPrimaryGatewayRecommendations(List.of(gatewayRequirement1, gatewayRequirement2));

        assertEquals(Optional.empty(), underTest.getPrimaryGatewayRequirement(99));
        assertEquals(Optional.of(gatewayRequirement1), underTest.getPrimaryGatewayRequirement(100));
        assertEquals(Optional.of(gatewayRequirement1), underTest.getPrimaryGatewayRequirement(199));
        assertEquals(Optional.of(gatewayRequirement2), underTest.getPrimaryGatewayRequirement(200));
        assertEquals(Optional.of(gatewayRequirement2), underTest.getPrimaryGatewayRequirement(600));
    }

    @Test
    public void testSafeNodeCountLimit() {
        when(entitlementService.isExperimentalNodeCountLimitsEnabled(any())).thenReturn(false);

        underTest.setNodeCountLimits(Map.of("safe", Map.of("cm", 200), "experimental", Map.of("cm", 300)));

        assertEquals(200, underTest.getNodeCountLimit(Optional.of("account1")));
    }

    @Test
    public void testExperimentalNodeCountLimit() {
        when(entitlementService.isExperimentalNodeCountLimitsEnabled(any())).thenReturn(true);

        underTest.setNodeCountLimits(Map.of("safe", Map.of("cm", 200), "experimental", Map.of("cm", 300)));

        assertEquals(300, underTest.getNodeCountLimit(Optional.of("account1")));
    }
}