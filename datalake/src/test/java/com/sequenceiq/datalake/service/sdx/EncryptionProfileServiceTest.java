package com.sequenceiq.datalake.service.sdx;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.cloudbreak.common.exception.WebApplicationExceptionMessageExtractor;
import com.sequenceiq.environment.api.v1.encryptionprofile.endpoint.EncryptionProfileEndpoint;
import com.sequenceiq.environment.api.v1.encryptionprofile.model.EncryptionProfileResponse;
import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;
import com.sequenceiq.sdx.api.model.SdxClusterRequest;

@ExtendWith(MockitoExtension.class)
class EncryptionProfileServiceTest {

    @Mock
    private EncryptionProfileEndpoint encryptionProfileEndpoint;

    @Mock
    private EntitlementService entitlementService;

    @Mock
    private SdxVersionRuleEnforcer sdxVersionRuleEnforcer;

    @Mock
    private WebApplicationExceptionMessageExtractor webApplicationExceptionMessageExtractor;

    @InjectMocks
    private EncryptionProfileService underTest;

    @Test
    void testValidateEncryptionProfileWhenCustomEncryptionProfileInEnvIsNotSupportedByRuntime() {
        SdxClusterRequest clusterRequest = new SdxClusterRequest();
        clusterRequest.setRuntime("7.3.1");
        DetailedEnvironmentResponse environment = new DetailedEnvironmentResponse();
        environment.setCloudPlatform("AWS");
        environment.setEncryptionProfileCrn("crn:cdp:environments:us-west-1:cloudera:encryptionProfile:custom-ep-123");

        when(entitlementService.isConfigureEncryptionProfileEnabled(any())).thenReturn(true);
        when(sdxVersionRuleEnforcer.isCustomEncryptionProfileSupported(clusterRequest.getRuntime())).thenReturn(false);
        when(encryptionProfileEndpoint.getByCrn(any())).thenReturn(mock(EncryptionProfileResponse.class));

        BadRequestException exception =
                assertThrows(BadRequestException.class, () -> underTest.validateEncryptionProfile(clusterRequest, environment));

        assertEquals("Encryption Profile is not supported in 7.3.1 runtime. Please use 7.3.2 or above", exception.getMessage());
    }

    @Test
    void testValidateEncryptionProfileWhenCustomEncryptionProfileInDatalakeRequestIsNotSupportedByRuntime() {
        SdxClusterRequest clusterRequest = new SdxClusterRequest();
        clusterRequest.setRuntime("7.3.1");
        DetailedEnvironmentResponse environment = new DetailedEnvironmentResponse();
        environment.setCloudPlatform("AWS");
        clusterRequest.setEncryptionProfileCrn("crn:cdp:environments:us-west-1:cloudera:encryptionProfile:custom-ep-123");

        when(entitlementService.isConfigureEncryptionProfileEnabled(any())).thenReturn(true);
        when(sdxVersionRuleEnforcer.isCustomEncryptionProfileSupported(clusterRequest.getRuntime())).thenReturn(false);
        when(encryptionProfileEndpoint.getByCrn(any())).thenReturn(mock(EncryptionProfileResponse.class));

        BadRequestException exception =
                assertThrows(BadRequestException.class, () -> underTest.validateEncryptionProfile(clusterRequest, environment));

        assertEquals("Encryption Profile is not supported in 7.3.1 runtime. Please use 7.3.2 or above", exception.getMessage());
    }

    @Test
    void testValidateEncryptionProfileWhenEntitlementIsNotGranted() {
        SdxClusterRequest clusterRequest = new SdxClusterRequest();
        clusterRequest.setRuntime("7.3.2");
        DetailedEnvironmentResponse environment = new DetailedEnvironmentResponse();
        environment.setCloudPlatform("AWS");
        environment.setEncryptionProfileCrn("crn:cdp:environments:us-west-1:cloudera:encryptionProfile:custom-ep-123");

        when(entitlementService.isConfigureEncryptionProfileEnabled(any())).thenReturn(false);
        when(sdxVersionRuleEnforcer.isCustomEncryptionProfileSupported(clusterRequest.getRuntime())).thenReturn(true);
        when(encryptionProfileEndpoint.getByCrn(any())).thenReturn(mock(EncryptionProfileResponse.class));

        BadRequestException exception =
                assertThrows(BadRequestException.class, () -> underTest.validateEncryptionProfile(clusterRequest, environment));

        assertEquals("Encryption Profile entitlement is not granted to the account", exception.getMessage());
    }

    @Test
    void testValidateEncryptionProfileWhenEncryptionProfileIsNotFound() {
        SdxClusterRequest clusterRequest = new SdxClusterRequest();
        clusterRequest.setRuntime("7.3.2");
        DetailedEnvironmentResponse environment = new DetailedEnvironmentResponse();
        environment.setCloudPlatform("AWS");
        environment.setEncryptionProfileCrn("crn:cdp:environments:us-west-1:cloudera:encryptionProfile:custom-ep-123");

        when(entitlementService.isConfigureEncryptionProfileEnabled(any())).thenReturn(true);
        when(sdxVersionRuleEnforcer.isCustomEncryptionProfileSupported(clusterRequest.getRuntime())).thenReturn(true);
        when(encryptionProfileEndpoint.getByCrn(any())).thenReturn(null);

        BadRequestException exception =
                assertThrows(BadRequestException.class, () -> underTest.validateEncryptionProfile(clusterRequest, environment));

        assertEquals("Encryption Profile not found", exception.getMessage());
    }

    @Test
    void testValidateEncryptionProfileWhenCustomEncryptionProfileIsSupportedByRuntime() {
        SdxClusterRequest clusterRequest = new SdxClusterRequest();
        clusterRequest.setRuntime("7.3.2");
        DetailedEnvironmentResponse environment = new DetailedEnvironmentResponse();
        environment.setCloudPlatform("AWS");
        environment.setEncryptionProfileCrn("crn:cdp:environments:us-west-1:cloudera:encryptionProfile:custom-ep-123");

        when(entitlementService.isConfigureEncryptionProfileEnabled(any())).thenReturn(true);
        when(sdxVersionRuleEnforcer.isCustomEncryptionProfileSupported(clusterRequest.getRuntime())).thenReturn(true);
        when(encryptionProfileEndpoint.getByCrn(any())).thenReturn(mock(EncryptionProfileResponse.class));

        assertDoesNotThrow(() -> underTest.validateEncryptionProfile(clusterRequest, environment));
    }

    @ParameterizedTest
    @ValueSource(strings = {"7.3.1", "7.2.18", "7.2.17"})
    void runtime731AndBelowShouldThrowExceptionWhenEncryptionProfileIsUsed(String runtime) {
        SdxClusterRequest clusterRequest = new SdxClusterRequest();
        clusterRequest.setRuntime(runtime);
        DetailedEnvironmentResponse environment = new DetailedEnvironmentResponse();
        environment.setCloudPlatform("AWS");
        environment.setEncryptionProfileCrn("crn:cdp:environments:us-west-1:cloudera:encryptionProfile:custom-ep-123");

        when(entitlementService.isConfigureEncryptionProfileEnabled(any())).thenReturn(true);
        when(encryptionProfileEndpoint.getByCrn(any())).thenReturn(mock(EncryptionProfileResponse.class));
        doCallRealMethod().when(sdxVersionRuleEnforcer).isCustomEncryptionProfileSupported(clusterRequest.getRuntime());

        assertThatThrownBy(() -> underTest.validateEncryptionProfile(clusterRequest, environment))
                .hasMessage("Encryption Profile is not supported in " + runtime + " runtime. Please use 7.3.2 or above");
    }

    @Test
    void testgetEncryptionProfileFromDatalakeOtherwiseFromEnvWithDatalakeEncryptionProfile() {
        String encryptionProfileFromEnv = "crn:cdp:environments:us-west-1:cloudera:encryptionProfile:env-123";
        String encryptionProfileCrnFromCluster = "crn:cdp:environments:us-west-1:cloudera:encryptionProfile:datalake-123";

        underTest.getEncryptionProfileFromDatalakeOtherwiseFromEnv(encryptionProfileFromEnv, encryptionProfileCrnFromCluster);

        ArgumentCaptor<String> encryptionCrnArgumentCaptor = ArgumentCaptor.forClass(String.class);
        verify(encryptionProfileEndpoint, times(1)).getByCrn(encryptionCrnArgumentCaptor.capture());

        assertEquals(encryptionProfileCrnFromCluster, encryptionCrnArgumentCaptor.getValue());
    }

    @Test
    void testgetEncryptionProfileFromDatalakeOtherwiseFromEnvWithEnvEncryptionProfile() {
        String encryptionProfileFromEnv = "crn:cdp:environments:us-west-1:cloudera:encryptionProfile:env-123";
        String encryptionProfileCrnFromCluster = null;

        underTest.getEncryptionProfileFromDatalakeOtherwiseFromEnv(encryptionProfileFromEnv, encryptionProfileCrnFromCluster);

        ArgumentCaptor<String> encryptionCrnArgumentCaptor = ArgumentCaptor.forClass(String.class);
        verify(encryptionProfileEndpoint, times(1)).getByCrn(encryptionCrnArgumentCaptor.capture());

        assertEquals(encryptionProfileFromEnv, encryptionCrnArgumentCaptor.getValue());
    }

}
