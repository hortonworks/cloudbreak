package com.sequenceiq.datalake.service.sdx;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
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
        String runtimeVersion = "7.3.1";
        SdxClusterRequest clusterRequest = new SdxClusterRequest();
        DetailedEnvironmentResponse environment = new DetailedEnvironmentResponse();
        environment.setCloudPlatform("AWS");
        environment.setEncryptionProfileCrn("crn:cdp:environments:us-west-1:cloudera:encryptionProfile:custom-ep-123");

        when(entitlementService.isConfigureEncryptionProfileEnabled(any())).thenReturn(true);
        when(sdxVersionRuleEnforcer.isCustomEncryptionProfileSupported(runtimeVersion)).thenReturn(false);

        BadRequestException exception =
                assertThrows(BadRequestException.class, () -> underTest.validateEncryptionProfile(clusterRequest, environment, runtimeVersion));

        assertEquals("Encryption Profile is not supported in 7.3.1 runtime. Please use 7.3.2 or above", exception.getMessage());
    }

    @Test
    void testValidateEncryptionProfileWhenCustomEncryptionProfileInDatalakeRequestIsNotSupportedByRuntime() {
        String runtimeVersion = "7.3.1";
        SdxClusterRequest clusterRequest = new SdxClusterRequest();
        DetailedEnvironmentResponse environment = new DetailedEnvironmentResponse();
        environment.setCloudPlatform("AWS");
        clusterRequest.setEncryptionProfileCrn("crn:cdp:environments:us-west-1:cloudera:encryptionProfile:custom-ep-123");

        when(entitlementService.isConfigureEncryptionProfileEnabled(any())).thenReturn(true);
        when(sdxVersionRuleEnforcer.isCustomEncryptionProfileSupported(runtimeVersion)).thenReturn(false);

        BadRequestException exception =
                assertThrows(BadRequestException.class, () -> underTest.validateEncryptionProfile(clusterRequest, environment, runtimeVersion));

        assertEquals("Encryption Profile is not supported in 7.3.1 runtime. Please use 7.3.2 or above", exception.getMessage());
    }

    @Test
    void testValidateEncryptionProfileWhenEntitlementIsNotGranted() {
        String runtimeVersion = "7.3.2";
        SdxClusterRequest clusterRequest = new SdxClusterRequest();
        DetailedEnvironmentResponse environment = new DetailedEnvironmentResponse();
        environment.setCloudPlatform("AWS");
        environment.setEncryptionProfileCrn("crn:cdp:environments:us-west-1:cloudera:encryptionProfile:custom-ep-123");

        when(entitlementService.isConfigureEncryptionProfileEnabled(any())).thenReturn(false);
        when(sdxVersionRuleEnforcer.isCustomEncryptionProfileSupported(runtimeVersion)).thenReturn(true);

        BadRequestException exception =
                assertThrows(BadRequestException.class, () -> underTest.validateEncryptionProfile(clusterRequest, environment, runtimeVersion));

        assertEquals("Encryption Profile entitlement is not granted to the account", exception.getMessage());
    }

    @Test
    void testValidateEncryptionProfileWhenCustomEncryptionProfileIsSupportedByRuntime() {
        String runtimeVersion = "7.3.2";
        SdxClusterRequest clusterRequest = new SdxClusterRequest();
        DetailedEnvironmentResponse environment = new DetailedEnvironmentResponse();
        environment.setCloudPlatform("AWS");
        environment.setEncryptionProfileCrn("crn:cdp:environments:us-west-1:cloudera:encryptionProfile:custom-ep-123");

        when(entitlementService.isConfigureEncryptionProfileEnabled(any())).thenReturn(true);
        when(sdxVersionRuleEnforcer.isCustomEncryptionProfileSupported(runtimeVersion)).thenReturn(true);

        assertDoesNotThrow(() -> underTest.validateEncryptionProfile(clusterRequest, environment, runtimeVersion));
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
        doCallRealMethod().when(sdxVersionRuleEnforcer).isCustomEncryptionProfileSupported(runtime);

        assertThatThrownBy(() -> underTest.validateEncryptionProfile(clusterRequest, environment, runtime))
                .hasMessage("Encryption Profile is not supported in " + runtime + " runtime. Please use 7.3.2 or above");
    }

    @Test
    void testGetEncryptionProfileWhenInputIsNullThenResponseShouldBeNullAndDoesNotThrowException() {
        SdxClusterRequest clusterRequest = new SdxClusterRequest();

        EncryptionProfileResponse response = assertDoesNotThrow(() -> underTest.getEncryptionProfile(clusterRequest));

        assertThat(response).isNull();
    }

    @Test
    void testGetEncryptionProfileWhenProfileNameIsUsed() {
        SdxClusterRequest clusterRequest = new SdxClusterRequest();
        clusterRequest.setEncryptionProfileNameOrCrn("epName");

        underTest.getEncryptionProfile(clusterRequest);

        verify(encryptionProfileEndpoint, times(1)).getByName("epName");
        verify(encryptionProfileEndpoint, never()).getByCrn(anyString());
    }

    @Test
    void testGetEncryptionProfileWhenProfileCrnIsUsed() {
        SdxClusterRequest clusterRequest = new SdxClusterRequest();
        clusterRequest.setEncryptionProfileCrn("crn:cdp:environments:us-west-1:cloudera:encryptionProfile:custom-123");

        underTest.getEncryptionProfile(clusterRequest);

        verify(encryptionProfileEndpoint, times(1))
                .getByCrn("crn:cdp:environments:us-west-1:cloudera:encryptionProfile:custom-123");
        verify(encryptionProfileEndpoint, never()).getByName(anyString());
    }

}
