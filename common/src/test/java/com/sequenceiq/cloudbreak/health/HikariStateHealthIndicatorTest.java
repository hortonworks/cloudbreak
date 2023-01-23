package com.sequenceiq.cloudbreak.health;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import javax.sql.DataSource;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.Status;
import org.springframework.test.util.ReflectionTestUtils;

import com.zaxxer.hikari.HikariDataSource;

@ExtendWith(MockitoExtension.class)
public class HikariStateHealthIndicatorTest {
    @Spy
    private List<DataSource> dataSources = new ArrayList<>();

    @Mock
    private HikariDataSource genericDs;

    @Mock
    private HikariDataSourcePoolMetadataExtractor hikariDataSourcePoolMetadataExtractor;

    @InjectMocks
    private HikariStateHealthIndicator underTest;

    @BeforeEach
    public void setup() throws Exception {
        dataSources.clear();
        dataSources.add(genericDs);
    }

    @Test
    public void testHealthWithNormalPoolSize() {
        configDataSource(genericDs, "app", 1, 30);
        ReflectionTestUtils.setField(underTest, "setReadinessProbeDownIfPoolIsFull", false);

        Health health = underTest.health();

        assertEquals(Status.UP, health.getStatus());
    }

    @Test
    public void testPoolIsFull() {
        configDataSource(genericDs, "app", 30, 30);
        ReflectionTestUtils.setField(underTest, "setReadinessProbeDownIfPoolIsFull", false);

        Health health = underTest.health();

        assertEquals(Status.UP, health.getStatus());
    }

    @Test
    public void testPoolIsFullAndRedinessDownIsEnabled() {
        configDataSource(genericDs, "app", 30, 30);
        ReflectionTestUtils.setField(underTest, "setReadinessProbeDownIfPoolIsFull", true);

        Health health = underTest.health();

        assertEquals(Status.DOWN, health.getStatus());
    }

    private void configDataSource(HikariDataSource ds, String poolName, Integer active, Integer max) {
        when(ds.getPoolName()).thenReturn(poolName);
        when(hikariDataSourcePoolMetadataExtractor.extract(any())).thenReturn(new DataSourcePoolMetadata(active, max - active, max));
    }
}