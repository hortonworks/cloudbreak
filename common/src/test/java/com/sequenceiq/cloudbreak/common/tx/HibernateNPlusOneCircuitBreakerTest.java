package com.sequenceiq.cloudbreak.common.tx;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.fail;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class HibernateNPlusOneCircuitBreakerTest {

    private HibernateNPlusOneCircuitBreaker underTest;

    @BeforeEach
    public void setUp() {
        underTest = new HibernateNPlusOneCircuitBreaker();
        HibernateCircuitBreakerConfigProvider.init(null);
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
        callSQL(1500);
        underTest.end();

        String log = underTest.constructStatementStatisticLogline();
        assertThat(log, containsString("executing 1500 JDBC statements"));
        // No Exception shall thrown
    }

    @Test
    void testBreak() {
        try {
            callSQL(1501);
            fail("We are over 1500 queries per hibernate session! Circuit breaker should have thrown an HibernateNPlusOneException");
        } catch (HibernateNPlusOneException e) {
            // No Exception shall thrown
            assertThat(e.getMessage(), containsString("executed 1501 queries in a single transaction"));
        }
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
