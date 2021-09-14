package com.sequenceiq.datalake.service.sdx.flowwait.task.factory;

import javax.inject.Inject;

import org.springframework.stereotype.Service;

import com.sequenceiq.datalake.entity.SdxCluster;
import com.sequenceiq.datalake.service.sdx.PollingConfig;
import com.sequenceiq.datalake.service.sdx.flowwait.task.SdxWaitTaskForCloudbreakFlow;

@Service
public class SdxWaitTaskFactory {

    @Inject
    private SdxWaitTaskForCloudbreakFactory sdxWaitTaskForCloudbreakFactory;

    public SdxWaitTaskForCloudbreakFlow createCloudbreakFlowWaitTask(SdxCluster sdxCluster, PollingConfig pollingConfig, String pollingMessage) {
        return sdxWaitTaskForCloudbreakFactory.createCloudbreakFlowWaitTask(sdxCluster, pollingConfig, pollingMessage);
    }
}
