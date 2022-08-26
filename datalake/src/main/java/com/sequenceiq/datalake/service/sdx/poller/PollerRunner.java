package com.sequenceiq.datalake.service.sdx.poller;

import java.util.function.Consumer;

import com.sequenceiq.datalake.entity.SdxCluster;
import com.sequenceiq.datalake.service.sdx.PollingConfig;

@FunctionalInterface
public interface PollerRunner {
    PollerRunnerResult run(
            PollingConfig pollingConfig,
            Consumer<PollingConfig> pollingCommand,
            String processName,
            SdxCluster sdxCluster);
}
