package com.sequenceiq.datalake.service.sdx;

import static com.sequenceiq.datalake.flow.cert.renew.event.SdxCertRenewalEvent.CERT_RENEWAL_STARTED_EVENT;
import static com.sequenceiq.datalake.flow.create.SdxCreateEvent.SDX_STACK_CREATION_IN_PROGRESS_EVENT;
import static com.sequenceiq.datalake.flow.datalake.upgrade.DatalakeUpgradeEvent.DATALAKE_UPGRADE_IN_PROGRESS_EVENT;
import static com.sequenceiq.datalake.flow.datalake.upgrade.DatalakeUpgradeEvent.DATALAKE_VM_REPLACE_IN_PROGRESS_EVENT;
import static com.sequenceiq.datalake.flow.repair.SdxRepairEvent.SDX_REPAIR_IN_PROGRESS_EVENT;
import static com.sequenceiq.datalake.flow.start.SdxStartEvent.SDX_START_IN_PROGRESS_EVENT;
import static com.sequenceiq.datalake.flow.stop.SdxStopEvent.SDX_STOP_IN_PROGRESS_EVENT;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.InternalServerErrorException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.google.common.collect.Sets;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.StackV4Endpoint;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.crn.RegionAwareInternalCrnGeneratorFactory;
import com.sequenceiq.datalake.entity.SdxCluster;
import com.sequenceiq.datalake.flow.dr.backup.DatalakeBackupFlowConfig;
import com.sequenceiq.datalake.flow.dr.restore.DatalakeRestoreFlowConfig;
import com.sequenceiq.flow.api.model.FlowIdentifier;
import com.sequenceiq.flow.core.Flow2Handler;
import com.sequenceiq.flow.core.FlowEvent;
import com.sequenceiq.flow.core.config.FlowConfiguration;
import com.sequenceiq.flow.domain.FlowLog;

@Service
public class SdxRetryService {

    private static final Logger LOGGER = LoggerFactory.getLogger(SdxRetryService.class);

    private static final Set<Class<? extends FlowConfiguration<?>>> FULLY_RESTARTABLE_FLOWS =  Sets.newHashSet(
            DatalakeRestoreFlowConfig.class,
            DatalakeBackupFlowConfig.class);

    @Inject
    private Flow2Handler flow2Handler;

    @Inject
    private StackV4Endpoint stackV4Endpoint;

    @Inject
    private RegionAwareInternalCrnGeneratorFactory regionAwareInternalCrnGeneratorFactory;

    public FlowIdentifier retrySdx(SdxCluster sdxCluster) {
        FlowLog flowLog = flow2Handler.getFirstRetryableStateLogfromLatestFlow(sdxCluster.getId());
        if (!flowLog.getFlowType().isOnClassPath()) {
            throw new InternalServerErrorException(String.format("Flow type %s is not on classpath", flowLog.getFlowType().getName()));
        }
        if (FULLY_RESTARTABLE_FLOWS.contains(flowLog.getFlowType().getClassValue())) {
            return flow2Handler.retryLastFailedFlowFromStart(sdxCluster.getId());
        } else {
            return flow2Handler.retryLastFailedFlow(sdxCluster.getId(), lastSuccessfulStateLog -> retryCloudbreakIfNecessary(sdxCluster,
                    lastSuccessfulStateLog));
        }
    }

    private void retryCloudbreakIfNecessary(SdxCluster sdxCluster, FlowLog lastSuccessfulStateLog) {
        if (isCloudbreakRetryNecessary(lastSuccessfulStateLog.getNextEvent())) {
            LOGGER.info("Last successful state was " + lastSuccessfulStateLog.getNextEvent() + ", so try a retry on stack");
            try {
                ThreadBasedUserCrnProvider.doAsInternalActor(
                        regionAwareInternalCrnGeneratorFactory.iam().getInternalCrnForServiceAsString(),
                        () -> stackV4Endpoint.retry(0L, sdxCluster.getClusterName(), sdxCluster.getAccountId()));
            } catch (BadRequestException e) {
                LOGGER.info("Sdx retry failed on cloudbreak side, but try to restart the flow. Related exception: ", e);
            }
        }
    }

    private boolean isCloudbreakRetryNecessary(String retriedEvent) {
        return getStackRetryEvents().stream().anyMatch(flowEvent -> retriedEvent.equals(flowEvent.name()) || retriedEvent.equals(flowEvent.event()));
    }

    private List<FlowEvent> getStackRetryEvents() {
        return Arrays.asList(
                SDX_STACK_CREATION_IN_PROGRESS_EVENT,
                DATALAKE_UPGRADE_IN_PROGRESS_EVENT,
                DATALAKE_VM_REPLACE_IN_PROGRESS_EVENT,
                SDX_REPAIR_IN_PROGRESS_EVENT,
                SDX_START_IN_PROGRESS_EVENT,
                SDX_STOP_IN_PROGRESS_EVENT,
                CERT_RENEWAL_STARTED_EVENT
        );
    }
}
