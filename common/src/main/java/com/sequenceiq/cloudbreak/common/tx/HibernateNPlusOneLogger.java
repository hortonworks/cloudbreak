package com.sequenceiq.cloudbreak.common.tx;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HibernateNPlusOneLogger extends HibernateStatementStatistics {

    private static final Logger LOGGER = LoggerFactory.getLogger(HibernateNPlusOneLogger.class);

    private static final int SESSION_MAX_STATEMENT_WARNING = 1000;

    @Override
    public void end() {
        super.end();
        int queryCount = getQueryCount();
        logStack(queryCount);
    }

    private void logStack(int queryCount) {
        String message = constructLogline();
        if (queryCount > SESSION_MAX_STATEMENT_WARNING) {
            HibernateNPlusOneException e = new HibernateNPlusOneException(
                    String.format("You have executed %d queries in a single transaction, " +
                            "please doublecheck the entity relationship!", queryCount));
            LOGGER.warn(message, e);
        } else {
            LOGGER.debug(message);
        }
    }
}
