package com.sequenceiq.datalake.service.sdx.flowwait;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import com.dyngr.Polling;
import com.dyngr.core.AttemptResult;
import com.dyngr.core.AttemptResults;
import com.dyngr.exception.PollerException;
import com.sequenceiq.datalake.entity.SdxCluster;
import com.sequenceiq.datalake.service.sdx.PollingConfig;
import com.sequenceiq.datalake.service.sdx.SdxService;
import com.sequenceiq.datalake.service.sdx.flowwait.exception.SdxWaitException;
import com.sequenceiq.datalake.service.sdx.flowwait.task.SdxWaitTask;
import com.sequenceiq.datalake.service.sdx.flowwait.task.SdxWaitTaskService;

@ExtendWith(MockitoExtension.class)
public class SdxWaitTaskServiceTest {

    private static final long SLEEP_TIME = 43L;

    private static final int DURATION = 51;

    private static final TimeUnit SLEEP_TIME_UNIT = TimeUnit.SECONDS;

    private static final TimeUnit DURATION_TIME_UNIT = TimeUnit.MINUTES;

    private static final PollingConfig POLLING_CONFIG = new PollingConfig(SLEEP_TIME, SLEEP_TIME_UNIT, DURATION, DURATION_TIME_UNIT)
            .withStopPollingIfExceptionOccurred(true);

    @Mock
    private Polling.PollingOptions pollingOptions;

    @Mock
    private SdxService sdxService;

    @InjectMocks
    private SdxWaitTaskService underTest;

    @Test
    void testWaitFor() {
        try (MockedStatic<Polling> polling = mockStatic(Polling.class)) {
            SdxWaitTask<?> sdxWaitTask = new TestWaitTask(new SdxCluster(), POLLING_CONFIG, "My Sdx task");
            setupPolling(polling);

            underTest.waitFor(sdxWaitTask);

            verify(pollingOptions).stopAfterDelay(DURATION, DURATION_TIME_UNIT);
            verify(pollingOptions).stopIfException(Boolean.TRUE);
            verify(pollingOptions).run(sdxWaitTask);
        }
    }

    @Test
    void testWaitForWithPollerException() {
        try (MockedStatic<Polling> polling = mockStatic(Polling.class)) {
            SdxWaitTask<?> sdxWaitTask = new TestWaitTask(new SdxCluster(), POLLING_CONFIG, "My Sdx task");
            setupPolling(polling);
            doThrow(new PollerException()).when(pollingOptions).run(sdxWaitTask);

            assertThrows(SdxWaitException.class, () -> {
                underTest.waitFor(sdxWaitTask);
            });

            verify(pollingOptions).stopAfterDelay(DURATION, DURATION_TIME_UNIT);
            verify(pollingOptions).stopIfException(Boolean.TRUE);
            verify(pollingOptions).run(sdxWaitTask);
        }
    }

    private void setupPolling(MockedStatic<Polling> polling) {
        when(pollingOptions.stopAfterDelay(DURATION, DURATION_TIME_UNIT)).thenReturn(pollingOptions);
        when(pollingOptions.stopIfException(Boolean.TRUE)).thenReturn(pollingOptions);
        polling.when(() -> Polling.waitPeriodly(SLEEP_TIME, SLEEP_TIME_UNIT)).thenReturn(pollingOptions);
    }

    private static class TestWaitTask extends SdxWaitTask<String> {

        TestWaitTask(SdxCluster sdxCluster, PollingConfig pollingConfig, String pollingMessage) {
            super(sdxCluster, pollingConfig, pollingMessage);
        }

        @Override
        public AttemptResult<String> process() throws Exception {
            return AttemptResults.finishWith("Ready");
        }
    }
}
