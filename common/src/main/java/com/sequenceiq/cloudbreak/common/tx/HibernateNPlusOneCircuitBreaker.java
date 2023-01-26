package com.sequenceiq.cloudbreak.common.tx;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.cloudbreak.app.StaticApplicationContext;

public class HibernateNPlusOneCircuitBreaker extends HibernateStatementStatisticsLogger {

    private static final Logger LOGGER = LoggerFactory.getLogger(HibernateNPlusOneCircuitBreaker.class);

    private static final int DEFAULT_SESSION_MAX_STATEMENT_CIRCUIT_BREAK = 1500;

    private final int maxStatementBreak;

    public HibernateNPlusOneCircuitBreaker() {
        maxStatementBreak = StaticApplicationContext.getEnvironmentProperty("hibernate.session.circuitbreak.max.count",
                Integer.class, DEFAULT_SESSION_MAX_STATEMENT_CIRCUIT_BREAK);
    }

    @Override
    public void jdbcPrepareStatementEnd() {
        super.jdbcPrepareStatementEnd();
        breakCircuit();
    }

    @Override
    public void jdbcExecuteStatementEnd() {
        super.jdbcExecuteStatementEnd();
        breakCircuit();
    }

    private void breakCircuit() {
        int queryCount = getQueryCount();
        if (getQueryCount() > maxStatementBreak) {
            HibernateNPlusOneException e = new HibernateNPlusOneException(queryCount);
            LOGGER.error("Max allowed statement: {}", maxStatementBreak, e);
            throw e;
        }
    }
}
