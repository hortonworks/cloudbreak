package com.sequenceiq.cloudbreak.quartz.configuration;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.common.service.TransactionService;
import com.sequenceiq.cloudbreak.quartz.configuration.scheduler.TransactionalScheduler;
import com.sequenceiq.cloudbreak.quartz.model.JobInitializer;
import com.sequenceiq.cloudbreak.quartz.statuschecker.StatusCheckerConfig;

@ExtendWith(MockitoExtension.class)
class QuartzJobInitializerServiceTest {

    @Mock
    private Optional<List<JobInitializer>> initJobDefinitions;

    @Mock
    private StatusCheckerConfig properties;

    @Mock
    private TransactionalScheduler transactionalScheduler1;

    @Mock
    private TransactionalScheduler transactionalScheduler2;

    @Mock
    private TransactionalScheduler transactionalScheduler3;

    @Spy
    private List<TransactionalScheduler> transactionalSchedulers = new ArrayList<>();

    @Mock
    private JobInitializer1 jobInitializer1;

    @Mock
    private JobInitializer jobInitializer2;

    @Mock
    private JobInitializer3 jobInitializer3;

    @InjectMocks
    private QuartzJobInitializerService underTest;

    @BeforeEach
    void init() {
        transactionalSchedulers.add(transactionalScheduler1);
        transactionalSchedulers.add(transactionalScheduler2);
        transactionalSchedulers.add(transactionalScheduler3);
        when(initJobDefinitions.isPresent()).thenReturn(Boolean.TRUE);
        when(initJobDefinitions.get()).thenReturn(List.of(jobInitializer1, jobInitializer2, jobInitializer3));
    }

    @Test
    void testInitQuartz() throws TransactionService.TransactionExecutionException {
        when(properties.isAutoSyncEnabled()).thenReturn(Boolean.TRUE);
        underTest.initQuartz();
        verify(transactionalScheduler1, times(1)).clear();
        verify(transactionalScheduler2, times(1)).clear();
        verify(transactionalScheduler3, times(1)).clear();
        verify(jobInitializer1, times(1)).initJobs();
        verify(jobInitializer2, times(1)).initJobs();
        verify(jobInitializer3, times(1)).initJobs();
    }

    @Test
    void testInitQuartzJobInitException() {
        when(properties.isAutoSyncEnabled()).thenReturn(Boolean.TRUE);
        RuntimeException exception1 = new RuntimeException("exception1");
        RuntimeException exception3 = new RuntimeException("exception3");
        doThrow(exception1).when(jobInitializer1).initJobs();
        doThrow(exception3).when(jobInitializer3).initJobs();

        CloudbreakServiceException actualException = Assertions.assertThrows(CloudbreakServiceException.class, () -> underTest.initQuartz());

        verify(jobInitializer1, times(1)).initJobs();
        verify(jobInitializer2, times(1)).initJobs();
        verify(jobInitializer3, times(1)).initJobs();
        Assertions.assertEquals(exception1, actualException.getSuppressed()[0]);
        Assertions.assertEquals(exception3, actualException.getSuppressed()[1]);
    }

    private static class JobInitializer1 implements JobInitializer {
        @Override
        public void initJobs() {
        }
    }

    private static class JobInitializer3 implements JobInitializer {
        @Override
        public void initJobs() {
        }
    }
}
