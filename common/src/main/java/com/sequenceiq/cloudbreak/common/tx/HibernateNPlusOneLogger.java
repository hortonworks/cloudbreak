package com.sequenceiq.cloudbreak.common.tx;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.cloudbreak.app.StaticApplicationContext;

public class HibernateNPlusOneLogger extends HibernateStatementStatistics {

    private static final Logger LOGGER = LoggerFactory.getLogger(HibernateNPlusOneLogger.class);

    private static final int DEFAULT_SESSION_MAX_STATEMENT_WARNING = 500;

    private final int maxStatementWarning;

    public HibernateNPlusOneLogger() {
        this.maxStatementWarning = StaticApplicationContext.getEnvironmentProperty("hibernate.session.warning.max.count",
                Integer.class, DEFAULT_SESSION_MAX_STATEMENT_WARNING);
    }

    @Override
    public void end() {
        log(getQueryCount());
    }

    private void log(int queryCount) {
        String message = constructLogline();
        if (queryCount > maxStatementWarning) {
            LOGGER.warn(message, new HibernateNPlusOneException(queryCount));
        } else {
            LOGGER.debug(message);
        }
    }
}
