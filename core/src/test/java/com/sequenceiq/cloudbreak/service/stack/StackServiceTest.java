package com.sequenceiq.cloudbreak.service.stack;

import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.MockitoAnnotations;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class StackServiceTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(StackServiceTest.class);

    @InjectMocks
    private StackService stackService;

    @Before
    public void before() {
        stackService = new DefaultStackService();
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testLogger() {
        Throwable t = new IllegalStateException("mamamama");
        LOGGER.error("test. Ex: {}", 12, t);
    }

}
