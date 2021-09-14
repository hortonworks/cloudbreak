package com.sequenceiq.datalake.service.sdx.flowwait.task;

import com.dyngr.core.AttemptMaker;
import com.sequenceiq.datalake.entity.SdxCluster;
import com.sequenceiq.datalake.service.sdx.PollingConfig;

public abstract class SdxWaitTask<V> implements AttemptMaker<V> {

    private final SdxCluster sdxCluster;

    private final PollingConfig pollingConfig;

    private final String pollingMessage;

    public SdxWaitTask(SdxCluster sdxCluster, PollingConfig pollingConfig, String pollingMessage) {
        this.sdxCluster = sdxCluster;
        this.pollingConfig = pollingConfig;
        this.pollingMessage = pollingMessage;
    }

    public SdxCluster getSdxCluster() {
        return sdxCluster;
    }

    public PollingConfig getPollingConfig() {
        return pollingConfig;
    }

    public String getPollingMessage() {
        return pollingMessage;
    }

    @Override
    public String toString() {
        return "SdxWaitTask{" +
                "sdxCluster=" + sdxCluster +
                ", pollingConfig=" + pollingConfig +
                ", pollingMessage='" + pollingMessage + '\'' +
                '}';
    }
}
