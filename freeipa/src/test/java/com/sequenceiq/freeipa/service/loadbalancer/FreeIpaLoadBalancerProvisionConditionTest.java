package com.sequenceiq.freeipa.service.loadbalancer;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.service.stack.StackService;
import com.sequenceiq.freeipa.util.CrnService;

@ExtendWith(MockitoExtension.class)
class FreeIpaLoadBalancerProvisionConditionTest {

    private static final long STACK_ID = 1L;

    private static final Set<String> SUPPORTED_VARIANTS = Set.of("AZURE", "GCP", "AWS_NATIVE");

    private static final String ACCOUNT_ID = "accountId";

    @InjectMocks
    private FreeIpaLoadBalancerProvisionCondition underTest;

    @Mock
    private EntitlementService entitlementService;

    @Mock
    private CrnService crnService;

    @Mock
    private StackService stackService;

    @Mock
    private Stack stack;

    @BeforeEach
    void before() {
        ReflectionTestUtils.setField(underTest, "supportedVariants", SUPPORTED_VARIANTS);
        when(stackService.getStackById(STACK_ID)).thenReturn(stack);
        when(crnService.getCurrentAccountId()).thenReturn(ACCOUNT_ID);
    }

    @Test
    void testLoadBalancerProvisionEnabledShouldReturnTrue() {
        when(stack.getPlatformvariant()).thenReturn("AZURE");
        when(entitlementService.isFreeIpaLoadBalancerEnabled(ACCOUNT_ID)).thenReturn(true);

        assertTrue(underTest.loadBalancerProvisionEnabled(STACK_ID));
    }

    @Test
    void testLoadBalancerProvisionEnabledShouldReturnFalseWhenTheVariantIsNotSupported() {
        when(stack.getPlatformvariant()).thenReturn("AWS");
        when(entitlementService.isFreeIpaLoadBalancerEnabled(ACCOUNT_ID)).thenReturn(true);

        assertFalse(underTest.loadBalancerProvisionEnabled(STACK_ID));
    }

    @Test
    void testLoadBalancerProvisionEnabledShouldReturnFalseWhenTheEntitlementDisabled() {
        when(stack.getPlatformvariant()).thenReturn("AZURE");
        when(entitlementService.isFreeIpaLoadBalancerEnabled(ACCOUNT_ID)).thenReturn(false);

        assertFalse(underTest.loadBalancerProvisionEnabled(STACK_ID));
    }

}