package com.sequenceiq.datalake.service.sdx.flowwait.task.factory;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.datalake.entity.SdxCluster;
import com.sequenceiq.datalake.service.sdx.PollingConfig;
import com.sequenceiq.datalake.service.sdx.flowcheck.CloudbreakFlowService;
import com.sequenceiq.datalake.service.sdx.flowwait.task.SdxWaitTaskForCloudbreakFlow;

@Component
public class SdxWaitTaskForCloudbreakFactory {

    @Inject
    private CloudbreakFlowService cloudbreakFlowService;

    public SdxWaitTaskForCloudbreakFlow createCloudbreakFlowWaitTask(SdxCluster sdxCluster, PollingConfig pollingConfig, String pollingMessage) {
        return new SdxWaitTaskForCloudbreakFlow(cloudbreakFlowService, sdxCluster, pollingConfig, pollingMessage);
    }

}
