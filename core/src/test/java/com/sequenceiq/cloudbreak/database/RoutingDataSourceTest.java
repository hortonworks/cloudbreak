package com.sequenceiq.cloudbreak.database;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.lenient;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Collection;
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

    @Mock
    private Connection defaultConnection;

    @Mock
    private Connection quartzConnection;

    /**
     * @return Collection of thread names inspected while running quartz jobs
     */
    private static Collection<String> getQuartzThreadNames() {
        return Set.of(
                "quartzExecutor-6",
                "quartzMeteringExecutor-6",
                "quartzMeteringSyncExecutor-6",
                "quartzDynamicEntitlementExecutor-6",
                "QuartzScheduler_quartzScheduler-dbajzath1648036370090_ClusterManager",
                "QuartzScheduler_quartzScheduler-dbajzath1648036370090_MisfireHandler"
        );
    }

    @BeforeEach
    void setUp() throws SQLException {
        lenient().when(defaultDataSource.getConnection()).thenReturn(defaultConnection);
        lenient().when(quartzDataSource.getConnection()).thenReturn(quartzConnection);
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
}
