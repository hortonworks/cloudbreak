package com.sequenceiq.cloudbreak.common.tx;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class HibernateStatementStatisticsLoggerTest {

    private HibernateStatementStatisticsLogger underTest;

    @BeforeEach
    public void setUp() {
        underTest = new HibernateStatementStatisticsLogger();
    }

    @Test
    public void testNoWarning() {
        callSQL(500);
        underTest.end();

        String log = underTest.constructStatementStatisticLogline();
        assertThat(log, containsString("executing 500 JDBC statements"));

        // No Exception shall thrown
    }

    @Test
    void testWarning() {
        callSQL(501);
        underTest.end();
        String log = underTest.constructStatementStatisticLogline();
        assertThat(log, containsString("executing 501 JDBC statements"));
        // No Exception shall thrown
    }

    private void callSQL(int count) {
        underTest.jdbcPrepareStatementStart();
        underTest.jdbcPrepareStatementEnd();

        for (int i = 0; i < count; i++) {
            underTest.jdbcExecuteStatementStart();
            underTest.jdbcExecuteStatementEnd();
        }
    }

}
