package com.sequenceiq.freeipa.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.common.api.encryption.response.StackEncryptionResponse;
import com.sequenceiq.freeipa.converter.encryption.StackEncryptionToStackEncryptionResponseConverter;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.entity.StackEncryption;
import com.sequenceiq.freeipa.service.StackEncryptionService;
import com.sequenceiq.freeipa.service.stack.StackService;
import com.sequenceiq.freeipa.util.CrnService;

@ExtendWith(MockitoExtension.class)
class EncryptionV1ControllerTest {

    @Mock
    private CrnService crnService;

    @Mock
    private StackService stackService;

    @Mock
    private StackEncryptionService stackEncryptionService;

    @Mock
    private StackEncryptionToStackEncryptionResponseConverter stackEncryptionConverter;

    @InjectMocks
    private EncryptionV1Controller underTest;

    @Test
    void testGetEncryptionKeys() {
        Stack stack = mock(Stack.class);
        StackEncryption stackEncryption = mock(StackEncryption.class);
        StackEncryptionResponse response = mock(StackEncryptionResponse.class);
        when(stack.getId()).thenReturn(1L);
        when(crnService.getCurrentAccountId()).thenReturn("accountId");
        when(stackService.getFreeIpaStackWithMdcContext("environmentCrn", "accountId")).thenReturn(stack);
        when(stackEncryptionService.getStackEncryption(1L)).thenReturn(stackEncryption);
        when(stackEncryptionConverter.convert(stackEncryption)).thenReturn(response);

        assertEquals(response, underTest.getEncryptionKeys("environmentCrn"));
    }
}
