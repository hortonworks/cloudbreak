package com.sequenceiq.cloudbreak.common.tx;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;

import com.sequenceiq.cloudbreak.common.metrics.CommonMetricService;
import com.sequenceiq.cloudbreak.common.metrics.MetricService;

public class HibernateMetricsProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(HibernateMetricsProvider.class);

    private static MetricService metricService;

    private HibernateMetricsProvider() {
    }

    public static void init(ApplicationContext applicationContext) {
        if (applicationContext != null) {
            try {
                metricService = applicationContext.getBean(CommonMetricService.class);
                LOGGER.info("HibernateMetricsProvider initialized with CommonMetricService");
            } catch (Exception e) {
                LOGGER.warn("MetricService bean not available, Hibernate metrics will not be recorded");
            }
        }
    }

    public static MetricService getMetricService() {
        return metricService;
    }
}
