package com.sequenceiq.flow.cleanup;

import javax.inject.Inject;

import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.service.TransactionService;
import com.sequenceiq.cloudbreak.quartz.TracedQuartzJob;
import com.sequenceiq.flow.core.FlowLogService;
import com.sequenceiq.flow.core.FlowRegister;
import com.sequenceiq.flow.core.cache.FlowStatCache;
import com.sequenceiq.flow.service.flowlog.FlowChainLogService;

import io.opentracing.Tracer;

@Component
public class FlowCleanupJob extends TracedQuartzJob {

    private static final Logger LOGGER = LoggerFactory.getLogger(FlowCleanupJob.class);

    @Inject
    private TransactionService transactionService;

    @Inject
    private FlowLogService flowLogService;

    @Inject
    private FlowChainLogService flowChainLogService;

    @Inject
    private FlowRegister runningFlows;

    @Inject
    private FlowStatCache flowStatCache;

    public FlowCleanupJob(Tracer tracer) {
        super(tracer, "Flow Cleanup Job");
    }

    @Override
    protected Object getMdcContextObject() {
        return null;
    }

    @Override
    protected void executeTracedJob(JobExecutionContext context) throws JobExecutionException {
        try {
            purgeFinalisedFlowLogs();
            purgeFlowStatCache();
        } catch (TransactionService.TransactionExecutionException e) {
            LOGGER.error("Transaction failed for flow cleanup.", e);
            throw new JobExecutionException(e);
        }
    }

    private void purgeFlowStatCache() {
        flowStatCache.cleanOldCacheEntries(runningFlows.getRunningFlowIds());
    }

    public void purgeFinalisedFlowLogs() throws TransactionService.TransactionExecutionException {
        transactionService.required(() -> {
            LOGGER.debug("Cleaning finalised flowlogs");
            int purgedFinalizedFlowLogs = flowLogService.purgeFinalizedFlowLogs();
            LOGGER.debug("Deleted flowlog count: {}", purgedFinalizedFlowLogs);
            LOGGER.debug("Cleaning orphan flowchainlogs");
            int purgedOrphanFLowChainLogs = flowChainLogService.purgeOrphanFLowChainLogs();
            LOGGER.debug("Deleted flowchainlog count: {}", purgedOrphanFLowChainLogs);
            return null;
        });
    }
}
