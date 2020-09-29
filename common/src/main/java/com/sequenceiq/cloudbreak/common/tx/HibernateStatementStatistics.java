package com.sequenceiq.cloudbreak.common.tx;

import static java.util.concurrent.TimeUnit.NANOSECONDS;

import org.hibernate.BaseSessionEventListener;

public class HibernateStatementStatistics extends BaseSessionEventListener {

    private int jdbcPrepareStatementCount;

    private long jdbcPrepareStatementTime;

    private int jdbcExecuteStatementCount;

    private long jdbcExecuteStatementTime;

    private long jdbcPrepStart = -1;

    private long jdbcExecutionStart = -1;

    public int getQueryCount() {
        return Math.max(jdbcPrepareStatementCount, jdbcExecuteStatementCount);
    }

    @Override
    public void jdbcPrepareStatementStart() {
        jdbcPrepStart = System.nanoTime();
    }

    @Override
    public void jdbcPrepareStatementEnd() {
        jdbcPrepareStatementCount++;
        jdbcPrepareStatementTime += System.nanoTime() - jdbcPrepStart;
        jdbcPrepStart = -1;
    }

    @Override
    public void jdbcExecuteStatementStart() {
        jdbcExecutionStart = System.nanoTime();
    }

    @Override
    public void jdbcExecuteStatementEnd() {
        jdbcExecuteStatementCount++;
        jdbcExecuteStatementTime += System.nanoTime() - jdbcExecutionStart;
        jdbcExecutionStart = -1;
    }

    public String constructLogline() {
        return String.format("Session Metrics " +
                        "%d s spent preparing %d JDBC statements; " +
                        "%d s spent executing %d JDBC statements",
                NANOSECONDS.toSeconds(jdbcPrepareStatementTime),
                jdbcPrepareStatementCount,
                NANOSECONDS.toSeconds(jdbcExecuteStatementTime),
                jdbcExecuteStatementCount);
    }
}
