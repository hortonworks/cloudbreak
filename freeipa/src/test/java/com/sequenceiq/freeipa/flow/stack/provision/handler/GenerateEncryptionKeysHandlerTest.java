package com.sequenceiq.freeipa.flow.stack.provision.handler;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;
import com.sequenceiq.freeipa.flow.stack.provision.event.encryption.GenerateEncryptionKeysFailed;
import com.sequenceiq.freeipa.flow.stack.provision.event.encryption.GenerateEncryptionKeysRequest;
import com.sequenceiq.freeipa.flow.stack.provision.event.encryption.GenerateEncryptionKeysSuccess;
import com.sequenceiq.freeipa.service.encryption.EncryptionKeyService;

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