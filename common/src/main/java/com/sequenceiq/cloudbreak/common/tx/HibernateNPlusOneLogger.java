package com.sequenceiq.cloudbreak.common.tx;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.cloudbreak.app.StaticApplicationContext;

public class HibernateNPlusOneLogger extends HibernateStatementStatistics {

    private static final Logger LOGGER = LoggerFactory.getLogger(HibernateNPlusOneLogger.class);

    private static final int DEFAULT_SESSION_MAX_STATEMENT_WARNING = 1000;

    @Override
    public void end() {
        super.end();
        int queryCount = getQueryCount();
        logStack(queryCount);
    }

    private void logStack(int queryCount) {
        String message = constructLogline();
        int maxStatementWarning = StaticApplicationContext.getEnvironmentProperty("hibernate.session.warning.max.count",
                Integer.class, DEFAULT_SESSION_MAX_STATEMENT_WARNING);
        if (queryCount > maxStatementWarning) {
            HibernateNPlusOneException e = new HibernateNPlusOneException(
                    String.format("You have executed %d queries in a single transaction, " +
                            "please doublecheck the entity relationship!", queryCount));
            LOGGER.warn(message, e);
        } else {
            LOGGER.debug(message);
        }
    }
}
