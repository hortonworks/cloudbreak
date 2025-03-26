package com.sequenceiq.cloudbreak.rotation.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.List;

import org.apache.commons.lang3.reflect.FieldUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.rotation.common.TestSecretType;
import com.sequenceiq.cloudbreak.rotation.entity.SecretRotationHistory;
import com.sequenceiq.cloudbreak.rotation.response.BaseSecretTypeResponse;
import com.sequenceiq.cloudbreak.rotation.service.history.SecretRotationHistoryService;
import com.sequenceiq.cloudbreak.rotation.service.notification.SecretRotationNotificationService;

@ExtendWith(MockitoExtension.class)
public class SecretTypeListServiceTest {

    @Mock
    private SecretRotationNotificationService notificationService;

    @Mock
    private SecretRotationHistoryService historyService;

    @InjectMocks
    private SecretTypeListService<BaseSecretTypeResponse> underTest;

    @BeforeEach
    void setup() throws IllegalAccessException {
        FieldUtils.writeField(underTest, "enabledSecretTypes", List.of(TestSecretType.TEST), true);
    }

    @Test
    void testGetTypes() {
        when(historyService.getHistoryForResource(any())).thenReturn(List.of(new SecretRotationHistory("crn", TestSecretType.TEST, 1L)));
        when(notificationService.getMessage(any(), any())).thenReturn("message");

        List<BaseSecretTypeResponse> result =
                underTest.listRotatableSecretType("crn", secretTypeResponse -> secretTypeResponse);

        assertEquals(1, result.size());
    }
}
