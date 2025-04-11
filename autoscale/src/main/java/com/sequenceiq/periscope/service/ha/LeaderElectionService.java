package com.sequenceiq.periscope.service.ha;

import static com.sequenceiq.cloudbreak.common.exception.NotFoundException.notFound;

import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Timer;
import java.util.TimerTask;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.common.service.Clock;
import com.sequenceiq.cloudbreak.common.service.TransactionService;
import com.sequenceiq.cloudbreak.common.service.TransactionService.TransactionExecutionException;
import com.sequenceiq.cloudbreak.ha.NodeConfig;
import com.sequenceiq.periscope.domain.Cluster;
import com.sequenceiq.periscope.domain.MetricType;
import com.sequenceiq.periscope.domain.PeriscopeNode;
import com.sequenceiq.periscope.domain.TimeAlert;
import com.sequenceiq.periscope.monitor.evaluator.CronTimeEvaluator;
import com.sequenceiq.periscope.repository.ClusterRepository;
import com.sequenceiq.periscope.repository.PeriscopeNodeRepository;
import com.sequenceiq.periscope.service.DateTimeService;
import com.sequenceiq.periscope.service.PeriscopeMetricService;
import com.sequenceiq.periscope.utils.TimeUtil;

@Service
public class LeaderElectionService {

    private static final Logger LOGGER = LoggerFactory.getLogger(LeaderElectionService.class);

    private static final long LEADER_TASK_DELAY = 1000L;

    private static final long STACK_COLLECTOR_PERIOD = 10000L;

    @Value("${periscope.ha.heartbeat.threshold:60000}")
    private Integer heartbeatThresholdRate;

    @Inject
    private ApplicationContext applicationContext;

    @Inject
    private NodeConfig periscopeNodeConfig;

    @Inject
    private PeriscopeNodeRepository periscopeNodeRepository;

    @Inject
    private ClusterRepository clusterRepository;

    @Inject
    private Clock clock;

    @Inject
    private TransactionService transactionService;

    @Inject
    private DateTimeService dateTimeService;

    @Inject
    private PeriscopeMetricService metricService;

    private Timer timer;

    private Supplier<Timer> timerFactory = Timer::new;

    @PostConstruct
    public void init() {
        timer = timerFactory.get();
    }

    @Scheduled(initialDelay = 35000L, fixedDelay = 30000L)
    public void leaderElection() {
        if (periscopeNodeConfig.isNodeIdSpecified()) {
            long leaders = periscopeNodeRepository.countByLeaderIsTrueAndLastUpdatedIsGreaterThan(clock.getCurrentTimeMillis() - heartbeatThresholdRate);
            if (leaders == 0L) {
                metricService.gauge(MetricType.LEADER, 0);
                LOGGER.info("There is no active leader available");
                resetTimer();
                try {
                    transactionService.required(() -> {
                        periscopeNodeRepository.deallocateLeader();
                        PeriscopeNode me = periscopeNodeRepository.findById(periscopeNodeConfig.getId())
                                .orElseThrow(notFound("Periscope node", periscopeNodeConfig.getId()));
                        me.setLeader(true);
                        periscopeNodeRepository.save(me);
                        return null;
                    });
                } catch (TransactionExecutionException e) {
                    LOGGER.error("Failed to select node as leader, something went wrong. Message: {}", e.getMessage());
                    return;
                }
                metricService.gauge(MetricType.LEADER, 1);
                LOGGER.info("Selected {} as leader", periscopeNodeConfig.getId());
                timer.schedule(new TimerTask() {
                    @Override
                    public void run() {
                        try {
                            long limit = clock.getCurrentTimeMillis() - heartbeatThresholdRate;
                            List<PeriscopeNode> activeNodes = periscopeNodeRepository.findAllByLastUpdatedIsGreaterThan(limit);
                            if (!activeNodes.isEmpty()) {
                                reallocateOrphanClusters(activeNodes);
                                cleanupInactiveNodesByActiveNodes(activeNodes);
                            }
                        } catch (RuntimeException e) {
                            LOGGER.error("Error happend during fetching cluster allocating them to nodes", e);
                        }
                    }
                }, LEADER_TASK_DELAY, STACK_COLLECTOR_PERIOD);
            }
        }
    }

    private void reallocateOrphanClusters(List<PeriscopeNode> activeNodes) {
        if (activeNodes.stream().noneMatch(n -> n.isLeader() && n.getUuid().equals(periscopeNodeConfig.getId()))) {
            Optional<PeriscopeNode> leader = activeNodes.stream().filter(PeriscopeNode::isLeader).findFirst();
            LOGGER.info("Leader is {}, let's drop leader scope", leader.isPresent() ? leader.get().getUuid() : "-");
            resetTimer();
            return;
        }
        List<String> nodeIds = activeNodes.stream().map(PeriscopeNode::getUuid).collect(Collectors.toList());
        List<Cluster> orphanClusters = clusterRepository.findAllByPeriscopeNodeIdNotInOrPeriscopeNodeIdIsNull(nodeIds);
        if (!orphanClusters.isEmpty()) {
            Iterator<PeriscopeNode> iterator = activeNodes.iterator();
            for (Cluster cluster : orphanClusters) {
                if (!iterator.hasNext()) {
                    iterator = activeNodes.iterator();
                }
                if (isExecutionOfMissedTimeBasedAlertsNeeded(cluster)) {
                    LOGGER.info("Executing missed alerts on cluster {}", cluster.getId());
                    executeMissedTimeBasedAlerts(cluster);
                }
                cluster.setPeriscopeNodeId(iterator.next().getUuid());
                LOGGER.info("Allocationg cluster {} to node {}", cluster.getId(), cluster.getPeriscopeNodeId());
            }
            clusterRepository.saveAll(orphanClusters);
        }
    }

    private boolean isExecutionOfMissedTimeBasedAlertsNeeded(Cluster cluster) {
        long now = clock.getCurrentTimeMillis();
        return cluster.getPeriscopeNodeId() != null
                && cluster.isAutoscalingEnabled()
                && cluster.getLastEvaluated() != 0L
                && now - cluster.getLastScalingActivity() < now - TimeUtil.convertMinToMillisec(cluster.getCoolDown())
                && cluster.getTimeAlerts() != null && !cluster.getTimeAlerts().isEmpty();
    }

    private void executeMissedTimeBasedAlerts(Cluster cluster) {
        Map<TimeAlert, ZonedDateTime> alerts = new LinkedHashMap<>();
        ZonedDateTime now = dateTimeService.getDefaultZonedDateTime();
        long millisDiff = clock.getCurrentTimeMillis() - cluster.getLastEvaluated();
        long coolDown = TimeUtil.convertMinToMillisec(cluster.getCoolDown());
        long rewindMillis = Math.min(millisDiff, coolDown);
        LOGGER.debug("Start rewind for cluster {} at {} - millisDiff: {}, coolDown: {}, rewindMillis: {}",
                cluster.getId(), now, millisDiff, coolDown, rewindMillis);
        for (long r = rewindMillis; r > TimeUtil.SECOND_TO_MILLISEC; r -= TimeUtil.SECOND_TO_MILLISEC) {
            for (TimeAlert alert : cluster.getTimeAlerts()) {
                ZonedDateTime past = dateTimeService.getNextSecound(now.minus(r, ChronoUnit.MILLIS));
                LOGGER.debug("Create alert for cluster {} at {}", cluster.getId(), past);
                alerts.put(alert, past);
            }
        }
        if (!alerts.isEmpty()) {
            CronTimeEvaluator evaluator = applicationContext.getBean("CronTimeEvaluator", CronTimeEvaluator.class);
            evaluator.publishIfNeeded(alerts);
        }
    }

    private void cleanupInactiveNodesByActiveNodes(List<PeriscopeNode> activeNodes) {
        try {
            transactionService.required(() -> {
                periscopeNodeRepository.deleteAllOtherNodes(activeNodes);
                return null;
            });
        } catch (TransactionExecutionException e) {
            LOGGER.error("Unable to delete inactive periscope nodes", e);
        }
    }

    private void resetTimer() {
        timer.cancel();
        timer.purge();
        timer = timerFactory.get();
    }
}
