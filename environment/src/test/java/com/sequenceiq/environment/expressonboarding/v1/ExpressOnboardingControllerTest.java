package com.sequenceiq.environment.expressonboarding.v1;

import static com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider.doAs;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.auth.altus.exception.UnauthorizedException;
import com.sequenceiq.environment.api.v1.expressonboarding.model.response.DeploymentInformationResponse;
import com.sequenceiq.environment.api.v1.expressonboarding.model.response.DetailedExpressOnboardingRegionResponse;
import com.sequenceiq.environment.api.v1.expressonboarding.model.response.TenantInformationResponse;
import com.sequenceiq.environment.expressonboarding.controller.DeploymentInformationResponseConverter;
import com.sequenceiq.environment.expressonboarding.controller.ExpressOnboardingRegionsResponseConverter;
import com.sequenceiq.environment.expressonboarding.controller.TenantInformationResponseConverter;

@ExtendWith(MockitoExtension.class)
class ExpressOnboardingControllerTest {

    private static final String USER_CRN = "crn:altus:iam:us-west-1:12345:user:" + UUID.randomUUID();

    @Mock
    private DeploymentInformationResponseConverter deploymentInformationResponseConverter;

    @Mock
    private TenantInformationResponseConverter tenantInformationResponseConverter;

    @Mock
    private ExpressOnboardingRegionsResponseConverter expressOnboardingRegionsResponseConverter;

    @Mock
    private EntitlementService entitlementService;

    @InjectMocks
    private ExpressOnboardingController expressOnboardingController;

    @Test
    public void testGetSuccessfulResponse() {
        when(entitlementService.isExpressOnboardingEnabled(anyString())).thenReturn(true);
        when(tenantInformationResponseConverter.tenantInformationResponse()).thenReturn(new TenantInformationResponse());
        when(deploymentInformationResponseConverter.deploymentInformationResponse()).thenReturn(new DeploymentInformationResponse());
        when(expressOnboardingRegionsResponseConverter.expressOnboardingRegionsResponse()).thenReturn(new HashMap<>());

        DetailedExpressOnboardingRegionResponse response = doAs(USER_CRN, () ->
                expressOnboardingController.get());

        assertNotNull(response);
        verify(entitlementService, times(1)).isExpressOnboardingEnabled(anyString());
        verify(tenantInformationResponseConverter, times(1)).tenantInformationResponse();
        verify(deploymentInformationResponseConverter, times(1)).deploymentInformationResponse();
        verify(expressOnboardingRegionsResponseConverter, times(1)).expressOnboardingRegionsResponse();
    }

    @Test
    public void testGetUnauthorizedException() {
        when(entitlementService.isExpressOnboardingEnabled(anyString())).thenReturn(false);

        UnauthorizedException exception = assertThrows(UnauthorizedException.class, () ->
            doAs(USER_CRN, () -> expressOnboardingController.get()));

        verify(entitlementService, times(1)).isExpressOnboardingEnabled(anyString());
        assertEquals("The 'CDP_EXPRESS_ONBOARDING' not granted to your tenant. Please contact the administrator.", exception.getMessage());
    }

}