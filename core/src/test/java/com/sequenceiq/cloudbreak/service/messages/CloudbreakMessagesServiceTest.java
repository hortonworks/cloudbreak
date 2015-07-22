package com.sequenceiq.cloudbreak.service.messages;

import java.util.Arrays;

import javax.inject.Inject;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = { MessagesConfig.class, TestConfig.class })
public class CloudbreakMessagesServiceTest {

    @Inject
    private CloudbreakMessagesService messageService;

    @Test
    public void shouldResolveMessageIfCodeProvided() throws Exception {
        // GIVEN

        // WHEN
        String message = messageService.getMessage("test.message");
        // THEN

        Assert.assertEquals("Invalid message", "Hi my dear friend", message);

    }

    @Test
    public void shouldResolveCodeAndMergeArgs() throws Exception {
        // GIVEN


        // WHEN
        String message = messageService.getMessage("stack.infrastructure.time", Arrays.asList(123));
        // THEN
        Assert.assertEquals("Invalid message resolution!", "Infrastructure creation took 123 seconds", message);


    }
}