package com.sequenceiq.freeipa.service.loadbalancer;

import static com.sequenceiq.environment.environment.dto.FreeIpaLoadBalancerType.INTERNAL_NLB;
import static com.sequenceiq.environment.environment.dto.FreeIpaLoadBalancerType.NONE;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.image.Image;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.service.client.CachedEnvironmentClientService;
import com.sequenceiq.freeipa.service.image.ImageService;
import com.sequenceiq.freeipa.service.stack.StackService;
import com.sequenceiq.freeipa.util.CrnService;

@ExtendWith(MockitoExtension.class)
class FreeIpaLoadBalancerProvisionConditionTest {

    private static final long STACK_ID = 1L;

    private static final Set<String> SUPPORTED_VARIANTS = Set.of("AZURE", "GCP", "AWS_NATIVE");

    private static final String ACCOUNT_ID = "accountId";

    private static final String ENV_CRN = "envCrn";

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

    @Mock
    private ImageService imageService;

    @Mock
    private Image image;

    @Mock
    private CachedEnvironmentClientService environmentService;

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
        when(imageService.getImageForStack(stack)).thenReturn(image);
        when(image.getPackageVersions()).thenReturn(Map.of("freeipa-health-agent", "2.1.0.2-b2228"));

        assertTrue(underTest.loadBalancerProvisionEnabled(STACK_ID, INTERNAL_NLB));
    }

    @Test
    void testHealthAgentVersionNewerShouldReturnTrue() {
        when(stack.getPlatformvariant()).thenReturn("AZURE");
        when(entitlementService.isFreeIpaLoadBalancerEnabled(ACCOUNT_ID)).thenReturn(true);
        when(imageService.getImageForStack(stack)).thenReturn(image);
        when(image.getPackageVersions()).thenReturn(Map.of("freeipa-health-agent", "2.1.0.2-b2228"));

        assertTrue(underTest.loadBalancerProvisionEnabled(STACK_ID, INTERNAL_NLB));
    }

    @Test
    void testHealthAgentVersionOlderShouldReturnFalse() {
        when(stack.getPlatformvariant()).thenReturn("AZURE");
        when(entitlementService.isFreeIpaLoadBalancerEnabled(ACCOUNT_ID)).thenReturn(true);
        when(imageService.getImageForStack(stack)).thenReturn(image);
        when(image.getPackageVersions()).thenReturn(Map.of("freeipa-health-agent", "0.1-20240222112618git0dd472a"));

        assertFalse(underTest.loadBalancerProvisionEnabled(STACK_ID, INTERNAL_NLB));
    }

    @Test
    void testLoadBalancerProvisionEnabledShouldReturnFalseWhenTheVariantIsNotSupported() {
        when(stack.getPlatformvariant()).thenReturn("AWS");
        when(entitlementService.isFreeIpaLoadBalancerEnabled(ACCOUNT_ID)).thenReturn(true);

        assertFalse(underTest.loadBalancerProvisionEnabled(STACK_ID, INTERNAL_NLB));
    }

    @Test
    void testLoadBalancerProvisionEnabledShouldReturnFalseWhenTheEntitlementDisabled() {
        when(stack.getPlatformvariant()).thenReturn("AZURE");
        when(stack.getEnvironmentCrn()).thenReturn(ENV_CRN);
        when(entitlementService.isFreeIpaLoadBalancerEnabled(ACCOUNT_ID)).thenReturn(false);
        when(environmentService.getByCrn(ENV_CRN)).thenReturn(new DetailedEnvironmentResponse());

        assertFalse(underTest.loadBalancerProvisionEnabled(STACK_ID, INTERNAL_NLB));
    }

    @Test
    void testLoadBalancerProvisionEnabledShouldReturnTrueWhenTheEntitlementDisabledWithHybridEnvironment() {
        when(stack.getPlatformvariant()).thenReturn("AZURE");
        when(stack.getEnvironmentCrn()).thenReturn(ENV_CRN);
        when(entitlementService.isFreeIpaLoadBalancerEnabled(ACCOUNT_ID)).thenReturn(false);
        DetailedEnvironmentResponse environmentResponse = new DetailedEnvironmentResponse();
        environmentResponse.setEnvironmentType("HYBRID");
        when(environmentService.getByCrn(ENV_CRN)).thenReturn(environmentResponse);

        assertFalse(underTest.loadBalancerProvisionEnabled(STACK_ID, INTERNAL_NLB));
    }

    @Test
    void testLoadBalancerProvisionEnabledShouldReturnFalseWhenLoadBalancerTypeDisabled() {
        when(stack.getPlatformvariant()).thenReturn("AZURE");
        when(entitlementService.isFreeIpaLoadBalancerEnabled(ACCOUNT_ID)).thenReturn(true);

        assertFalse(underTest.loadBalancerProvisionEnabled(STACK_ID, NONE));
    }

}