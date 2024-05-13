package com.sequenceiq.cloudbreak.reactor.handler.orchestration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.cloudbreak.reactor.api.event.stack.encryption.GenerateEncryptionKeysFailed;
import com.sequenceiq.cloudbreak.reactor.api.event.stack.encryption.GenerateEncryptionKeysRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.stack.encryption.GenerateEncryptionKeysSuccess;
import com.sequenceiq.cloudbreak.service.encryption.EncryptionKeyService;
import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;

@ExtendWith(MockitoExtension.class)
class GenerateEncryptionKeysHandlerTest {

    @Mock
    private EncryptionKeyService encryptionKeyService;

    @InjectMocks
    private GenerateEncryptionKeysHandler underTest;

    @Test
    void testSelector() {
        assertEquals(EventSelectorUtil.selector(GenerateEncryptionKeysRequest.class), underTest.selector());
    }

    @Test
    void testDefaultFailureEvent() {
        Exception e = new Exception("test");
        GenerateEncryptionKeysFailed result = (GenerateEncryptionKeysFailed) underTest
                .defaultFailureEvent(1L, e, new Event<>(new GenerateEncryptionKeysRequest(1L)));
        assertEquals(1L, result.getResourceId());
        assertEquals(e, result.getException());
    }

    @Test
    void testAccept() {
        GenerateEncryptionKeysSuccess result = (GenerateEncryptionKeysSuccess) underTest.doAccept(
                new HandlerEvent<>(new Event<>(new GenerateEncryptionKeysRequest(1L))));
        verify(encryptionKeyService).generateEncryptionKeys(1L);
        assertEquals(1L, result.getResourceId());
    }
}