package com.sequenceiq.datalake.service.sdx.attach;

import java.util.concurrent.TimeUnit;

import jakarta.inject.Inject;
import jakarta.ws.rs.NotFoundException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.StackV4Endpoint;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.crn.RegionAwareInternalCrnGeneratorFactory;
import com.sequenceiq.datalake.entity.SdxCluster;
import com.sequenceiq.datalake.service.sdx.CloudbreakPoller;
import com.sequenceiq.datalake.service.sdx.PollingConfig;
import com.sequenceiq.flow.api.model.FlowIdentifier;
import com.sequenceiq.redbeams.api.endpoint.v4.databaseserver.DatabaseServerV4Endpoint;

@Component
public class SdxAttachDetachUtils {
    private static final Logger LOGGER = LoggerFactory.getLogger(SdxAttachDetachUtils.class);

    @Value("${sdx.stack.re_register_cluster_proxy_config.sleeptime_sec:2}")
    private int reRegisterClusterProxyConfigSleepTimeInSec;

    @Value("${sdx.stack.re_register_cluster_proxy_config.duration_min:10}")
    private int reRegisterClusterProxyConfigDurationTimeInMin;

    @Inject
    private StackV4Endpoint stackV4Endpoint;

    @Inject
    private DatabaseServerV4Endpoint redbeamsServerEndpoint;

    @Inject
    private RegionAwareInternalCrnGeneratorFactory regionAwareInternalCrnGeneratorFactory;

    @Inject
    private CloudbreakPoller cloudbreakPoller;

    public void updateClusterNameAndCrn(SdxCluster sdxCluster, String newName, String newCrn) {
        sdxCluster.setClusterName(newName);
        sdxCluster.setOriginalCrn(sdxCluster.getCrn());
        sdxCluster.setCrn(newCrn);
        sdxCluster.setStackCrn(newCrn);
    }

    public void updateStack(String originalName, String newName, String newCrn, boolean retainOriginalName) {
        String initiatorUserCrn = ThreadBasedUserCrnProvider.getUserCrn();
        ThreadBasedUserCrnProvider.doAsInternalActor(
            regionAwareInternalCrnGeneratorFactory.iam().getInternalCrnForServiceAsString(),
            () -> {
                try {
                    stackV4Endpoint.updateNameAndCrn(
                            0L, originalName, initiatorUserCrn, newName, newCrn, retainOriginalName
                    );
                } catch (NotFoundException e) {
                    LOGGER.warn("Stack not found for original name: '" + originalName +
                            "'. Skipping update of stack.", e);
                }
            }
        );
    }

    public void updateExternalDatabase(SdxCluster cluster, String originalCrn) {
        String initiatorUserCrn = ThreadBasedUserCrnProvider.getUserCrn();
        ThreadBasedUserCrnProvider.doAsInternalActor(
                regionAwareInternalCrnGeneratorFactory.iam().getInternalCrnForServiceAsString(),
                () -> {
                    try {
                        redbeamsServerEndpoint.updateClusterCrn(
                                cluster.getEnvCrn(), originalCrn, cluster.getCrn(), initiatorUserCrn
                        );
                    } catch (NotFoundException e) {
                        LOGGER.warn("External DB not found for original CRN: '" + originalCrn +
                                "'. Skipping update of external DB.", e);
                    }
                }
        );
    }

    public void reRegisterClusterProxyConfig(SdxCluster cluster, boolean skipFullReRegistration, String originalCrn) {
        LOGGER.info("Attempting to re-register the cluster proxy config for SDX cluster with ID: {}", cluster.getId());
        String initiatorUserCrn = ThreadBasedUserCrnProvider.getUserCrn();
        FlowIdentifier flowIdentifier = ThreadBasedUserCrnProvider.doAsInternalActor(
                regionAwareInternalCrnGeneratorFactory.iam().getInternalCrnForServiceAsString(),
                () -> stackV4Endpoint.reRegisterClusterProxyConfig(0L, cluster.getCrn(), skipFullReRegistration, originalCrn, initiatorUserCrn)
        );
        PollingConfig pollingConfig = new PollingConfig(
                reRegisterClusterProxyConfigSleepTimeInSec, TimeUnit.SECONDS,
                reRegisterClusterProxyConfigDurationTimeInMin, TimeUnit.MINUTES
        ).withStopPollingIfExceptionOccurred(Boolean.TRUE);
        cloudbreakPoller.pollFlowStateByFlowIdentifierUntilComplete(
                "re-register cluster proxy config", flowIdentifier, cluster.getId(), pollingConfig
        );
        LOGGER.info("Finished re-register of cluster proxy config for SDX cluster with ID: {}", cluster.getId());
    }
}
