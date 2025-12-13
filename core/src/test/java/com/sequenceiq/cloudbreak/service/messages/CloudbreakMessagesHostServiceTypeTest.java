package com.sequenceiq.cloudbreak.service.messages;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.Locale;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.MessageSource;

import com.sequenceiq.cloudbreak.message.CloudbreakMessagesService;

@ExtendWith(MockitoExtension.class)
class CloudbreakMessagesHostServiceTypeTest {

    @Mock
    private MessageSource messageSource;

    @InjectMocks
    private CloudbreakMessagesService messagesService;

    @Test
    void shouldResolveMessageIfCodeProvided() {
        when(messageSource.getMessage("test.message", null, Locale.getDefault())).thenReturn("Hi my dear friend");
        assertEquals("Hi my dear friend", messagesService.getMessage("test.message"));
    }

    @Test
    void shouldResolveCodeAndMergeArgs() {
        when(messageSource.getMessage("stack.infrastructure.time", new Object[]{123}, Locale.getDefault()))
                .thenReturn("Infrastructure creation took 123 seconds");
        assertEquals("Infrastructure creation took 123 seconds", messagesService.getMessage("stack.infrastructure.time", Collections.singletonList(123)));
    }
}
