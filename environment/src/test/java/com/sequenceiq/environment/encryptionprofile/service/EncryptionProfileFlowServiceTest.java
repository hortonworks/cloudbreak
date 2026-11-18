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

import jakarta.ws.rs.ForbiddenException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.api.endpoint.v4.dto.NameOrCrn;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.environment.encryptionprofile.domain.EncryptionProfile;
import com.sequenceiq.environment.environment.EnvironmentStatus;
import com.sequenceiq.environment.environment.dto.EnvironmentDto;
import com.sequenceiq.environment.environment.flow.EnvironmentReactorFlowManager;
import com.sequenceiq.environment.environment.flow.encryptionprofile.validator.EncryptionProfileValidator;
import com.sequenceiq.environment.environment.service.EnvironmentService;

@ExtendWith(MockitoExtension.class)
public class EncryptionProfileFlowServiceTest {

    private static final String ENCRYPTION_PROFILE_NAME = "encryptionProfileName";

    private static final String ENCRYPTION_PROFILE_CRN = "crn:cdp:environments:us-west-1:cloudera:encryptionProfile:custom-123";

    private static final String ENV_CRN = "crn:cdp:environments:us-west-1:tenant:environment:envCrn";

    @Mock
    private EnvironmentService environmentService;

    @Mock
    private EncryptionProfileService encryptionProfileService;

    @Mock
    private EnvironmentReactorFlowManager reactorFlowManager;

    @Mock
    private EncryptionProfileValidator encryptionProfileValidator;

    @InjectMocks
    private EncryptionProfileFlowService underTest;

    @Test
    void testeEnableEncryptionProfileByCrn() {
        EnvironmentDto environmentDto = envDto();
        EncryptionProfile encryptionProfile = new EncryptionProfile();
        encryptionProfile.setName(ENCRYPTION_PROFILE_NAME);
        encryptionProfile.setResourceCrn(ENCRYPTION_PROFILE_CRN);

        when(environmentService.getByCrnAndAccountId(eq(ENV_CRN), any())).thenReturn(environmentDto);
        when(encryptionProfileService.getByCrn(eq(ENCRYPTION_PROFILE_CRN), any())).thenReturn(encryptionProfile);

        ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.enableEncryptionProfileByCrn(
                NameOrCrn.ofCrn(ENV_CRN), ENCRYPTION_PROFILE_CRN));

        verify(encryptionProfileValidator).validate(environmentDto.getResourceCrn());
        verify(reactorFlowManager).triggerEnableEncryptionProfile(eq(environmentDto.getId()), eq(environmentDto.getName()), eq(environmentDto.getResourceCrn()),
                any());
    }

    @Test
    void testeEnableEncryptionProfileByName() {
        EnvironmentDto environmentDto = envDto();
        EncryptionProfile encryptionProfile = new EncryptionProfile();
        encryptionProfile.setName(ENCRYPTION_PROFILE_NAME);
        encryptionProfile.setResourceCrn(ENCRYPTION_PROFILE_CRN);

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

        when(environmentService.getByNameAndAccountId(eq("envName"), any())).thenReturn(environmentDto);
        when(encryptionProfileService.getByNameAndAccountId(eq(ENCRYPTION_PROFILE_NAME), any())).thenReturn(encryptionProfile);

        BadRequestException ex = assertThrows(BadRequestException.class, () -> ThreadBasedUserCrnProvider.doAs(USER_CRN, () ->
                underTest.enableEncryptionProfileByName(NameOrCrn.ofName("envName"), ENCRYPTION_PROFILE_NAME)));

        assertEquals("Environment 'envName' needs to be in one of the following states to enable encryption profile: "
                + "AVAILABLE, DISABLE_ENCRYPTION_PROFILE_FAILED, ENABLE_ENCRYPTION_PROFILE_FAILED. "
                + "Current status: ENABLE_ENCRYPTION_PROFILE_IN_PROGRESS", ex.getMessage());
        verify(encryptionProfileValidator, never()).validate(any());
        verify(reactorFlowManager, never()).triggerEnableEncryptionProfile(anyLong(), any(), any(), any());
    }

    @ParameterizedTest
    @EnumSource(value = EnvironmentStatus.class, names = {"AVAILABLE", "ENABLE_ENCRYPTION_PROFILE_FAILED", "DISABLE_ENCRYPTION_PROFILE_FAILED"})
    void testeEnableEncryptionProfileInAllowedStatus(EnvironmentStatus status) {
        EnvironmentDto environmentDto = envDto();
        environmentDto.setStatus(status);
        EncryptionProfile encryptionProfile = new EncryptionProfile();
        encryptionProfile.setName(ENCRYPTION_PROFILE_NAME);
        encryptionProfile.setResourceCrn(ENCRYPTION_PROFILE_CRN);

        when(environmentService.getByNameAndAccountId(eq("envName"), any())).thenReturn(environmentDto);
        when(encryptionProfileService.getByNameAndAccountId(eq(ENCRYPTION_PROFILE_NAME), any())).thenReturn(encryptionProfile);

        ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.enableEncryptionProfileByName(
                NameOrCrn.ofName("envName"), ENCRYPTION_PROFILE_NAME));

        verify(encryptionProfileValidator).validate(environmentDto.getResourceCrn());
        verify(reactorFlowManager).triggerEnableEncryptionProfile(eq(environmentDto.getId()), eq(environmentDto.getName()), eq(environmentDto.getResourceCrn()),
                any());
    }

    @ParameterizedTest
    @EnumSource(value = EnvironmentStatus.class, names = {"UPDATE_FAILED", "ENABLE_ENCRYPTION_PROFILE_IN_PROGRESS", "DISABLE_ENCRYPTION_PROFILE_IN_PROGRESS",
            "ENV_STOPPED"})
    void testEnableEncryptionProfileWhenEnvironmentStatusIsNotAllowed(EnvironmentStatus status) {
        EnvironmentDto environmentDto = envDto();
        environmentDto.setStatus(status);
        EncryptionProfile encryptionProfile = new EncryptionProfile();
        encryptionProfile.setResourceCrn(ENCRYPTION_PROFILE_CRN);

        when(environmentService.getByNameAndAccountId(eq("envName"), any())).thenReturn(environmentDto);
        when(encryptionProfileService.getByNameAndAccountId(eq(ENCRYPTION_PROFILE_NAME), any())).thenReturn(encryptionProfile);

        BadRequestException ex = assertThrows(BadRequestException.class, () -> ThreadBasedUserCrnProvider.doAs(USER_CRN, () ->
                underTest.enableEncryptionProfileByName(NameOrCrn.ofName("envName"), ENCRYPTION_PROFILE_NAME)));

        assertEquals("Environment 'envName' needs to be in one of the following states to enable encryption profile: "
                + "AVAILABLE, DISABLE_ENCRYPTION_PROFILE_FAILED, ENABLE_ENCRYPTION_PROFILE_FAILED. Current status: " + status, ex.getMessage());
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
        EnvironmentDto environmentDto = envDto();

        when(environmentService.getByCrnAndAccountId(eq(ENV_CRN), any())).thenReturn(environmentDto);

        ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.disableEncryptionProfile(NameOrCrn.ofCrn(ENV_CRN)));

        verify(encryptionProfileValidator).validateDisable();
        verify(reactorFlowManager).triggerDisableEncryptionProfile(
                eq(environmentDto.getId()), eq(environmentDto.getName()), eq(environmentDto.getResourceCrn()));
    }

    @ParameterizedTest
    @EnumSource(value = EnvironmentStatus.class, names = {"AVAILABLE", "UPDATE_FAILED", "ENABLE_ENCRYPTION_PROFILE_FAILED", "DISABLE_ENCRYPTION_PROFILE_FAILED"})
    void testeDisableEncryptionProfileByNameInAllowedStatus(EnvironmentStatus status) {
        EnvironmentDto environmentDto = envDto();
        environmentDto.setStatus(status);

        when(environmentService.getByNameAndAccountId(eq("envName"), any())).thenReturn(environmentDto);

        ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.disableEncryptionProfile(NameOrCrn.ofName("envName")));

        verify(reactorFlowManager).triggerDisableEncryptionProfile(
                eq(environmentDto.getId()), eq(environmentDto.getName()), eq(environmentDto.getResourceCrn()));
    }

    @ParameterizedTest
    @EnumSource(value = EnvironmentStatus.class, names = {"ENV_STOPPED", "STOP_DATAHUB_STARTED", "DISABLE_ENCRYPTION_PROFILE_IN_PROGRESS",
            "ENABLE_ENCRYPTION_PROFILE_IN_PROGRESS"})
    void testDisableEncryptionProfileWhenEnvironmentStatusIsNotAllowed(EnvironmentStatus status) {
        EnvironmentDto environmentDto = envDto();
        environmentDto.setStatus(status);

        when(environmentService.getByNameAndAccountId(eq("envName"), any())).thenReturn(environmentDto);

        BadRequestException ex = assertThrows(BadRequestException.class, () -> ThreadBasedUserCrnProvider.doAs(USER_CRN, () ->
                underTest.disableEncryptionProfile(NameOrCrn.ofName("envName"))));

        assertEquals("Environment 'envName' needs to be in one of the following states to disable encryption profile: "
                + "AVAILABLE, DISABLE_ENCRYPTION_PROFILE_FAILED, ENABLE_ENCRYPTION_PROFILE_FAILED, UPDATE_FAILED. Current status: " + status, ex.getMessage());
        verify(reactorFlowManager, never()).triggerDisableEncryptionProfile(anyLong(), any(), any());
    }

    @Test
    void testDisableEncryptionProfileWhenNotEntitled() {
        doThrow(new ForbiddenException("not entitled")).when(encryptionProfileValidator).validateDisable();

        assertThrows(ForbiddenException.class, () -> ThreadBasedUserCrnProvider.doAs(USER_CRN, () ->
                underTest.disableEncryptionProfile(NameOrCrn.ofName("envName"))));

        verify(environmentService, never()).getByNameAndAccountId(any(), any());
        verify(reactorFlowManager, never()).triggerDisableEncryptionProfile(anyLong(), any(), any());
    }

    private EnvironmentDto envDto() {
        EnvironmentDto environmentDto = new EnvironmentDto();
        environmentDto.setId(1L);
        environmentDto.setName("envName");
        environmentDto.setResourceCrn(ENV_CRN);
        environmentDto.setStatus(EnvironmentStatus.AVAILABLE);
        return environmentDto;
    }
}
