package com.sequenceiq.cloudbreak.common.tx;

import static java.util.concurrent.TimeUnit.NANOSECONDS;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.cloudbreak.app.StaticApplicationContext;

public class HibernateStatementStatisticsLogger extends HibernateStatementStatistics {

    private static final Logger LOGGER = LoggerFactory.getLogger(HibernateStatementStatisticsLogger.class);

    private static final int DEFAULT_SESSION_MAX_STATEMENT_WARNING = 500;

    private static final long DEFAULT_SESSION_MAX_TIME_WARNING = 1000;

    private final int maxStatementWarning;

    private final long maxTimeWarning;

    public HibernateStatementStatisticsLogger() {
        this.maxStatementWarning = StaticApplicationContext.getEnvironmentProperty("hibernate.session.warning.max.count",
                Integer.class, DEFAULT_SESSION_MAX_STATEMENT_WARNING);
        this.maxTimeWarning = StaticApplicationContext.getEnvironmentProperty("hibernate.session.warning.max.time",
                Long.class, DEFAULT_SESSION_MAX_TIME_WARNING);
    }

    @Override
    public void end() {
        logMaxStatementWarning(getQueryCount());
        logJdbcExecutionTimeWarning();
    }

    private void logMaxStatementWarning(int queryCount) {
        String message = constructStatementStatisticLogline();
        if (queryCount > maxStatementWarning) {
            LOGGER.warn("Statement max count warning (>{}): {}", maxStatementWarning, message, new HibernateNPlusOneException(queryCount));
        } else {
            LOGGER.trace("Statement max count warning (>{}): {}", maxStatementWarning, message);
        }
    }

    private void logJdbcExecutionTimeWarning() {
        long jdbcExecuteStatementTime = NANOSECONDS.toMillis(getJdbcExecuteStatementTime());
        LOGGER.trace("Max time warning threshold: {}", maxTimeWarning);
        if (jdbcExecuteStatementTime > maxTimeWarning) {
            LOGGER.debug("JDBC Execution Statement time warning (>{}ms): {}ms", maxTimeWarning, jdbcExecuteStatementTime);
        }
        long jdbcExecuteBatchTime = NANOSECONDS.toMillis(getJdbcExecuteBatchTime());
        if (jdbcExecuteBatchTime > maxTimeWarning) {
            LOGGER.debug("JDBC Execution Batch time warning (>{}ms): {}ms", maxTimeWarning, jdbcExecuteBatchTime);
        }
        long jdbcPrepareStatementTime = NANOSECONDS.toMillis(getJdbcPrepareStatementTime());
        if (jdbcPrepareStatementTime > maxTimeWarning) {
            LOGGER.debug("JDBC Prepare Statement time warning (>{}ms): {}ms", maxTimeWarning, jdbcPrepareStatementTime);
        }
    }

}
