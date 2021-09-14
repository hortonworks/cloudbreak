package com.sequenceiq.datalake.service.sdx.flowwait;

import javax.inject.Inject;

import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;

import com.sequenceiq.datalake.entity.SdxCluster;
import com.sequenceiq.datalake.service.sdx.PollingConfig;

@Service
public class SdxWaitTaskFactory {

    @Inject
    private ApplicationContext applicationContext;

    public SdxWaitTaskForCloudbreakFlow createCloudbreakFlowWaitTask(SdxCluster sdxCluster, PollingConfig pollingConfig, String pollingMessage) {
        return applicationContext.getBean(SdxWaitTaskForCloudbreakFlow.class, sdxCluster, pollingConfig, pollingMessage);
    }
}
