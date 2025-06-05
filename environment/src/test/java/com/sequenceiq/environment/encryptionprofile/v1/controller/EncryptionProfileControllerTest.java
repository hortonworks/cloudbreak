package com.sequenceiq.environment.encryptionprofile.v1.controller;

import static com.sequenceiq.environment.encryptionprofile.EncryptionProfileTestConstants.USER_CRN;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.event.ResourceEvent;
import com.sequenceiq.environment.api.v1.encryptionprofile.model.EncryptionProfileRequest;
import com.sequenceiq.environment.api.v1.encryptionprofile.model.EncryptionProfileResponse;
import com.sequenceiq.environment.encryptionprofile.domain.EncryptionProfile;
import com.sequenceiq.environment.encryptionprofile.service.EncryptionProfileService;
import com.sequenceiq.environment.encryptionprofile.v1.converter.EncryptionProfileRequestToEncryptionProfileConverter;
import com.sequenceiq.environment.encryptionprofile.v1.converter.EncryptionProfileToEncryptionProfileResponseConverter;
import com.sequenceiq.notification.NotificationService;

public class EncryptionProfileControllerTest {

    private EncryptionProfileService encryptionProfileService;

    private EncryptionProfileRequestToEncryptionProfileConverter requestConverter;

    private EncryptionProfileToEncryptionProfileResponseConverter responseConverter;

    private NotificationService notificationService;

    private EncryptionProfileController controller;

    @BeforeEach
    public void setUp() throws Exception {
        encryptionProfileService = mock(EncryptionProfileService.class);
        requestConverter = mock(EncryptionProfileRequestToEncryptionProfileConverter.class);
        responseConverter = mock(EncryptionProfileToEncryptionProfileResponseConverter.class);
        notificationService = mock(NotificationService.class);

        controller = new EncryptionProfileController(encryptionProfileService, requestConverter, responseConverter);

        // Inject mock NotificationService into the superclass via reflection
        Field field = controller.getClass().getSuperclass().getDeclaredField("notificationService");
        field.setAccessible(true);
        field.set(controller, notificationService);
    }

    @Test
    public void testConvertRequestAndReturnResponse() {
        EncryptionProfileRequest request = new EncryptionProfileRequest();
        EncryptionProfile profile = new EncryptionProfile();
        EncryptionProfileResponse expectedResponse = new EncryptionProfileResponse();

        when(requestConverter.convert(request)).thenReturn(profile);
        when(encryptionProfileService.create(eq(profile), anyString(), anyString())).thenReturn(profile);
        when(responseConverter.convert(profile)).thenReturn(expectedResponse);

        EncryptionProfileResponse actualResponse = ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> {
            return controller.create(request);
        });

        assertThat(actualResponse).isEqualTo(expectedResponse);

        verify(requestConverter).convert(request);
        verify(encryptionProfileService).create(eq(profile), anyString(), anyString());
        verify(responseConverter).convert(profile);
        verify(notificationService).send(eq(ResourceEvent.ENCRYPTION_PROFILE_CREATED), any(), Optional.ofNullable(any()));
    }
}
