package com.sequenceiq.cloudbreak.common.tx;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationContext;

import com.sequenceiq.cloudbreak.common.metrics.CommonMetricService;

@ExtendWith(MockitoExtension.class)
class HibernateMetricsProviderTest {

    @Mock
    private ApplicationContext applicationContext;

    @Mock
    private CommonMetricService commonMetricService;

    @BeforeEach
    void setUp() throws Exception {
        Field metricServiceField = HibernateMetricsProvider.class.getDeclaredField("metricService");
        metricServiceField.setAccessible(true);
        metricServiceField.set(null, null);
    }

    @Test
    void testInitWithValidContext() {
        when(applicationContext.getBean(CommonMetricService.class)).thenReturn(commonMetricService);
        HibernateMetricsProvider.init(applicationContext);
        assertNotNull(HibernateMetricsProvider.getMetricService());
    }

    @Test
    void testInitWithNullContext() {
        HibernateMetricsProvider.init(null);
        assertNull(HibernateMetricsProvider.getMetricService());
    }

    @Test
    void testInitWhenBeanNotAvailable() {
        when(applicationContext.getBean(CommonMetricService.class)).thenThrow(new RuntimeException("No bean"));
        HibernateMetricsProvider.init(applicationContext);
        assertNull(HibernateMetricsProvider.getMetricService());
    }
}
