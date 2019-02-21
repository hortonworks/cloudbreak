package com.sequenceiq.cloudbreak.service.messages;

import java.util.Collections;

import javax.inject.Inject;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.sequenceiq.cloudbreak.message.CloudbreakMessagesService;
import com.sequenceiq.cloudbreak.message.MessagesConfig;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = { MessagesConfig.class, TestConfig.class })
public class CloudbreakMessagesHostServiceTypeTest {

    @Inject
    private CloudbreakMessagesService messageService;

    @Test
    public void shouldResolveMessageIfCodeProvided() {
        // GIVEN

        // WHEN
        String message = messageService.getMessage("test.message");
        // THEN

        Assert.assertEquals("Invalid message", "Hi my dear friend", message);

    }

    @Test
    public void shouldResolveCodeAndMergeArgs() {
        // GIVEN


        // WHEN
        String message = messageService.getMessage("stack.infrastructure.time", Collections.singletonList(123));
        // THEN
        Assert.assertEquals("Invalid message resolution!", "Infrastructure creation took 123 seconds", message);


    }
}