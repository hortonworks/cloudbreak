package com.sequenceiq.cloudbreak.core.flow;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.cloudbreak.core.flow.service.SimpleTransitionKeyService;

public class SimpleTransitionKeyServiceTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(SimpleTransitionKeyService.class);
    private TransitionKeyService transitionKeyService;

    @Before
    public void setUp() {
        transitionKeyService = new SimpleTransitionKeyService();
        transitionKeyService.registerTransition(String.class, TransitionFactory.createTransition("CURRENT", "SUCCESS", "FAILURE"));
        transitionKeyService.registerTransition(Integer.class, TransitionFactory.createTransition("CURRENT_NULL", "", ""));
    }

    @Test
    public void shouldReturnSuccessKeyForRegisteredHandler() {
        String successKey = transitionKeyService.successKey(String.class);
        Assert.assertNotNull(successKey);
        Assert.assertEquals("Wrong transition key returned", "SUCCESS", successKey);
    }

    @Test
    public void shouldReturnFailureKeyForRegisteredHandler() {
        String failureKey = transitionKeyService.failureKey(String.class);
        Assert.assertNotNull(failureKey);
        Assert.assertEquals("Wrong transition key returned", "FAILURE", failureKey);
    }

    @Test(expected = IllegalStateException.class)
    public void shouldThrowExceptionWhenHandlerClassNotRegistered() {
        String successKey = transitionKeyService.failureKey(Long.class);
    }

}
