package com.sequenceiq.cloudbreak.service;

import java.util.Set;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.service.TransactionService;
import com.sequenceiq.cloudbreak.repository.CloudbreakFlowLogRepository;
import com.sequenceiq.cloudbreak.service.flowlog.FlowChainLogService;

@Component
public class CloudbreakFlowLogService {

    private static final Logger LOGGER = LoggerFactory.getLogger(CloudbreakFlowLogService.class);

    @Inject
    private CloudbreakFlowLogRepository cloudbreakFlowLogRepository;

    @Inject
    private FlowChainLogService flowChainLogService;

    @Inject
    private TransactionService transactionService;

    public void purgeTerminatedStacksFlowLogs() throws TransactionService.TransactionExecutionException {
        transactionService.required(() -> {
            LOGGER.debug("Cleaning deleted stack's flowlog");
            int purgedTerminatedStackLogs = cloudbreakFlowLogRepository.purgeTerminatedStackLogs();
            LOGGER.debug("Deleted flowlog count: {}", purgedTerminatedStackLogs);
            LOGGER.debug("Cleaning orphan flowchainlogs");
            int purgedOrphanFLowChainLogs = flowChainLogService.purgeOrphanFLowChainLogs();
            LOGGER.debug("Deleted flowchainlog count: {}", purgedOrphanFLowChainLogs);
            return null;
        });
    }

    public Set<Long> findTerminatingStacksByCloudbreakNodeId(String id) {
        return cloudbreakFlowLogRepository.findTerminatingStacksByCloudbreakNodeId(id);
    }
}
