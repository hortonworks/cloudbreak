package com.sequenceiq.cloudbreak.common.tx;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.core.env.Environment;

public class HibernateCircuitBreakerConfigProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(HibernateCircuitBreakerConfigProvider.class);

    private static final int DEFAULT_SESSION_MAX_STATEMENT_CIRCUIT_BREAK = 1500;

    private static final int DEFAULT_SESSION_MAX_STATEMENT_WARNING = 500;

    private static final long DEFAULT_SESSION_MAX_TIME_WARNING = 1000;

    private static final long DEFAULT_TRANSACTION_MAX_TIME_WARNING = 500;

    private static int maxStatementBreak;

    private static int maxStatementWarning;

    private static long maxTimeWarning;

    private static long maxTransactionTimeThreshold;

    private HibernateCircuitBreakerConfigProvider() {
    }

    public static void init(ApplicationContext applicationContext) {
        maxStatementBreak = getEnvironmentProperty(applicationContext, "hibernate.session.circuitbreak.max.count",
                Integer.class, DEFAULT_SESSION_MAX_STATEMENT_CIRCUIT_BREAK);

        maxStatementWarning = getEnvironmentProperty(applicationContext, "hibernate.session.warning.max.count",
                Integer.class, DEFAULT_SESSION_MAX_STATEMENT_WARNING);

        maxTimeWarning = getEnvironmentProperty(applicationContext, "hibernate.session.warning.max.time",
                Long.class, DEFAULT_SESSION_MAX_TIME_WARNING);

        maxTransactionTimeThreshold = getEnvironmentProperty(applicationContext, "hibernate.transaction.warning.max.time",
                Long.class, DEFAULT_TRANSACTION_MAX_TIME_WARNING);

        LOGGER.info("Hibernate circuit breaker configuration: maxStatementBreak: {}, maxStatementWarning: {}, maxTimeWarning: {}," +
                        " maxTransactionTimeThreshold: {}", maxStatementBreak, maxStatementWarning, maxTimeWarning, maxTransactionTimeThreshold);
    }

    public static int getMaxStatementBreak() {
        return maxStatementBreak;
    }

    public static int getMaxStatementWarning() {
        return maxStatementWarning;
    }

    public static long getMaxTimeWarning() {
        return maxTimeWarning;
    }

    public static long getMaxTransactionTimeThreshold() {
        return maxTransactionTimeThreshold;
    }

    private static <T> T getEnvironmentProperty(ApplicationContext applicationContext, String key, Class<T> targetType, T defaultValue) {
        T ret = defaultValue;
        if (applicationContext != null) {
            Environment environment = applicationContext.getEnvironment();
            if (environment != null) {
                ret = environment.getProperty(key, targetType, defaultValue);
            }
        }
        return ret;
    }
}
