package com.sequenceiq.environment.encryptionprofile.v1.controller;

import static com.sequenceiq.environment.encryptionprofile.EncryptionProfileTestConstants.ACCOUNT_ID;
import static com.sequenceiq.environment.encryptionprofile.EncryptionProfileTestConstants.ENCRYPTION_PROFILE_CRN;
import static com.sequenceiq.environment.encryptionprofile.EncryptionProfileTestConstants.NAME;
import static com.sequenceiq.environment.encryptionprofile.EncryptionProfileTestConstants.USER_CRN;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Optional;

import jakarta.ws.rs.ForbiddenException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.authorization.resource.AuthorizationResourceAction;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.event.ResourceEvent;
import com.sequenceiq.environment.api.v1.encryptionprofile.model.EncryptionProfileRequest;
import com.sequenceiq.environment.api.v1.encryptionprofile.model.EncryptionProfileResponse;
import com.sequenceiq.environment.api.v1.encryptionprofile.model.EncryptionProfileResponses;
import com.sequenceiq.environment.authorization.EncryptionProfileFiltering;
import com.sequenceiq.environment.encryptionprofile.domain.EncryptionProfile;
import com.sequenceiq.environment.encryptionprofile.service.EncryptionProfileService;
import com.sequenceiq.environment.encryptionprofile.v1.converter.EncryptionProfileRequestToEncryptionProfileConverter;
import com.sequenceiq.environment.encryptionprofile.v1.converter.EncryptionProfileToEncryptionProfileResponseConverter;
import com.sequenceiq.notification.NotificationService;

@ExtendWith(MockitoExtension.class)
public class EncryptionProfileControllerTest {

    @Mock
    private EncryptionProfileService encryptionProfileService;

    @Mock
    private NotificationService notificationService;

    @Mock
    private EntitlementService entitlementService;

    @Mock
    private EncryptionProfileFiltering encryptionProfileFiltering;

    @Mock
    private EncryptionProfileRequestToEncryptionProfileConverter requestConverter;

    @Mock
    private EncryptionProfileToEncryptionProfileResponseConverter responseConverter;

    @InjectMocks
    private EncryptionProfileController controller;

    @BeforeEach
    public void setUp() throws Exception {
        when(entitlementService.isConfigureEncryptionProfileEnabled(anyString())).thenReturn(true);
        controller = new EncryptionProfileController(encryptionProfileService,
                requestConverter,
                responseConverter,
                encryptionProfileFiltering,
                entitlementService);
        // Inject mock NotificationService into the superclass via reflection
        Field field = controller.getClass().getSuperclass().getDeclaredField("notificationService");
        field.setAccessible(true);
        field.set(controller, notificationService);
    }

    @Test
    public void testCreate() {
        EncryptionProfileRequest request = new EncryptionProfileRequest();
        EncryptionProfile profile = new EncryptionProfile();
        EncryptionProfileResponse expectedResponse = new EncryptionProfileResponse();

        when(requestConverter.convert(request)).thenReturn(profile);
        when(encryptionProfileService.create(eq(profile), anyString(), anyString())).thenReturn(profile);
        when(responseConverter.convert(profile)).thenReturn(expectedResponse);

        EncryptionProfileResponse actualResponse = ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> controller.create(request));

        assertThat(actualResponse).isEqualTo(expectedResponse);

        verify(requestConverter).convert(request);
        verify(encryptionProfileService).create(eq(profile), anyString(), anyString());
        verify(responseConverter).convert(profile);
        verify(notificationService).send(eq(ResourceEvent.ENCRYPTION_PROFILE_CREATED), any(), Optional.ofNullable(any()));
    }

    @Test
    public void testGetByName() {
        EncryptionProfile profile = new EncryptionProfile();
        EncryptionProfileResponse expectedResponse = new EncryptionProfileResponse();

        when(encryptionProfileService.getByNameAndAccountId(eq(NAME), eq(ACCOUNT_ID)))
                .thenReturn(profile);
        when(responseConverter.convert(profile)).thenReturn(expectedResponse);

        EncryptionProfileResponse actualResponse = ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> controller.getByName(NAME));

        assertThat(actualResponse).isEqualTo(expectedResponse);

        verify(encryptionProfileService).getByNameAndAccountId(eq(NAME), eq(ACCOUNT_ID));
        verify(responseConverter).convert(profile);
    }

    @Test
    public void testGetByCrn() {
        EncryptionProfile profile = new EncryptionProfile();
        EncryptionProfileResponse expectedResponse = new EncryptionProfileResponse();

        when(encryptionProfileService.getByCrn(eq(ENCRYPTION_PROFILE_CRN))).thenReturn(profile);
        when(responseConverter.convert(profile)).thenReturn(expectedResponse);

        EncryptionProfileResponse actualResponse = ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> controller.getByCrn(ENCRYPTION_PROFILE_CRN));

        assertThat(actualResponse).isEqualTo(expectedResponse);

        verify(encryptionProfileService).getByCrn(eq(ENCRYPTION_PROFILE_CRN));
        verify(responseConverter).convert(profile);
    }

    @Test
    public void testList() {
        EncryptionProfileResponse response1 = new EncryptionProfileResponse();
        EncryptionProfileResponse response2 = new EncryptionProfileResponse();
        EncryptionProfileResponses expectedResponses = new EncryptionProfileResponses(new HashSet<>(Arrays.asList(response1, response2)));

        when(encryptionProfileFiltering.filterResources(any(), any(), any()))
                .thenReturn(expectedResponses);

        EncryptionProfileResponses actualResponses = ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> controller.list());

        assertThat(actualResponses).isEqualTo(expectedResponses);

        verify(encryptionProfileFiltering).filterResources(
                any(),
                eq(AuthorizationResourceAction.DESCRIBE_ENCRYPTION_PROFILE),
                eq(Collections.emptyMap()));
    }

    @Test
    public void testDeleteByName() {
        EncryptionProfile profile = new EncryptionProfile();
        EncryptionProfileResponse expectedResponse = new EncryptionProfileResponse();

        when(encryptionProfileService.deleteByNameAndAccountId(eq(NAME), eq(ACCOUNT_ID)))
                .thenReturn(profile);
        when(responseConverter.convert(profile, false)).thenReturn(expectedResponse);

        EncryptionProfileResponse actualResponse = ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> controller.deleteByName(NAME));

        assertThat(actualResponse).isEqualTo(expectedResponse);

        verify(encryptionProfileService).deleteByNameAndAccountId(eq(NAME), eq(ACCOUNT_ID));
        verify(responseConverter).convert(profile, false);
        verify(notificationService).send(eq(ResourceEvent.ENCRYPTION_PROFILE_DELETED), any(), Optional.ofNullable(any()));
    }

    @Test
    public void testDeleteByCrn() {
        EncryptionProfile profile = new EncryptionProfile();
        EncryptionProfileResponse expectedResponse = new EncryptionProfileResponse();

        when(encryptionProfileService.deleteByResourceCrn(eq(ENCRYPTION_PROFILE_CRN))).thenReturn(profile);
        when(responseConverter.convert(profile, false)).thenReturn(expectedResponse);

        EncryptionProfileResponse actualResponse = ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> controller.deleteByCrn(ENCRYPTION_PROFILE_CRN));

        assertThat(actualResponse).isEqualTo(expectedResponse);

        verify(encryptionProfileService).deleteByResourceCrn(eq(ENCRYPTION_PROFILE_CRN));
        verify(responseConverter).convert(profile, false);
        verify(notificationService).send(eq(ResourceEvent.ENCRYPTION_PROFILE_DELETED), any(), Optional.ofNullable(any()));
    }

    @Test
    public void testVerifyEncryptionProfileEntitlementForAllEndpoints() {

        when(entitlementService.isConfigureEncryptionProfileEnabled(anyString())).thenReturn(false);

        assertThatThrownBy(() ->
            ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> controller.create(new EncryptionProfileRequest())))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("Encryption profile operations are not enabled for account: cloudbreak");

        assertThatThrownBy(() ->
                ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> controller.getByName("test-name")))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("Encryption profile operations are not enabled for account: cloudbreak");

        assertThatThrownBy(() ->
                ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> controller.getByCrn("test-crn")))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("Encryption profile operations are not enabled for account: cloudbreak");

        assertThatThrownBy(() ->
                ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> controller.list()))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("Encryption profile operations are not enabled for account: cloudbreak");

        assertThatThrownBy(() ->
                ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> controller.deleteByName("test-name")))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("Encryption profile operations are not enabled for account: cloudbreak");

        assertThatThrownBy(() ->
                ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> controller.deleteByCrn("test-crn")))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("Encryption profile operations are not enabled for account: cloudbreak");
    }
}
