package com.sequenceiq.datalake.flow.upgrade.database.handler;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;

import java.util.function.Consumer;

import com.sequenceiq.datalake.service.sdx.PollingConfig;
import com.sequenceiq.datalake.service.sdx.poller.PollerRunner;
import com.sequenceiq.datalake.service.sdx.poller.PollerRunnerResult;

public class PollerRunnerMock {

    public void mockSuccess(PollerRunner pollerRunner) {
        mockRun(pollerRunner, PollerRunnerResult.ofSuccess());
    }

    public void mockError(PollerRunner pollerRunner) {
        mockRun(pollerRunner, PollerRunnerResult.ofError(new RuntimeException("my error"), "An error during polling"));
    }

    private static void mockRun(PollerRunner pollerRunner, PollerRunnerResult ofSuccess) {
        doAnswer(invocation -> {
            PollingConfig pollingConfig = invocation.getArgument(0);
            Consumer<PollingConfig> pollingMethodCaller = invocation.getArgument(1);
            pollingMethodCaller.accept(pollingConfig);
            return ofSuccess;
        }).when(pollerRunner).run(any(), any(), anyString(), any());
    }

}
