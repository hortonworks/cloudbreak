package com.sequenceiq.cloudbreak.common.tx;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class HibernateStatementStatisticsTest {

    private HibernateStatementStatistics underTest;

    @BeforeEach
    void setUp() {
        underTest = new HibernateStatementStatistics();
    }

    @Test
    void testLogMessage() {
        underTest.jdbcPrepareStatementStart();
        underTest.jdbcPrepareStatementEnd();

        underTest.jdbcExecuteStatementStart();
        underTest.jdbcExecuteStatementEnd();
        underTest.jdbcExecuteStatementStart();
        underTest.jdbcExecuteStatementEnd();

        String log = underTest.constructLogline();

        assertEquals(2, underTest.getQueryCount(), "Number of executed queries does not match");
        assertThat(log, containsString("preparing 1 JDBC statements"));
        assertThat(log, containsString("executing 2 JDBC statements"));
    }
}
