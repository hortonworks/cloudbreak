package com.sequenceiq.datalake.service.sdx.flowwait;

import javax.inject.Inject;

import org.springframework.stereotype.Service;

import com.sequenceiq.datalake.entity.SdxCluster;
import com.sequenceiq.datalake.service.sdx.PollingConfig;
import com.sequenceiq.datalake.service.sdx.flowwait.task.factory.SdxWaitTaskFactory;
import com.sequenceiq.datalake.service.sdx.flowwait.task.SdxWaitTaskForCloudbreakFlow;
import com.sequenceiq.datalake.service.sdx.flowwait.task.SdxWaitTaskService;

@Service
public class SdxWaitService {

    @Inject
    private SdxWaitTaskService sdxWaitTaskService;

    @Inject
    private SdxWaitTaskFactory sdxWaitTaskFactory;

    /**
     * Will wait for a cloudbreak flow or flowchain. Once it stops, the flow is checked if it succeeded.
     * @param sdxCluster The cluster for which a flow or flowchain is to be checked
     * @param pollingConfig Config class for polling
     * @param pollingMessage A message describing the action that is polled, and is to be used in logs and messages
     */
    public void waitForCloudbreakFlow(SdxCluster sdxCluster, PollingConfig pollingConfig, String pollingMessage) {
        SdxWaitTaskForCloudbreakFlow sdxWaitTaskForCloudbreakFlow = sdxWaitTaskFactory.createCloudbreakFlowWaitTask(sdxCluster, pollingConfig, pollingMessage);
        sdxWaitTaskService.waitFor(sdxWaitTaskForCloudbreakFlow);
    }

}
