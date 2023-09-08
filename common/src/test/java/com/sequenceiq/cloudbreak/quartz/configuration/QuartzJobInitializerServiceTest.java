package com.sequenceiq.cloudbreak.quartz.configuration;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.common.service.TransactionService;
import com.sequenceiq.cloudbreak.quartz.model.JobInitializer;
import com.sequenceiq.cloudbreak.quartz.statuschecker.StatusCheckerConfig;

@ExtendWith(MockitoExtension.class)
class QuartzJobInitializerServiceTest {

    @Mock
    private Optional<List<JobInitializer>> initJobDefinitions;

    @Mock
    private StatusCheckerConfig properties;

    @Mock
    private TransactionalScheduler transactionalScheduler;

    @Mock
    private TransactionalScheduler transactionalScheduler2;

    @Spy
    private List<TransactionalScheduler> transactionalSchedulers = new ArrayList<>();

    @Mock
    private JobInitializer jobInitializer1;

    @Mock
    private JobInitializer jobInitializer2;

    @InjectMocks
    private QuartzJobInitializerService underTest;

    @BeforeEach
    void init() {
        transactionalSchedulers.add(transactionalScheduler);
        transactionalSchedulers.add(transactionalScheduler2);
        when(initJobDefinitions.isPresent()).thenReturn(Boolean.TRUE);
        when(initJobDefinitions.get()).thenReturn(List.of(jobInitializer1, jobInitializer2));
    }

    @Test
    void testInitQuartz() throws TransactionService.TransactionExecutionException {
        when(properties.isAutoSyncEnabled()).thenReturn(Boolean.TRUE);
        underTest.initQuartz();
        verify(transactionalScheduler, times(1)).clear();
        verify(transactionalScheduler2, times(1)).clear();
        verify(jobInitializer1, times(1)).initJobs();
        verify(jobInitializer2, times(1)).initJobs();
    }
}