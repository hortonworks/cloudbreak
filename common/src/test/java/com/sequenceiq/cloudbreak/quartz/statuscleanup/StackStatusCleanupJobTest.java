package com.sequenceiq.cloudbreak.quartz.statuscleanup;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.apache.commons.lang3.reflect.FieldUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.util.TimeUtil;

@ExtendWith(MockitoExtension.class)
public class StackStatusCleanupJobTest {

    @Mock
    private StackStatusCleanupConfig stackStatusCleanUpConfig;

    @Mock
    private StackStatusCleanupService stackStatusCleanupService;

    @Mock
    private TimeUtil timeUtil;

    @Mock
    private StackStatusCleanupJobService stackStatusCleanUpJobService;

    @InjectMocks
    private StackStatusCleanupJob underTest;

    @Test
    void testExecute() throws IllegalAccessException {
        FieldUtils.writeField(underTest, "stackStatusCleanupService", Optional.of(stackStatusCleanupService), true);
        int retention = 10;
        int limit = 100;
        long timestamp = System.currentTimeMillis();
        when(stackStatusCleanUpConfig.getRetentionPeriodInDays()).thenReturn(retention);
        doNothing().when(stackStatusCleanUpJobService).reschedule();
        when(stackStatusCleanUpConfig.getLimit()).thenReturn(limit);
        when(timeUtil.getTimestampThatDaysBeforeNow(anyInt())).thenReturn(timestamp);
        doNothing().when(stackStatusCleanupService).cleanupByTimestamp(anyInt(), anyLong());
        underTest.executeTracedJob(null);
        verify(timeUtil).getTimestampThatDaysBeforeNow(eq(retention));
        verify(stackStatusCleanupService).cleanupByTimestamp(eq(limit), eq(timestamp));
    }
}
