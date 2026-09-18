package com.sequenceiq.datalake.service.sdx;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import jakarta.ws.rs.ForbiddenException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackV4Response;
import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.cloudbreak.common.exception.NotFoundException;
import com.sequenceiq.cloudbreak.common.exception.WebApplicationExceptionMessageExtractor;
import com.sequenceiq.cloudbreak.exception.FlowsAlreadyRunningException;
import com.sequenceiq.datalake.entity.SdxCluster;
import com.sequenceiq.datalake.flow.SdxReactorFlowManager;
import com.sequenceiq.environment.api.v1.encryptionprofile.endpoint.EncryptionProfileEndpoint;
import com.sequenceiq.environment.api.v1.encryptionprofile.model.EncryptionProfileResponse;
import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;
import com.sequenceiq.flow.api.model.FlowIdentifier;
import com.sequenceiq.flow.api.model.FlowType;
import com.sequenceiq.sdx.api.model.SdxClusterRequest;

@ExtendWith(MockitoExtension.class)
class EncryptionProfileServiceTest {

    private static final String DATALAKE_CRN = "crn:cdp:datalake:us-west-1:tenant:datalake:abc";

    @Mock
    private EncryptionProfileEndpoint encryptionProfileEndpoint;

    @Mock
    private EntitlementService entitlementService;

    @Mock
    private SdxVersionRuleEnforcer sdxVersionRuleEnforcer;

    @Mock
    private WebApplicationExceptionMessageExtractor webApplicationExceptionMessageExtractor;

    @Mock
    private SdxService sdxService;

    @Mock
    private SdxReactorFlowManager sdxReactorFlowManager;

    @Mock
    private StackService stackService;

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
        clusterRequest.setEncryptionProfileNameOrCrn("crn:cdp:environments:us-west-1:cloudera:encryptionProfile:custom-ep-123");

        when(entitlementService.isConfigureEncryptionProfileEnabled(any())).thenReturn(true);
        when(sdxVersionRuleEnforcer.isCustomEncryptionProfileSupported(runtimeVersion)).thenReturn(false);

        BadRequestException exception =
                assertThrows(BadRequestException.class, () -> underTest.validateEncryptionProfile(clusterRequest, environment, runtimeVersion));

        assertEquals("Encryption Profile is not supported in 7.3.1 runtime. Please use 7.3.2 or above", exception.getMessage());
    }

    @Test
    void testValidateEncryptionProfileWhenNameInNameOrCrnAndRuntimeSupportedDoesNotThrow() {
        String runtimeVersion = "7.3.2";
        SdxClusterRequest clusterRequest = new SdxClusterRequest();
        clusterRequest.setEncryptionProfileNameOrCrn("epName");
        DetailedEnvironmentResponse environment = new DetailedEnvironmentResponse();
        environment.setCloudPlatform("AWS");

        when(entitlementService.isConfigureEncryptionProfileEnabled(any())).thenReturn(true);
        when(sdxVersionRuleEnforcer.isCustomEncryptionProfileSupported(runtimeVersion)).thenReturn(true);

        assertDoesNotThrow(() -> underTest.validateEncryptionProfile(clusterRequest, environment, runtimeVersion));
    }

    @Test
    void testValidateEncryptionProfileWhenNameInNameOrCrnAndNotEntitledThrows() {
        String runtimeVersion = "7.3.2";
        SdxClusterRequest clusterRequest = new SdxClusterRequest();
        clusterRequest.setEncryptionProfileNameOrCrn("epName");
        DetailedEnvironmentResponse environment = new DetailedEnvironmentResponse();
        environment.setCloudPlatform("AWS");

        when(entitlementService.isConfigureEncryptionProfileEnabled(any())).thenReturn(false);
        when(sdxVersionRuleEnforcer.isCustomEncryptionProfileSupported(runtimeVersion)).thenReturn(true);

        BadRequestException exception =
                assertThrows(BadRequestException.class, () -> underTest.validateEncryptionProfile(clusterRequest, environment, runtimeVersion));

        assertEquals("Encryption Profile entitlement is not granted to the account", exception.getMessage());
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
        clusterRequest.setEncryptionProfileNameOrCrn("crn:cdp:environments:us-west-1:cloudera:encryptionProfile:custom-123");

        underTest.getEncryptionProfile(clusterRequest);

        verify(encryptionProfileEndpoint, times(1))
                .getByCrn("crn:cdp:environments:us-west-1:cloudera:encryptionProfile:custom-123");
        verify(encryptionProfileEndpoint, never()).getByName(anyString());
    }

    @Test
    void testEnableEncryptionProfileByCrnWhenEntitlementNotGrantedThenThrowsBadRequestException() {
        when(entitlementService.isChangeEncryptionProfileEnabled(any())).thenReturn(false);

        BadRequestException exception = assertThrows(BadRequestException.class,
                () -> underTest.enableEncryptionProfileByCrn(DATALAKE_CRN, "epName"));

        assertEquals("Enable encryption profile is not granted to the account", exception.getMessage());
        verify(sdxReactorFlowManager, never()).triggerEnableEncryptionProfile(any(), any());
    }

    @Test
    void testEnableEncryptionProfileByCrnWhenEncryptionProfileNameIsGivenThenResolvesViaGetByName() {
        String encryptionProfileName = "epName";
        String resolvedCrn = "crn:cdp:environments:us-west-1:cloudera:encryptionProfile:custom-123";
        EncryptionProfileResponse profile = new EncryptionProfileResponse();
        profile.setCrn(resolvedCrn);
        SdxCluster sdxCluster = new SdxCluster();
        sdxCluster.setId(1L);
        when(entitlementService.isChangeEncryptionProfileEnabled(any())).thenReturn(true);
        when(encryptionProfileEndpoint.getByName(encryptionProfileName)).thenReturn(profile);
        when(sdxService.getByCrn(DATALAKE_CRN)).thenReturn(sdxCluster);
        when(sdxReactorFlowManager.triggerEnableEncryptionProfile(sdxCluster, resolvedCrn))
                .thenReturn(new FlowIdentifier(FlowType.FLOW, "flowId"));

        underTest.enableEncryptionProfileByCrn(DATALAKE_CRN, encryptionProfileName);

        verify(encryptionProfileEndpoint, times(1)).getByName(encryptionProfileName);
        verify(encryptionProfileEndpoint, never()).getByCrn(anyString());
        verify(sdxReactorFlowManager, times(1)).triggerEnableEncryptionProfile(sdxCluster, resolvedCrn);
    }

    @Test
    void testEnableEncryptionProfileByCrnWhenEncryptionProfileCrnIsGivenThenResolvesViaGetByCrn() {
        String encryptionProfileCrn = "crn:cdp:environments:us-west-1:cloudera:encryptionProfile:custom-123";
        EncryptionProfileResponse profile = new EncryptionProfileResponse();
        profile.setCrn(encryptionProfileCrn);
        SdxCluster sdxCluster = new SdxCluster();
        sdxCluster.setId(1L);
        when(entitlementService.isChangeEncryptionProfileEnabled(any())).thenReturn(true);
        when(encryptionProfileEndpoint.getByCrn(encryptionProfileCrn)).thenReturn(profile);
        when(sdxService.getByCrn(DATALAKE_CRN)).thenReturn(sdxCluster);
        when(sdxReactorFlowManager.triggerEnableEncryptionProfile(sdxCluster, encryptionProfileCrn))
                .thenReturn(new FlowIdentifier(FlowType.FLOW, "flowId"));

        underTest.enableEncryptionProfileByCrn(DATALAKE_CRN, encryptionProfileCrn);

        verify(encryptionProfileEndpoint, times(1)).getByCrn(encryptionProfileCrn);
        verify(encryptionProfileEndpoint, never()).getByName(anyString());
        verify(sdxReactorFlowManager, times(1)).triggerEnableEncryptionProfile(sdxCluster, encryptionProfileCrn);
    }

    @Test
    void testEnableEncryptionProfileByCrnWhenProfileNotFoundThenThrowsBadRequestException() {
        String encryptionProfileName = "missing";
        when(entitlementService.isChangeEncryptionProfileEnabled(any())).thenReturn(true);
        when(encryptionProfileEndpoint.getByName(encryptionProfileName)).thenReturn(null);

        BadRequestException exception = assertThrows(BadRequestException.class,
                () -> underTest.enableEncryptionProfileByCrn(DATALAKE_CRN, encryptionProfileName));

        assertEquals("Encryption profile 'missing' not found", exception.getMessage());
        verify(sdxReactorFlowManager, never()).triggerEnableEncryptionProfile(any(), any());
    }

    @Test
    void testEnableEncryptionProfileByCrnWhenEncryptionProfileNameOrCrnIsBlankThenTriggersFlowWithNullCrn() {
        SdxCluster sdxCluster = new SdxCluster();
        sdxCluster.setId(1L);
        when(entitlementService.isChangeEncryptionProfileEnabled(any())).thenReturn(true);
        when(sdxService.getByCrn(DATALAKE_CRN)).thenReturn(sdxCluster);
        when(sdxReactorFlowManager.triggerEnableEncryptionProfile(sdxCluster, null))
                .thenReturn(new FlowIdentifier(FlowType.FLOW, "flowId"));

        underTest.enableEncryptionProfileByCrn(DATALAKE_CRN, "");

        verify(encryptionProfileEndpoint, never()).getByName(anyString());
        verify(encryptionProfileEndpoint, never()).getByCrn(anyString());
        verify(sdxReactorFlowManager, times(1)).triggerEnableEncryptionProfile(sdxCluster, null);
    }

    @ParameterizedTest
    @EnumSource(value = Status.class, names = {"AVAILABLE", "UPDATE_FAILED"})
    void testDisableEncryptionProfileByCrn(Status stackStatus) {
        when(entitlementService.isChangeEncryptionProfileEnabled(any())).thenReturn(true);
        SdxCluster sdxCluster = sdxCluster();
        when(sdxService.getByCrn(DATALAKE_CRN)).thenReturn(sdxCluster);
        stubStackStatus(stackStatus);
        when(sdxReactorFlowManager.triggerDisableEncryptionProfile(sdxCluster))
                .thenReturn(new FlowIdentifier(FlowType.FLOW, "1"));

        underTest.disableEncryptionProfile(DATALAKE_CRN);

        verify(sdxReactorFlowManager, times(1)).triggerDisableEncryptionProfile(sdxCluster);
    }

    @ParameterizedTest
    @EnumSource(value = Status.class, names = {"STOPPED", "STOP_IN_PROGRESS", "UPDATE_IN_PROGRESS"})
    void testDisableEncryptionProfileByCrnRejectedWhenStackStatusIsNotAllowed(Status stackStatus) {
        when(entitlementService.isChangeEncryptionProfileEnabled(any())).thenReturn(true);
        when(sdxService.getByCrn(DATALAKE_CRN)).thenReturn(sdxCluster());
        stubStackStatus(stackStatus);

        assertThatThrownBy(() -> underTest.disableEncryptionProfile(DATALAKE_CRN))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Data Lake 'dl-cluster' needs to be in AVAILABLE or UPDATE_FAILED state to disable encryption profile. "
                        + "Current status: " + stackStatus);
        verify(sdxReactorFlowManager, never()).triggerDisableEncryptionProfile(any());
    }

    @Test
    void testDisableEncryptionProfileByCrnRejectedWhenStackIsMissing() {
        when(entitlementService.isChangeEncryptionProfileEnabled(any())).thenReturn(true);
        when(sdxService.getByCrn(DATALAKE_CRN)).thenReturn(sdxCluster());
        when(stackService.getDetail(eq("dl-cluster"), any(), eq("tenant"))).thenReturn(null);

        assertThatThrownBy(() -> underTest.disableEncryptionProfile(DATALAKE_CRN))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Data Lake 'dl-cluster' needs to be in AVAILABLE or UPDATE_FAILED state to disable encryption profile. "
                        + "Current status: null");
        verify(sdxReactorFlowManager, never()).triggerDisableEncryptionProfile(any());
    }

    @Test
    void testDisableEncryptionProfileByCrnKeepsFlowsAlreadyRunningException() {
        when(entitlementService.isChangeEncryptionProfileEnabled(any())).thenReturn(true);
        SdxCluster sdxCluster = sdxCluster();
        when(sdxService.getByCrn(DATALAKE_CRN)).thenReturn(sdxCluster);
        stubStackStatus(Status.AVAILABLE);
        when(sdxReactorFlowManager.triggerDisableEncryptionProfile(sdxCluster)).thenThrow(new FlowsAlreadyRunningException("running"));

        assertThatThrownBy(() -> underTest.disableEncryptionProfile(DATALAKE_CRN))
                .isInstanceOf(FlowsAlreadyRunningException.class);
    }

    @Test
    void testDisableEncryptionProfileByCrnKeepsNotFoundException() {
        when(entitlementService.isChangeEncryptionProfileEnabled(any())).thenReturn(true);
        when(sdxService.getByCrn(DATALAKE_CRN)).thenThrow(new NotFoundException("not found"));

        assertThatThrownBy(() -> underTest.disableEncryptionProfile(DATALAKE_CRN))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void testDisableEncryptionProfileByCrnNotEntitled() {
        when(entitlementService.isChangeEncryptionProfileEnabled(any())).thenReturn(false);

        assertThatThrownBy(() -> underTest.disableEncryptionProfile(DATALAKE_CRN))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("Disable encryption profile is not granted to the account");
    }

    private SdxCluster sdxCluster() {
        SdxCluster sdxCluster = new SdxCluster();
        sdxCluster.setClusterName("dl-cluster");
        sdxCluster.setAccountId("tenant");
        return sdxCluster;
    }

    private void stubStackStatus(Status stackStatus) {
        StackV4Response stackV4Response = new StackV4Response();
        stackV4Response.setStatus(stackStatus);
        when(stackService.getDetail(eq("dl-cluster"), any(), eq("tenant"))).thenReturn(stackV4Response);
    }
}
