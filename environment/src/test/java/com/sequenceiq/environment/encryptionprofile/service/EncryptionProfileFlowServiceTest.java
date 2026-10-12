package com.sequenceiq.environment.encryptionprofile.service;

import static com.sequenceiq.environment.encryptionprofile.EncryptionProfileTestConstants.USER_CRN;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.api.endpoint.v4.dto.NameOrCrn;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.environment.encryptionprofile.domain.EncryptionProfile;
import com.sequenceiq.environment.environment.EnvironmentStatus;
import com.sequenceiq.environment.environment.dto.EnvironmentDto;
import com.sequenceiq.environment.environment.flow.EnvironmentReactorFlowManager;
import com.sequenceiq.environment.environment.flow.encryptionprofile.validator.EncryptionProfileValidator;
import com.sequenceiq.environment.environment.service.EnvironmentService;
import com.sequenceiq.environment.environment.service.sdx.SdxService;
import com.sequenceiq.environment.environment.service.stack.StackService;
import com.sequenceiq.sdx.api.model.SdxClusterResponse;

@ExtendWith(MockitoExtension.class)
public class EncryptionProfileFlowServiceTest {

    private static final String ENCRYPTION_PROFILE_NAME = "encryptionProfileName";

    private static final String ENCRYPTION_PROFILE_CRN = "crn:cdp:environments:us-west-1:cloudera:encryptionProfile:custom-123";

    @Mock
    private EnvironmentService environmentService;

    @Mock
    private EncryptionProfileService encryptionProfileService;

    @Mock
    private EnvironmentReactorFlowManager reactorFlowManager;

    @Mock
    private SdxService sdxService;

    @Mock
    private StackService stackService;

    @Mock
    private EntitlementService entitlementService;

    @Mock
    private EncryptionProfileValidator encryptionProfileValidator;

    @InjectMocks
    private EncryptionProfileFlowService underTest;

    @Test
    void testeEnableEncryptionProfileByCrn() {
        EnvironmentDto environmentDto = new EnvironmentDto();
        environmentDto.setId(1L);
        environmentDto.setName("envName");
        environmentDto.setResourceCrn("crn:cdp:environments:us-west-1:tenant:environment:envCrn");
        environmentDto.setStatus(EnvironmentStatus.AVAILABLE);
        EncryptionProfile encryptionProfile = new EncryptionProfile();
        encryptionProfile.setName(ENCRYPTION_PROFILE_NAME);
        encryptionProfile.setResourceCrn(ENCRYPTION_PROFILE_CRN);

        when(entitlementService.isChangeEncryptionProfileEnabled(any())).thenReturn(true);
        when(environmentService.getByCrnAndAccountId(eq("crn:cdp:environments:us-west-1:tenant:environment:envCrn"), any())).thenReturn(environmentDto);
        when(encryptionProfileService.getByCrn(eq(ENCRYPTION_PROFILE_CRN), any())).thenReturn(encryptionProfile);

        ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.enableEncryptionProfileByCrn(
                NameOrCrn.ofCrn("crn:cdp:environments:us-west-1:tenant:environment:envCrn"), ENCRYPTION_PROFILE_CRN));

        verify(encryptionProfileValidator).validate(environmentDto.getResourceCrn());
        verify(reactorFlowManager).triggerEnableEncryptionProfile(eq(environmentDto.getId()), eq(environmentDto.getName()), eq(environmentDto.getResourceCrn()),
                any());
    }

    @Test
    void testeEnableEncryptionProfileByName() {
        EnvironmentDto environmentDto = new EnvironmentDto();
        environmentDto.setId(1L);
        environmentDto.setName("envName");
        environmentDto.setResourceCrn("crn:cdp:environments:us-west-1:tenant:environment:envCrn");
        environmentDto.setStatus(EnvironmentStatus.AVAILABLE);
        EncryptionProfile encryptionProfile = new EncryptionProfile();
        encryptionProfile.setName(ENCRYPTION_PROFILE_NAME);
        encryptionProfile.setResourceCrn(ENCRYPTION_PROFILE_CRN);

        when(entitlementService.isChangeEncryptionProfileEnabled(any())).thenReturn(true);
        when(environmentService.getByNameAndAccountId(eq("envName"), any())).thenReturn(environmentDto);
        when(encryptionProfileService.getByNameAndAccountId(eq(ENCRYPTION_PROFILE_NAME), any())).thenReturn(encryptionProfile);

        ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.enableEncryptionProfileByName(
                NameOrCrn.ofName("envName"), ENCRYPTION_PROFILE_NAME));

        verify(encryptionProfileValidator).validate(environmentDto.getResourceCrn());
        verify(reactorFlowManager).triggerEnableEncryptionProfile(eq(environmentDto.getId()), eq(environmentDto.getName()), eq(environmentDto.getResourceCrn()),
                any());
    }

    @Test
    void testEnableEncryptionProfileWhenEnvironmentIsNotAvailable() {
        EnvironmentDto environmentDto = new EnvironmentDto();
        environmentDto.setId(1L);
        environmentDto.setName("envName");
        environmentDto.setResourceCrn("crn:cdp:environments:us-west-1:tenant:environment:envCrn");
        environmentDto.setStatus(EnvironmentStatus.ENABLE_ENCRYPTION_PROFILE_IN_PROGRESS);
        EncryptionProfile encryptionProfile = new EncryptionProfile();
        encryptionProfile.setResourceCrn(ENCRYPTION_PROFILE_CRN);

        when(entitlementService.isChangeEncryptionProfileEnabled(any())).thenReturn(true);
        when(environmentService.getByNameAndAccountId(eq("envName"), any())).thenReturn(environmentDto);
        when(encryptionProfileService.getByNameAndAccountId(eq(ENCRYPTION_PROFILE_NAME), any())).thenReturn(encryptionProfile);

        BadRequestException ex = assertThrows(BadRequestException.class, () -> ThreadBasedUserCrnProvider.doAs(USER_CRN, () ->
                underTest.enableEncryptionProfileByName(NameOrCrn.ofName("envName"), ENCRYPTION_PROFILE_NAME)));

        assertEquals("Environment 'envName' needs to be in AVAILABLE state to enable encryption profile. "
                + "Current status: ENABLE_ENCRYPTION_PROFILE_IN_PROGRESS", ex.getMessage());
        verify(encryptionProfileValidator, never()).validate(any());
        verify(reactorFlowManager, never()).triggerEnableEncryptionProfile(anyLong(), any(), any(), any());
    }

    @Test
    void testEnableEncryptionProfileWhenValidationFails() {
        EnvironmentDto environmentDto = new EnvironmentDto();
        environmentDto.setId(1L);
        environmentDto.setName("envName");
        environmentDto.setResourceCrn("crn:cdp:environments:us-west-1:tenant:environment:envCrn");
        environmentDto.setStatus(EnvironmentStatus.AVAILABLE);
        EncryptionProfile encryptionProfile = new EncryptionProfile();
        encryptionProfile.setResourceCrn(ENCRYPTION_PROFILE_CRN);

        when(entitlementService.isChangeEncryptionProfileEnabled(any())).thenReturn(true);
        when(environmentService.getByNameAndAccountId(eq("envName"), any())).thenReturn(environmentDto);
        when(encryptionProfileService.getByNameAndAccountId(eq(ENCRYPTION_PROFILE_NAME), any())).thenReturn(encryptionProfile);
        doThrow(new BadRequestException("dh1 not available")).when(encryptionProfileValidator).validate(environmentDto.getResourceCrn());

        BadRequestException ex = assertThrows(BadRequestException.class, () -> ThreadBasedUserCrnProvider.doAs(USER_CRN, () ->
                underTest.enableEncryptionProfileByName(NameOrCrn.ofName("envName"), ENCRYPTION_PROFILE_NAME)));

        assertEquals("dh1 not available", ex.getMessage());
        verify(reactorFlowManager, never()).triggerEnableEncryptionProfile(anyLong(), any(), any(), any());
    }

    @Test
    void testeDisableEncryptionProfileByCrn() {
        EnvironmentDto environmentDto = new EnvironmentDto();
        environmentDto.setId(1L);
        environmentDto.setName("envName");
        environmentDto.setResourceCrn("crn:cdp:environments:us-west-1:tenant:environment:envCrn");
        SdxClusterResponse sdxClusterResponse = new SdxClusterResponse();
        sdxClusterResponse.setCrn("crn:cdp:datalake:us-west-1:Test:datalake:40231209-6037-4d4b-95b9-f3c30698ae98");

        when(entitlementService.isChangeEncryptionProfileEnabled(any())).thenReturn(true);
        when(environmentService.getByCrnAndAccountId(eq("crn:cdp:environments:us-west-1:tenant:environment:envCrn"), any())).thenReturn(environmentDto);
        when(sdxService.list(eq(environmentDto.getName()))).thenReturn(List.of(sdxClusterResponse));

        ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.disableEncryptionProfile(
                NameOrCrn.ofCrn("crn:cdp:environments:us-west-1:tenant:environment:envCrn")));

        verify(stackService).updatePillarConfigurationByCrn(eq(sdxClusterResponse.getCrn()));
    }

    @Test
    void testeDisableEncryptionProfileByName() {
        EnvironmentDto environmentDto = new EnvironmentDto();
        environmentDto.setId(1L);
        environmentDto.setName("envName");
        environmentDto.setResourceCrn("crn:cdp:environments:us-west-1:tenant:environment:envCrn");
        SdxClusterResponse sdxClusterResponse = new SdxClusterResponse();
        sdxClusterResponse.setCrn("crn:cdp:datalake:us-west-1:Test:datalake:40231209-6037-4d4b-95b9-f3c30698ae98");

        when(entitlementService.isChangeEncryptionProfileEnabled(any())).thenReturn(true);
        when(environmentService.getByNameAndAccountId(eq("envName"), any())).thenReturn(environmentDto);
        when(sdxService.list(eq(environmentDto.getName()))).thenReturn(List.of(sdxClusterResponse));

        ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.disableEncryptionProfile(NameOrCrn.ofName("envName")));

        verify(stackService).updatePillarConfigurationByCrn(eq(sdxClusterResponse.getCrn()));
    }
}
