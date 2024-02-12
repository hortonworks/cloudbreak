package com.sequenceiq.cloudbreak.quartz.configuration.scheduler;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.quartz.SchedulerConfigException;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.scheduling.quartz.SchedulerFactoryBean;

class LocalTimedExecutorThreadPoolTest {

    private LocalTimedExecutorThreadPool underTest;

    @BeforeEach
    void setUp() {
        underTest = new LocalTimedExecutorThreadPool();
    }

    @Test
    void initalizeShouldFailIfTaskExecutorMissing() {
        SchedulerConfigException schedulerConfigException = assertThrows(SchedulerConfigException.class, () -> underTest.initialize());
        assertEquals("No local Executor found for configuration - 'taskExecutor' property must be set on SchedulerFactoryBean",
                schedulerConfigException.getMessage());
    }

    @Test
    void initalizeShouldFailIfTaskExecutorIsNotTimedSimpleThreadPoolTaskExecutor() throws Exception {
        try (MockedStatic<SchedulerFactoryBean> schedulerFactoryBean = mockStatic(SchedulerFactoryBean.class)) {
            schedulerFactoryBean.when(() -> SchedulerFactoryBean.getConfigTimeTaskExecutor()).thenReturn(new ThreadPoolTaskExecutor());
            SchedulerConfigException schedulerConfigException = assertThrows(SchedulerConfigException.class, () -> underTest.initialize());
            assertEquals("No valid Executor found for configuration - 'taskExecutor' must be a TimedSimpleThreadPoolTaskExecutor instance",
                    schedulerConfigException.getMessage());
        }
    }

    @Test
    void initalizeShouldSuccess() throws SchedulerConfigException {
        try (MockedStatic<SchedulerFactoryBean> schedulerFactoryBean = mockStatic(SchedulerFactoryBean.class)) {
            TimedSimpleThreadPoolTaskExecutor timedSimpleThreadPoolTaskExecutor = mock(TimedSimpleThreadPoolTaskExecutor.class);
            schedulerFactoryBean.when(() -> SchedulerFactoryBean.getConfigTimeTaskExecutor()).thenReturn(timedSimpleThreadPoolTaskExecutor);
            underTest.initialize();
            assertDoesNotThrow(() -> underTest.initialize());
        }
    }

    @Test
    void testRunInThread() throws SchedulerConfigException {
        try (MockedStatic<SchedulerFactoryBean> schedulerFactoryBean = mockStatic(SchedulerFactoryBean.class)) {
            TimedSimpleThreadPoolTaskExecutor timedSimpleThreadPoolTaskExecutor = mock(TimedSimpleThreadPoolTaskExecutor.class);
            schedulerFactoryBean.when(() -> SchedulerFactoryBean.getConfigTimeTaskExecutor()).thenReturn(timedSimpleThreadPoolTaskExecutor);
            underTest.initialize();
            Runnable task = mock(Runnable.class);
            underTest.runInThread(task);
            verify(timedSimpleThreadPoolTaskExecutor, times(1)).runInThread(eq(task));
        }
    }

    @Test
    void testBlockForAvailableThreads() throws SchedulerConfigException {
        try (MockedStatic<SchedulerFactoryBean> schedulerFactoryBean = mockStatic(SchedulerFactoryBean.class)) {
            TimedSimpleThreadPoolTaskExecutor timedSimpleThreadPoolTaskExecutor = mock(TimedSimpleThreadPoolTaskExecutor.class);
            schedulerFactoryBean.when(() -> SchedulerFactoryBean.getConfigTimeTaskExecutor()).thenReturn(timedSimpleThreadPoolTaskExecutor);
            underTest.initialize();
            underTest.blockForAvailableThreads();
            verify(timedSimpleThreadPoolTaskExecutor, times(1)).blockForAvailableThreads();
        }
    }

    @Test
    void testShutdown() throws SchedulerConfigException {
        try (MockedStatic<SchedulerFactoryBean> schedulerFactoryBean = mockStatic(SchedulerFactoryBean.class)) {
            TimedSimpleThreadPoolTaskExecutor timedSimpleThreadPoolTaskExecutor = mock(TimedSimpleThreadPoolTaskExecutor.class);
            schedulerFactoryBean.when(() -> SchedulerFactoryBean.getConfigTimeTaskExecutor()).thenReturn(timedSimpleThreadPoolTaskExecutor);
            underTest.initialize();
            underTest.shutdown(true);
            verify(timedSimpleThreadPoolTaskExecutor, times(1)).shutdown(eq(Boolean.TRUE));
        }
    }
}