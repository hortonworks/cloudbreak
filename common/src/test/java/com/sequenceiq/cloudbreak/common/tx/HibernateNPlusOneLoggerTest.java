package com.sequenceiq.cloudbreak.common.tx;

import static org.hamcrest.CoreMatchers.containsString;
import static org.junit.Assert.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class HibernateNPlusOneLoggerTest {

    private HibernateNPlusOneLogger underTest;

    @BeforeEach
    public void setUp() {
        underTest = new HibernateNPlusOneLogger();
    }

    @Test
    public void testNoWarning() {
        callSQL(1000);
        underTest.end();

        String log = underTest.constructLogline();
        assertThat(log, containsString("executing 1000 JDBC statements"));

        // No Exception shall thrown
    }

    @Test
    void testWarning() {
        callSQL(1001);
        underTest.end();

        String log = underTest.constructLogline();
        assertThat(log, containsString("executing 1001 JDBC statements"));
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
