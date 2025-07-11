package com.sequenceiq.cloudbreak.common.tx;

import static com.sequenceiq.cloudbreak.common.tx.HibernateCircuitBreakerConfigProvider.getMaxStatementWarning;
import static com.sequenceiq.cloudbreak.common.tx.HibernateCircuitBreakerConfigProvider.getMaxTimeWarning;
import static java.util.concurrent.TimeUnit.NANOSECONDS;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HibernateStatementStatisticsLogger extends HibernateStatementStatistics {

    private static final Logger LOGGER = LoggerFactory.getLogger(HibernateStatementStatisticsLogger.class);

    @Override
    public void end() {
        logMaxStatementWarning(getQueryCount());
        logJdbcExecutionTimeWarning();
    }

    private void logMaxStatementWarning(int queryCount) {
        String message = constructStatementStatisticLogline();
        if (queryCount > getMaxStatementWarning()) {
            LOGGER.warn("Statement max count warning (>{}): {}", getMaxStatementWarning(), message, new HibernateNPlusOneException(queryCount));
        } else {
            LOGGER.trace("Statement max count warning (>{}): {}", getMaxStatementWarning(), message);
        }
    }

    private void logJdbcExecutionTimeWarning() {
        long jdbcExecuteStatementTime = NANOSECONDS.toMillis(getJdbcExecuteStatementTime());
        LOGGER.trace("Max time warning threshold: {}", getMaxTimeWarning());
        if (jdbcExecuteStatementTime > getMaxTimeWarning()) {
            LOGGER.debug("JDBC Execution Statement time warning (>{}ms): {}ms. Method: {}", getMaxTimeWarning(), jdbcExecuteStatementTime, getRelevantMethod());
        }
        long jdbcExecuteBatchTime = NANOSECONDS.toMillis(getJdbcExecuteBatchTime());
        if (jdbcExecuteBatchTime > getMaxTimeWarning()) {
            LOGGER.debug("JDBC Execution Batch time warning (>{}ms): {}ms. Method: {}", getMaxTimeWarning(), jdbcExecuteBatchTime, getRelevantMethod());
        }
        long jdbcPrepareStatementTime = NANOSECONDS.toMillis(getJdbcPrepareStatementTime());
        if (jdbcPrepareStatementTime > getMaxTimeWarning()) {
            LOGGER.debug("JDBC Prepare Statement time warning (>{}ms): {}ms. Method: {}", getMaxTimeWarning(), jdbcPrepareStatementTime, getRelevantMethod());
        }
    }

    private String getRelevantMethod() {
        try {
            StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
            for (StackTraceElement element : stackTrace) {
                String className = element.getClassName();
                if (!className.startsWith("org.hibernate") &&
                        !className.startsWith("java.") &&
                        !className.startsWith("jdk.") &&
                        !className.startsWith("sun.") &&
                        !className.startsWith("org.springframework.") &&
                        !className.contains("SessionEventListener") &&
                        !className.contains(getClass().getSimpleName())) {
                    return element.toString();
                }
            }
        } catch (Exception e) {
            LOGGER.trace("Failed to find out relevant method for long query.");
        }
        return "unknown";
    }

}
