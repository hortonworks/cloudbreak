package com.sequenceiq.cloudbreak.database;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import javax.sql.DataSource;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RoutingDataSourceTest {

    @InjectMocks
    private RoutingDataSource underTest;

    @Mock(name = "defaultDataSource")
    private DataSource defaultDataSource;

    @Mock(name = "quartzDataSource")
    private DataSource quartzDataSource;

    @Mock(name = "quartzMeteringDataSource")
    private DataSource quartzMeteringDataSource;

    @Mock
    private Map<String, DataSource> dataSources;

    @Mock
    private Connection defaultConnection;

    @Mock
    private Connection quartzConnection;

    @Mock
    private Connection quartzMeteringConnection;

    /**
     * @return Collection of thread names inspected while running quartz jobs
     */
    private static Collection<String> getQuartzThreadNames() {
        return Set.of(
                "quartzExecutor-6",
                "QuartzScheduler_quartzScheduler-dbajzath1648036370090_ClusterManager",
                "QuartzScheduler_quartzScheduler-dbajzath1648036370090_MisfireHandler",
                "quartzScheduler_QuartzSchedulerThread");
    }

    private static Collection<String> getQuartzMeteringThreadNames() {
        return Set.of(
                "quartzMeteringExecutor-6",
                "QuartzScheduler_quartzMeteringScheduler-local1710246430961_MisfireHandler",
                "QuartzScheduler_quartzMeteringScheduler-local1710246430961_ClusterManager",
                "quartzMeteringScheduler_QuartzSchedulerThread");
    }

    @BeforeEach
    void setUp() throws SQLException {
        lenient().when(defaultDataSource.getConnection()).thenReturn(defaultConnection);
        lenient().when(quartzDataSource.getConnection()).thenReturn(quartzConnection);
        lenient().when(quartzMeteringDataSource.getConnection()).thenReturn(quartzMeteringConnection);
        when(dataSources.entrySet()).thenReturn(Map.of("quartzDataSource", quartzDataSource,
                "defaultDataSource", defaultDataSource,
                "quartzMeteringDataSource", quartzMeteringDataSource).entrySet());
        underTest.setUp();
        underTest.afterPropertiesSet();
    }

    @Test
    void shouldReturnDefaultDataSourceOnNonQuartzThread() {
        Connection result = getConnectionInThread("main");

        assertThat(result).isEqualTo(defaultConnection);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("getQuartzThreadNames")
    void shouldReturnQuartzDataSourceOnQuartzThread(String threadName) {
        Connection result = getConnectionInThread(threadName);

        assertThat(result).isEqualTo(quartzConnection);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("getQuartzMeteringThreadNames")
    void shouldReturnQuartzMeteringDataSourceOnQuartzThread(String threadName) {
        Connection result = getConnectionInThread(threadName);

        assertThat(result).isEqualTo(quartzMeteringConnection);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("getQuartzThreadNames")
    void shouldReturnQuartzDataSourceOnQuartzVirtualThread(String threadName) {
        Connection result = getConnectionInVirtualThread(threadName);

        assertThat(result).isEqualTo(quartzConnection);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("getQuartzMeteringThreadNames")
    void shouldReturnQuartzMeteringDataSourceOnQuartzVirtualThread(String threadName) {
        Connection result = getConnectionInVirtualThread(threadName);

        assertThat(result).isEqualTo(quartzMeteringConnection);
    }

    private Connection getConnectionInThread(String threadName) {
        try {
            AtomicReference<Connection> connection = new AtomicReference<>();

            Thread thread = new Thread(() -> {
                try {
                    connection.set(underTest.getConnection());
                } catch (SQLException e) {
                    throw new RuntimeException(e);
                }
            });
            thread.setName(threadName);
            thread.start();
            thread.join();

            return connection.get();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    private Connection getConnectionInVirtualThread(String threadName) {
        try {
            AtomicReference<Connection> connection = new AtomicReference<>();

            Thread thread = Thread.ofVirtual().name(threadName).start(() -> {
                try {
                    connection.set(underTest.getConnection());
                } catch (SQLException e) {
                    throw new RuntimeException(e);
                }
            });
            thread.join();
            return connection.get();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}
