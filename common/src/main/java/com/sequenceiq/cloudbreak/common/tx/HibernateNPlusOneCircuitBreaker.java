package com.sequenceiq.cloudbreak.common.tx;

import static com.sequenceiq.cloudbreak.common.tx.HibernateCircuitBreakerConfigProvider.getMaxStatementBreak;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HibernateNPlusOneCircuitBreaker extends HibernateStatementStatisticsLogger {

    private static final Logger LOGGER = LoggerFactory.getLogger(HibernateNPlusOneCircuitBreaker.class);

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
        if (getQueryCount() > getMaxStatementBreak()) {
            HibernateNPlusOneException e = new HibernateNPlusOneException(queryCount);
            LOGGER.error("Max allowed statement: {}", getMaxStatementBreak(), e);
            throw e;
        }
    }
}
