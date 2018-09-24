package com.sequenceiq.periscope.monitor.executor;

import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.concurrent.ExecutorService;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.sequenceiq.periscope.utils.LoggerUtils;
import com.sequenceiq.periscope.utils.MetricUtils;

@Ignore
public class LoggedExecutorServiceTest {

    @Mock
    private ExecutorService executorService;

    @Mock
    private LoggerUtils loggerUtils;

    @Mock
    private MetricUtils metricUtils;

    @InjectMocks
    private LoggedExecutorService underTest;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testSubmit() {
        Runnable runnable = () -> {
        };

        underTest.submit("name", runnable);

        verify(executorService).submit(runnable);
        verify(loggerUtils).logThreadPoolExecutorParameters(any(), eq("name"), eq(executorService));
        verify(metricUtils).submitThreadPoolExecutorParameters(eq(executorService));
    }

    @Test
    public void testSubmitWhenExecutorServiceThrows() {
        Runnable runnable = () -> {
        };
        when(executorService.submit(runnable)).thenThrow(new RuntimeException("exception from ExecutorService"));

        try {
            underTest.submit("name", runnable);
            fail("expected eception from executorService");
        } catch (RuntimeException e) {
            verify(executorService).submit(runnable);
            verify(loggerUtils).logThreadPoolExecutorParameters(any(), eq("name"), eq(executorService));
            verify(metricUtils).submitThreadPoolExecutorParameters(eq(executorService));
        }
    }

}
