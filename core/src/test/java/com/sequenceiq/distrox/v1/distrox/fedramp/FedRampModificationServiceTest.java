package com.sequenceiq.distrox.v1.distrox.fedramp;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashSet;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.common.gov.CommonGovService;
import com.sequenceiq.cloudbreak.common.provider.ProviderPreferencesService;
import com.sequenceiq.distrox.api.v1.distrox.model.DistroXV1Request;
import com.sequenceiq.distrox.api.v1.distrox.model.database.DistroXDatabaseAvailabilityType;
import com.sequenceiq.distrox.api.v1.distrox.model.database.DistroXDatabaseRequest;

@ExtendWith(MockitoExtension.class)
class FedRampModificationServiceTest {

    @Mock
    private CommonGovService commonGovService;

    @Mock
    private ProviderPreferencesService preferencesService;

    @Mock
    private EntitlementService entitlementService;

    @InjectMocks
    private FedRampModificationService underTest;

    @Test
    void testDistroxModificationIfGovDeploymentAndNoEntitlementGrantedThenHADbShouldBeInitiated() {
        boolean govDeployment = true;
        boolean fedrampExternalDatabaseForceDisabled = false;

        when(preferencesService.enabledGovPlatforms()).thenReturn(new HashSet<>());
        when(preferencesService.enabledPlatforms()).thenReturn(new HashSet<>());
        when(commonGovService.govCloudDeployment(anySet(), anySet())).thenReturn(govDeployment);
        when(entitlementService.isFedRampExternalDatabaseForceDisabled(any())).thenReturn(fedrampExternalDatabaseForceDisabled);

        DistroXV1Request distroXV1Request = new DistroXV1Request();

        underTest.prepare(distroXV1Request, "1");

        verify(commonGovService, times(1)).govCloudDeployment(anySet(), anySet());
        verify(entitlementService, times(1)).isFedRampExternalDatabaseForceDisabled(any());
        assertTrue(distroXV1Request.getExternalDatabase() != null);
        assertTrue(distroXV1Request.getExternalDatabase().getAvailabilityType().equals(DistroXDatabaseAvailabilityType.HA));
    }

    @Test
    void testDistroxModificationIfGovDeploymentAndEntitlementGrantedAndNoDistroxExternalDbConfiguredThenDontTouchTheConfig() {
        boolean govDeployment = true;
        boolean fedrampExternalDatabaseForceDisabled = true;

        when(preferencesService.enabledGovPlatforms()).thenReturn(new HashSet<>());
        when(preferencesService.enabledPlatforms()).thenReturn(new HashSet<>());
        when(commonGovService.govCloudDeployment(anySet(), anySet())).thenReturn(govDeployment);
        when(entitlementService.isFedRampExternalDatabaseForceDisabled(any())).thenReturn(fedrampExternalDatabaseForceDisabled);

        DistroXV1Request distroXV1Request = new DistroXV1Request();
        DistroXDatabaseRequest distroXDatabaseRequest = new DistroXDatabaseRequest();
        distroXDatabaseRequest.setAvailabilityType(DistroXDatabaseAvailabilityType.NON_HA);
        distroXV1Request.setExternalDatabase(distroXDatabaseRequest);

        underTest.prepare(distroXV1Request, "1");

        verify(commonGovService, times(1)).govCloudDeployment(anySet(), anySet());
        verify(entitlementService, times(1)).isFedRampExternalDatabaseForceDisabled(any());
        assertTrue(distroXV1Request.getExternalDatabase() == distroXDatabaseRequest);
        assertTrue(distroXV1Request.getExternalDatabase().getAvailabilityType().equals(DistroXDatabaseAvailabilityType.NON_HA));
    }

    @Test
    void testDistroxModificationIfGovDeploymentAndEntitlementGrantedAndDbNotConfiguredThenDontTouchTheConfig() {
        boolean govDeployment = true;
        boolean fedrampExternalDatabaseForceDisabled = true;

        when(preferencesService.enabledGovPlatforms()).thenReturn(new HashSet<>());
        when(preferencesService.enabledPlatforms()).thenReturn(new HashSet<>());
        when(commonGovService.govCloudDeployment(anySet(), anySet())).thenReturn(govDeployment);
        when(entitlementService.isFedRampExternalDatabaseForceDisabled(any())).thenReturn(fedrampExternalDatabaseForceDisabled);

        DistroXV1Request distroXV1Request = new DistroXV1Request();

        underTest.prepare(distroXV1Request, "1");

        verify(commonGovService, times(1)).govCloudDeployment(anySet(), anySet());
        verify(entitlementService, times(1)).isFedRampExternalDatabaseForceDisabled(any());
        assertTrue(distroXV1Request.getExternalDatabase() == null);
    }

    @Test
    void testDistroxModificationIfNotGovDeploymentThenDontTouchTheConfig() {
        boolean govDeployment = false;

        when(preferencesService.enabledGovPlatforms()).thenReturn(new HashSet<>());
        when(preferencesService.enabledPlatforms()).thenReturn(new HashSet<>());
        when(commonGovService.govCloudDeployment(anySet(), anySet())).thenReturn(govDeployment);

        DistroXV1Request distroXV1Request = new DistroXV1Request();

        underTest.prepare(distroXV1Request, "1");

        verify(commonGovService, times(1)).govCloudDeployment(anySet(), anySet());
        verify(entitlementService, times(0)).isFedRampExternalDatabaseForceDisabled(any());
        assertTrue(distroXV1Request.getExternalDatabase() == null);
    }
}