package com.sequenceiq.cloudbreak.service.messages

import java.util.Arrays

import javax.inject.Inject

import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner

@RunWith(SpringJUnit4ClassRunner::class)
@ContextConfiguration(classes = arrayOf(MessagesConfig::class, TestConfig::class))
class CloudbreakMessagesHostServiceTypeTest {

    @Inject
    private val messageService: CloudbreakMessagesService? = null

    @Test
    @Throws(Exception::class)
    fun shouldResolveMessageIfCodeProvided() {
        // GIVEN

        // WHEN
        val message = messageService!!.getMessage("test.message")
        // THEN

        Assert.assertEquals("Invalid message", "Hi my dear friend", message)

    }

    @Test
    @Throws(Exception::class)
    fun shouldResolveCodeAndMergeArgs() {
        // GIVEN


        // WHEN
        val message = messageService!!.getMessage("stack.infrastructure.time", Arrays.asList(123))
        // THEN
        Assert.assertEquals("Invalid message resolution!", "Infrastructure creation took 123 seconds", message)


    }
}