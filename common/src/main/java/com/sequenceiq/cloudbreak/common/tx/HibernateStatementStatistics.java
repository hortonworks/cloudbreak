package com.sequenceiq.cloudbreak.common.tx;

import static java.util.concurrent.TimeUnit.NANOSECONDS;

import org.apache.commons.lang3.math.NumberUtils;
import org.hibernate.BaseSessionEventListener;

/**
 * Collecting the most important JDBC statistics. This class is similar to @org.hibernate.engine.internal.StatisticalLoggingSessionEventListener, but
 * it is more targeted and has less footprint.
 */
public class HibernateStatementStatistics extends BaseSessionEventListener {

    private int jdbcPrepareStatementCount;

    private long jdbcPrepareStatementTime;

    private int jdbcExecuteStatementCount;

    private long jdbcExecuteStatementTime;

    private int jdbcExecuteBatchCount;

    private long jdbcExecuteBatchTime;

    private long jdbcPrepStart = -1;

    private long jdbcExecutionStart = -1;

    private long jdbcExecuteBatchStart = -1;

    public int getQueryCount() {
        return NumberUtils.max(jdbcPrepareStatementCount, jdbcExecuteStatementCount, jdbcPrepareStatementCount);
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

    @Override
    public void jdbcExecuteBatchStart() {
        jdbcExecuteBatchStart = System.nanoTime();
    }

    @Override
    public void jdbcExecuteBatchEnd() {
        jdbcExecuteBatchCount++;
        jdbcExecuteBatchTime += System.nanoTime() - jdbcExecuteBatchStart;
        jdbcExecuteBatchStart = -1;
    }

    public String constructLogline() {
        return String.format("Hibernate Session Metrics " +
                        "%d s spent preparing %d JDBC statements; " +
                        "%d s spent executing %d JDBC statements; " +
                        "%d s spent executing %d JDBC batch statements",
                NANOSECONDS.toSeconds(jdbcPrepareStatementTime),
                jdbcPrepareStatementCount,
                NANOSECONDS.toSeconds(jdbcExecuteStatementTime),
                jdbcExecuteStatementCount,
                NANOSECONDS.toSeconds(jdbcExecuteBatchTime),
                jdbcExecuteBatchCount);
    }
}
