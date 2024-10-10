package com.sequenceiq.environment.expressonboarding.controller;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.cloud.event.platform.GetCdpPlatformRegionsRequest;
import com.sequenceiq.cloudbreak.cloud.model.CloudRegions;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.environment.api.v1.platformresource.model.ExpressOnboardingCloudProvidersResponse;
import com.sequenceiq.environment.platformresource.PlatformParameterService;
import com.sequenceiq.environment.platformresource.v1.converter.PlatformRegionsToExpressOnboardingRegionsResponseConverter;

@ExtendWith(MockitoExtension.class)
class ExpressOnboardingCloudProviderResponseConverterTest {

    @InjectMocks
    private ExpressOnboardingRegionsResponseConverter converter;

    @Mock
    private PlatformParameterService platformParameterService;

    @Mock
    private PlatformRegionsToExpressOnboardingRegionsResponseConverter platformRegionsToExpressOnboardingRegionsResponseConverter;

    @Test
    public void testExpressOnboardingRegionsResponse() {
        GetCdpPlatformRegionsRequest mockRequest = mock(GetCdpPlatformRegionsRequest.class);
        CloudRegions mockRegions = mock(CloudRegions.class);
        ExpressOnboardingCloudProvidersResponse mockResponse = mock(ExpressOnboardingCloudProvidersResponse.class);

        when(platformParameterService.getCdpPlatformRegionsRequestV2(any(), any()))
                .thenReturn(mockRequest);
        when(platformParameterService.getCdpRegions(mockRequest)).thenReturn(mockRegions);
        when(platformRegionsToExpressOnboardingRegionsResponseConverter.convert(mockRegions)).thenReturn(mockResponse);

        Map<String, ExpressOnboardingCloudProvidersResponse> response = converter.expressOnboardingRegionsResponse();

        assertNotNull(response);
        verify(platformParameterService, times(CloudPlatform.publicCloudPlatforms().size())).getCdpPlatformRegionsRequestV2(anyString(), anyString());
        verify(platformParameterService, times(CloudPlatform.publicCloudPlatforms().size())).getCdpRegions(any());
        verify(platformRegionsToExpressOnboardingRegionsResponseConverter, times(CloudPlatform.publicCloudPlatforms().size())).convert(any());
    }
}