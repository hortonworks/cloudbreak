package com.sequenceiq.cloudbreak.common.tx;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationContext;
import org.springframework.core.env.Environment;

@ExtendWith(MockitoExtension.class)
public class HibernateCircuitBreakerConfigProviderTest {

    @Mock
    private ApplicationContext applicationContext;

    @Mock
    private Environment environment;

    @BeforeEach
    public void setUp() {
        when(applicationContext.getEnvironment()).thenReturn(environment);
    }

    @Test
    public void testInit() {
        when(environment.getProperty("hibernate.session.circuitbreak.max.count", Integer.class, 1500)).thenReturn(1501);
        when(environment.getProperty("hibernate.session.warning.max.count", Integer.class, 500)).thenReturn(501);
        when(environment.getProperty("hibernate.session.warning.max.time", Long.class, 1000L)).thenReturn(2002L);
        when(environment.getProperty("hibernate.transaction.warning.max.time", Long.class, 500L)).thenReturn(502L);

        HibernateCircuitBreakerConfigProvider.init(applicationContext);

        assertEquals(1501, HibernateCircuitBreakerConfigProvider.getMaxStatementBreak());
        assertEquals(501, HibernateCircuitBreakerConfigProvider.getMaxStatementWarning());
        assertEquals(2002L, HibernateCircuitBreakerConfigProvider.getMaxTimeWarning());
        assertEquals(502L, HibernateCircuitBreakerConfigProvider.getMaxTransactionTimeThreshold());
    }
}