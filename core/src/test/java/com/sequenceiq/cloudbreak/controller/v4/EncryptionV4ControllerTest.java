package com.sequenceiq.cloudbreak.controller.v4;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.converter.v4.stacks.StackEncryptionToStackEncryptionResponseConverter;
import com.sequenceiq.cloudbreak.domain.stack.StackEncryption;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;
import com.sequenceiq.cloudbreak.service.stack.StackEncryptionService;
import com.sequenceiq.common.api.encryption.response.StackEncryptionResponse;

@ExtendWith(MockitoExtension.class)
class EncryptionV4ControllerTest {

    @Mock
    private StackDtoService stackDtoService;

    @Mock
    private StackEncryptionService stackEncryptionService;

    @Mock
    private StackEncryptionToStackEncryptionResponseConverter stackEncryptionConverter;

    @InjectMocks
    private EncryptionV4Controller underTest;

    @Test
    void testGetEncryptionKeys() {
        StackDto stack = mock(StackDto.class);
        StackEncryption stackEncryption = mock(StackEncryption.class);
        StackEncryptionResponse response = mock(StackEncryptionResponse.class);
        when(stack.getId()).thenReturn(1L);
        when(stackDtoService.getByCrnWithMdcContext("crn")).thenReturn(stack);
        when(stackEncryptionService.getStackEncryption(1L)).thenReturn(stackEncryption);
        when(stackEncryptionConverter.convert(stackEncryption)).thenReturn(response);

        assertEquals(response, underTest.getEncryptionKeys("crn"));
    }
}
