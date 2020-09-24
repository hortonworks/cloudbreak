package com.sequenceiq.flow.cleanup;

import javax.inject.Inject;

import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.quartz.QuartzJobBean;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.service.TransactionService;
import com.sequenceiq.flow.core.FlowLogService;
import com.sequenceiq.flow.service.flowlog.FlowChainLogService;

@Component
public class FlowCleanupJob extends QuartzJobBean {

    private static final Logger LOGGER = LoggerFactory.getLogger(FlowCleanupJob.class);

    @Inject
    private TransactionService transactionService;

    @Inject
    private FlowLogService flowLogService;

    @Inject
    private FlowChainLogService flowChainLogService;

    @Override
    protected void executeInternal(JobExecutionContext context) throws JobExecutionException {
        try {
            purgeFinalisedFlowLogs();
        } catch (TransactionService.TransactionExecutionException e) {
            LOGGER.error("Transaction failed for flow cleanup.", e);
            throw new JobExecutionException(e);
        }
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
