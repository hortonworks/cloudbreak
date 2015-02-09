package com.sequenceiq.cloudbreak.core.flow;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SimpleFlowManagerTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(SimpleFlowManagerTest.class);
    private SimpleFlowManager flowManager;

    @Before
    public void setUp() throws Exception {
        flowManager = new SimpleFlowManager();
        flowManager.registerTransition(this.getClass(), SimpleFlowManager.TransitionFactory.createTransition("PHASE_0", "PHASE_1", "ERROR_0"));
    }

    @Test
    public void shouldReturnTheNextSuccessTransition() throws Exception {
        String phase = flowManager.transition(this.getClass(), true);
        Assert.assertEquals("The returned key is wrong!", "PHASE_1", phase);
    }

    @Test
    public void shouldReturnTheNextFailureTransition() throws Exception {
        String phase = flowManager.transition(this.getClass(), false);
        Assert.assertEquals("The returned key is wrong!", "ERROR_0", phase);
    }

}