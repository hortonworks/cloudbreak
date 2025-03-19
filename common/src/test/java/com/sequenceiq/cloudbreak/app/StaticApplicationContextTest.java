package com.sequenceiq.cloudbreak.app;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationContext;
import org.springframework.core.env.Environment;
import org.springframework.test.util.ReflectionTestUtils;

class StaticApplicationContextTest {

    private static final String DEFAULT_VALUE = "default";

    private static final String KEY = "key";

    private static final String VALUE = "value";

    @BeforeEach
    @AfterEach
    void setUp() {
        ReflectionTestUtils.setField(StaticApplicationContext.class, "context", null);
    }

    @Test
    void testGetPropertyWhenContextIsNull() {
        assertEquals(DEFAULT_VALUE, StaticApplicationContext.getProperty(KEY, DEFAULT_VALUE));
    }

    @Test
    void testGetPropertyWhenContextEnvironmentIsNull() {
        ApplicationContext context = mock(ApplicationContext.class);
        ReflectionTestUtils.setField(StaticApplicationContext.class, "context", context);
        assertEquals(DEFAULT_VALUE, StaticApplicationContext.getProperty(KEY, DEFAULT_VALUE));
    }

    @Test
    void testGetPropertyWhenPropertyIsNull() {
        ApplicationContext context = mock(ApplicationContext.class);
        Environment environment = mock(Environment.class);
        when(environment.getProperty(eq(KEY), eq(DEFAULT_VALUE))).thenReturn(DEFAULT_VALUE);
        when(context.getEnvironment()).thenReturn(environment);
        ReflectionTestUtils.setField(StaticApplicationContext.class, "context", context);
        assertEquals(DEFAULT_VALUE, StaticApplicationContext.getProperty(KEY, DEFAULT_VALUE));
    }

    @Test
    void testGetPropertyWhenPropertyIsNotNull() {
        ApplicationContext context = mock(ApplicationContext.class);
        Environment environment = mock(Environment.class);
        when(environment.getProperty(eq(KEY), eq(DEFAULT_VALUE))).thenReturn(VALUE);
        when(context.getEnvironment()).thenReturn(environment);
        ReflectionTestUtils.setField(StaticApplicationContext.class, "context", context);
        assertEquals(VALUE, StaticApplicationContext.getProperty(KEY, DEFAULT_VALUE));
    }

}