package com.sequenceiq.cloudbreak.health;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
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
        // Since we have not done a dump yet, therefroe it should be default 0 (aka 1st Jan, 1970)
        assertEquals(0, Long.parseLong(ReflectionTestUtils.getField(underTest, "lastDumpTime").toString()));

        configDataSource(genericDs, "app", 30, 30);
        ReflectionTestUtils.setField(underTest, "setReadinessProbeDownIfPoolIsFull", true);

        Health health1 = underTest.health();
        Health health2 = underTest.health();

        //just to check whether we ate not flipping state after two consequential run
        assertEquals(health1.getStatus(), health2.getStatus());
        assertEquals(Status.DOWN, health2.getStatus());

        // After we made the dump, it should not be 0 (aka 1st Jan, 1970)
        assertNotEquals(0, Long.parseLong(ReflectionTestUtils.getField(underTest, "lastDumpTime").toString()));
    }

    private void configDataSource(HikariDataSource ds, String poolName, Integer active, Integer max) {
        when(ds.getPoolName()).thenReturn(poolName);
        when(hikariDataSourcePoolMetadataExtractor.extract(any())).thenReturn(new DataSourcePoolMetadata(active, max - active, max));
    }
}