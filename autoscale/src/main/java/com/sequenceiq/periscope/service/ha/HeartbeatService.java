package com.sequenceiq.periscope.service.ha;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.service.Clock;
import com.sequenceiq.cloudbreak.service.Retry;
import com.sequenceiq.cloudbreak.service.Retry.ActionWentFailException;
import com.sequenceiq.cloudbreak.service.TransactionService;
import com.sequenceiq.cloudbreak.service.TransactionService.TransactionExecutionException;
import com.sequenceiq.periscope.domain.PeriscopeNode;
import com.sequenceiq.periscope.repository.ClusterRepository;
import com.sequenceiq.periscope.repository.PeriscopeNodeRepository;

@Service
public class HeartbeatService {

    private static final Logger LOGGER = LoggerFactory.getLogger(HeartbeatService.class);

    @Inject
    private PeriscopeNodeConfig periscopeNodeConfig;

    @Inject
    private PeriscopeNodeRepository periscopeNodeRepository;

    @Inject
    @Qualifier("DefaultRetryService")
    private Retry retryService;

    @Inject
    private Clock clock;

    @Inject
    private ClusterRepository clusterRepository;

    @Inject
    private TransactionService transactionService;

    @PostConstruct
    public void init() {
        heartbeat(true);
    }

    @Scheduled(cron = "${cb.ha.heartbeat.rate:0/30 * * * * *}")
    public void heartbeat() {
        heartbeat(false);
    }

    private void heartbeat(boolean unLeaderIt) {
        if (periscopeNodeConfig.isNodeIdSpecified()) {
            String nodeId = periscopeNodeConfig.getId();
            try {
                retryService.testWith2SecDelayMax5Times(() -> {
                    try {
                        PeriscopeNode self = periscopeNodeRepository.findById(nodeId).orElse(new PeriscopeNode(nodeId));
                        self.setLastUpdated(clock.getCurrentTimeMillis());
                        if (unLeaderIt) {
                            self.setLeader(false);
                        }
                        periscopeNodeRepository.save(self);
                    } catch (RuntimeException e) {
                        LOGGER.error("Failed to update the heartbeat timestamp", e);
                        throw new ActionWentFailException(e.getMessage());
                    }
                    return Boolean.TRUE;
                });
            } catch (ActionWentFailException af) {
                LOGGER.error("Failed to update the heartbeat timestamp 5 times for node {}: {}", nodeId, af.getMessage());
                try {
                    transactionService.required(() -> {
                        clusterRepository.deallocateClustersOfNode(nodeId);
                        return null;
                    });
                } catch (TransactionExecutionException e) {
                    LOGGER.error("Unable to deallocate clusters", e);
                }
            }
        }
    }
}
