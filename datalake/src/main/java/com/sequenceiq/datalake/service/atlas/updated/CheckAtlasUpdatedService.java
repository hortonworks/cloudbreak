package com.sequenceiq.datalake.service.atlas.updated;

import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.StackV4Endpoint;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.crn.RegionAwareInternalCrnGeneratorFactory;
import com.sequenceiq.datalake.entity.SdxCluster;
import com.sequenceiq.datalake.service.sdx.CloudbreakPoller;
import com.sequenceiq.datalake.service.sdx.PollingConfig;
import com.sequenceiq.datalake.service.sdx.flowcheck.CloudbreakFlowService;
import com.sequenceiq.flow.api.model.FlowIdentifier;

@Service
public class CheckAtlasUpdatedService {
    @Value("${sdx.stack.check_atlas_updated.sleeptime_sec:5}")
    private int checkAtlasUpdatedSleepTimeSec;

    @Value("${sdx.stack.check_atlas_updated.duration_min:25}")
    private int checkAtlasUpdatedDurationMin;

    @Inject
    private StackV4Endpoint stackV4Endpoint;

    @Inject
    private RegionAwareInternalCrnGeneratorFactory regionAwareInternalCrnGeneratorFactory;

    @Inject
    private CloudbreakFlowService cloudbreakFlowService;

    @Inject
    private CloudbreakPoller cloudbreakPoller;

    public void verifyAtlasUpToDate(SdxCluster sdxCluster) {
        String initiatorUserCrn = ThreadBasedUserCrnProvider.getUserCrn();
        FlowIdentifier flowId = ThreadBasedUserCrnProvider.doAsInternalActor(
                regionAwareInternalCrnGeneratorFactory.iam().getInternalCrnForServiceAsString(),
                () -> stackV4Endpoint.checkAtlasUpdated(0L, sdxCluster.getCrn(), initiatorUserCrn)
        );
        cloudbreakFlowService.saveLastCloudbreakFlowChainId(sdxCluster, flowId);
        PollingConfig pollingConfig = new PollingConfig(
                checkAtlasUpdatedSleepTimeSec, TimeUnit.SECONDS,
                checkAtlasUpdatedDurationMin, TimeUnit.MINUTES
        ).withStopPollingIfExceptionOccurred(Boolean.TRUE);
        cloudbreakPoller.pollFlowChainStateUntilComplete(
                "check atlas service up to date", sdxCluster, pollingConfig
        );
    }
}
