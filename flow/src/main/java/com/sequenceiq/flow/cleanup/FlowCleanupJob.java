package com.sequenceiq.flow.cleanup;

import java.util.Optional;

import jakarta.inject.Inject;

import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.service.TransactionService;
import com.sequenceiq.cloudbreak.logger.MdcContextInfoProvider;
import com.sequenceiq.cloudbreak.quartz.MdcQuartzJob;
import com.sequenceiq.flow.core.FlowLogService;
import com.sequenceiq.flow.core.FlowRegister;
import com.sequenceiq.flow.core.cache.FlowStatCache;
import com.sequenceiq.flow.service.flowlog.FlowChainLogService;

@Component
public class FlowCleanupJob extends MdcQuartzJob {

    private static final Logger LOGGER = LoggerFactory.getLogger(FlowCleanupJob.class);

    @Inject
    private TransactionService transactionService;

    @Inject
    private FlowLogService flowLogService;

    @Inject
    private FlowChainLogService flowChainLogService;

    @Inject
    private FlowCleanupConfig flowCleanupConfig;

    @Inject
    private FlowRegister runningFlows;

    @Inject
    private FlowStatCache flowStatCache;

    @Override
    protected Optional<MdcContextInfoProvider> getMdcContextConfigProvider() {
        return Optional.empty();
    }

    @Override
    protected void executeTracedJob(JobExecutionContext context) throws JobExecutionException {
        try {
            purgeFinalisedFlowLogs(flowCleanupConfig.getRetentionPeriodInHours(), flowCleanupConfig.getRetentionPeriodInHoursForFailedFlows());
            purgeFlowStatCache();
        } catch (TransactionService.TransactionExecutionException e) {
            LOGGER.error("Transaction failed for flow cleanup.", e);
            throw new JobExecutionException(e);
        }
    }

    private void purgeFlowStatCache() {
        flowStatCache.cleanOldCacheEntries(runningFlows.getRunningFlowIdsSnapshot());
    }

    public void purgeFinalisedFlowLogs(int retentionPeriodHours, int retentionPeriodHoursForFailedFlows)
            throws TransactionService.TransactionExecutionException {
        transactionService.required(() -> {
            LOGGER.debug("Cleaning successful finalised flowlogs");
            int purgedFinalizedSuccessfulFlowLogs = flowLogService.purgeFinalizedSuccessfulFlowLogs(retentionPeriodHours);
            LOGGER.debug("Deleted successful flowlog count: {}", purgedFinalizedSuccessfulFlowLogs);
            LOGGER.debug("Cleaning failed finalised flowlogs");
            int purgedFinalizedFailedFlowLogs = flowLogService.purgeFinalizedFailedFlowLogs(retentionPeriodHoursForFailedFlows);
            LOGGER.debug("Deleted failed flowlog count: {}", purgedFinalizedFailedFlowLogs);
            LOGGER.debug("Cleaning orphan flowchainlogs");
            int purgedOrphanFLowChainLogs = flowChainLogService.purgeOrphanFlowChainLogs();
            LOGGER.debug("Deleted flowchainlog count: {}", purgedOrphanFLowChainLogs);
            return null;
        });
    }
}
