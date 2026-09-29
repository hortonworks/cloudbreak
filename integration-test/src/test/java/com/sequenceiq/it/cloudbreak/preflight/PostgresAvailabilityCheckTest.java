package com.sequenceiq.it.cloudbreak.preflight;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.springframework.test.util.ReflectionTestUtils;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.sequenceiq.it.cloudbreak.config.database.DatabaseProperties;

public class PostgresAvailabilityCheckTest {

    private DatabaseProperties databaseProperties;

    private PostgresAvailabilityCheck underTest;

    @BeforeMethod
    public void setUp() {
        databaseProperties = mock(DatabaseProperties.class);

        underTest = new PostgresAvailabilityCheck();
        ReflectionTestUtils.setField(underTest, "databaseProperties", databaseProperties);
    }

    @Test
    public void testCheckFailsWhenConnectionUnreachable() {
        when(databaseProperties.getConnectionUrl()).thenReturn("jdbc:postgresql://localhost:1/nonexistent");
        when(databaseProperties.getUsername()).thenReturn("postgres");
        when(databaseProperties.getPassword()).thenReturn("postgres");

        PreflightCheckResult result = underTest.check();

        assertThat(result.isPassed()).isFalse();
        assertThat(result.isSkipped()).isFalse();
    }
}
