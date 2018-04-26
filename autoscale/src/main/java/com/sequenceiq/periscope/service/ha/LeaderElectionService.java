package com.sequenceiq.periscope.service.ha;

import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.Timer;
import java.util.TimerTask;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.service.Clock;
import com.sequenceiq.cloudbreak.service.TransactionService;
import com.sequenceiq.cloudbreak.service.TransactionService.TransactionExecutionException;
import com.sequenceiq.periscope.domain.Cluster;
import com.sequenceiq.periscope.domain.PeriscopeNode;
import com.sequenceiq.periscope.repository.ClusterRepository;
import com.sequenceiq.periscope.repository.PeriscopeNodeRepository;
import com.sequenceiq.periscope.service.StackCollectorService;

@Service
public class LeaderElectionService {

    private static final Logger LOGGER = LoggerFactory.getLogger(LeaderElectionService.class);

    private static final long LEADER_TASK_DELAY = 1000L;

    private static final long STACK_COLLECTOR_PERIOD = 10000L;

    @Value("${periscope.ha.heartbeat.threshold:60000}")
    private Integer heartbeatThresholdRate;

    @Inject
    private PeriscopeNodeConfig periscopeNodeConfig;

    @Inject
    private PeriscopeNodeRepository periscopeNodeRepository;

    @Inject
    private ClusterRepository clusterRepository;

    @Inject
    private Clock clock;

    @Inject
    private TransactionService transactionService;

    @Inject
    private StackCollectorService stackCollectorService;

    private Timer timer;

    private Supplier<Timer> timerFactory = Timer::new;

    @PostConstruct
    public void init() {
        timer = timerFactory.get();
        if (!periscopeNodeConfig.isNodeIdSpecified()) {
            timer.schedule(new TimerTask() {
                @Override
                public void run() {
                    try {
                        stackCollectorService.collectStackDetails();
                    } catch (RuntimeException e) {
                        LOGGER.error("Error happend during fetching stacks", e);
                    }
                }
            }, 0L, STACK_COLLECTOR_PERIOD);
        }
    }

    @Scheduled(initialDelay = 35000L, fixedDelay = 30000L)
    public void leaderElection() {
        if (periscopeNodeConfig.isNodeIdSpecified()) {
            long leaders = periscopeNodeRepository.countByLeaderIsTrueAndLastUpdatedIsGreaterThan(clock.getCurrentTime() - heartbeatThresholdRate);
            if (leaders == 0L) {
                LOGGER.info("There is no active leader available");
                resetTimer();
                try {
                    transactionService.required(() -> {
                        periscopeNodeRepository.deallocateLeader();
                        PeriscopeNode me = periscopeNodeRepository.findOne(periscopeNodeConfig.getId());
                        me.setLeader(true);
                        periscopeNodeRepository.save(me);
                        return null;
                    });
                } catch (TransactionExecutionException e) {
                    LOGGER.info("Failed to select node as leader, something went wrong. Message: {}", e.getMessage());
                    return;
                }
                LOGGER.info(String.format("Selected %s as leader", periscopeNodeConfig.getId()));
                timer.schedule(new TimerTask() {
                    @Override
                    public void run() {
                        try {
                            stackCollectorService.collectStackDetails();
                            reallocateOrphanClusters();
                        } catch (RuntimeException e) {
                            LOGGER.error("Error happend during fetching cluster allocating them to nodes", e);
                        }
                    }
                }, LEADER_TASK_DELAY, STACK_COLLECTOR_PERIOD);
            }
        }
    }

    private void reallocateOrphanClusters() {
        List<PeriscopeNode> nodes = periscopeNodeRepository.findAllByLastUpdatedIsGreaterThan(clock.getCurrentTime() - heartbeatThresholdRate);
        if (nodes.stream().noneMatch(n -> n.isLeader() && n.getUuid().equals(periscopeNodeConfig.getId()))) {
            Optional<PeriscopeNode> leader = nodes.stream().filter(PeriscopeNode::isLeader).findFirst();
            LOGGER.info(String.format("Leader is %s, let's drop leader scope", leader.isPresent() ? leader.get().getUuid() : "-"));
            resetTimer();
            return;
        }
        List<String> nodeIds = nodes.stream().map(PeriscopeNode::getUuid).collect(Collectors.toList());
        List<Cluster> orphanClusters = clusterRepository.findAllByPeriscopeNodeIdNotInOrPeriscopeNodeIdIsNull(nodeIds);
        if (!orphanClusters.isEmpty()) {
            Iterator<PeriscopeNode> iterator = nodes.iterator();
            for (Cluster cluster : orphanClusters) {
                if (!iterator.hasNext()) {
                    iterator = nodes.iterator();
                }
                cluster.setPeriscopeNodeId(iterator.next().getUuid());
                LOGGER.info(String.format("Allocationg cluster %s to node %s", cluster.getId(), cluster.getPeriscopeNodeId()));
            }
            clusterRepository.save(orphanClusters);
        }
    }

    private void resetTimer() {
        timer.cancel();
        timer.purge();
        timer = timerFactory.get();
    }
}
