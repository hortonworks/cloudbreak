package com.sequenceiq.it.cloudbreak.preflight;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.it.cloudbreak.config.database.DatabaseProperties;

@ExtendWith(MockitoExtension.class)
class PostgresAvailabilityCheckTest {

    @Mock
    private DatabaseProperties databaseProperties;

    @InjectMocks
    private PostgresAvailabilityCheck underTest;

    @Test
    void testCheckFailsWhenConnectionUnreachable() {
        when(databaseProperties.getConnectionUrl()).thenReturn("jdbc:postgresql://localhost:1/nonexistent");
        when(databaseProperties.getUsername()).thenReturn("postgres");
        when(databaseProperties.getPassword()).thenReturn("postgres");

        PreflightCheckResult result = underTest.check();

        assertThat(result.isPassed()).isFalse();
        assertThat(result.isSkipped()).isFalse();
    }
}
