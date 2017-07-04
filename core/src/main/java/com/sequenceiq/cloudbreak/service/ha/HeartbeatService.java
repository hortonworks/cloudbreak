package com.sequenceiq.cloudbreak.service.ha;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.transaction.Transactional;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.google.api.client.util.Lists;
import com.sequenceiq.cloudbreak.core.flow2.Flow2Handler;
import com.sequenceiq.cloudbreak.core.flow2.FlowRegister;
import com.sequenceiq.cloudbreak.domain.CloudbreakNode;
import com.sequenceiq.cloudbreak.domain.FlowLog;
import com.sequenceiq.cloudbreak.repository.CloudbreakNodeRepository;
import com.sequenceiq.cloudbreak.repository.FlowLogRepository;
import com.sequenceiq.cloudbreak.service.Clock;

@Service
public class HeartbeatService {

    private static final Logger LOGGER = LoggerFactory.getLogger(HeartbeatService.class);

    private static final int HEARTBEAT_RATE = 30_000;

    private static final int FLOW_RESTART_RATE = 10_000;

    private static final int FLOW_DISTRIBUTION_RATE = 35_000;

    private static final int HEARTBEAT_THRESHOLD_RATE = 2 * HEARTBEAT_RATE;

    @Value("${cb.instance.uuid:}")
    private String uuid;

    @Inject
    private CloudbreakNodeRepository cloudbreakNodeRepository;

    @Inject
    private FlowLogRepository flowLogRepository;

    @Inject
    private Flow2Handler flow2Handler;

    @Inject
    private Clock clock;

    @Inject
    private FlowDistributor flowDistributor;

    @Inject
    private FlowRegister runningFlows;

    @Scheduled(fixedRate = HEARTBEAT_RATE)
    public void heartbeat() {
        if (StringUtils.isNoneBlank(uuid)) {
            CloudbreakNode self = cloudbreakNodeRepository.findOne(uuid);
            if (self == null) {
                self = new CloudbreakNode(uuid);
            }
            self.setLastUpdated(clock.getCurrentTime());
            cloudbreakNodeRepository.save(self);
        }
    }

    @Scheduled(fixedRate = FLOW_DISTRIBUTION_RATE, initialDelay = HEARTBEAT_RATE)
    public void scheduledFlowDistribution() {
        try {
            distributeFlows();
        } catch (OptimisticLockingFailureException e) {
            LOGGER.error("Failed to distribute the flowLogs across the active nodes", e);
        }

        Set<String> allMyFlows = flowLogRepository.findAllByCloudbreakNodeId(uuid).stream().map(FlowLog::getFlowId).distinct().collect(Collectors.toSet());
        Set<String> newFlows = allMyFlows.stream().filter(f -> runningFlows.get(f) == null).collect(Collectors.toSet());
        for (String flow : newFlows) {
            try {
                flow2Handler.restartFlow(flow);
            } catch (Exception e) {
                LOGGER.error(String.format("Failed to restart flow: %s", flow), e);
            }
        }
    }

    @Transactional
    public void distributeFlows() {
        if (StringUtils.isNoneBlank(uuid)) {
            List<CloudbreakNode> cloudbreakNodes = Lists.newArrayList(cloudbreakNodeRepository.findAll());
            long currentTimeMillis = clock.getCurrentTime();
            List<CloudbreakNode> failedNodes = cloudbreakNodes.stream()
                    .filter(cb -> currentTimeMillis - cb.getLastUpdated() > HEARTBEAT_THRESHOLD_RATE).collect(Collectors.toList());
            List<CloudbreakNode> activeNodes = cloudbreakNodes.stream().filter(c -> !failedNodes.contains(c)).collect(Collectors.toList());
            LOGGER.info("Active CB nodes: ({})[{}], failed CB nodes: ({})[{}]", activeNodes.size(), activeNodes, failedNodes.size(), failedNodes);

            List<FlowLog> flowLogs = failedNodes.stream()
                    .map(node -> flowLogRepository.findAllByCloudbreakNodeId(node.getUuid()))
                    .flatMap(Set::stream)
                    .collect(Collectors.toList());

            if (!flowLogs.isEmpty()) {
                List<String> flowIds = flowLogs.stream().map(FlowLog::getFlowId).distinct().collect(Collectors.toList());
                Map<CloudbreakNode, List<String>> flowDistribution = flowDistributor.distribute(flowIds, activeNodes);
                List<FlowLog> updatedFlowLogs = new ArrayList<>();
                for (CloudbreakNode node : flowDistribution.keySet()) {
                    flowDistribution.get(node).forEach(flowId ->
                            flowLogs.stream().filter(flowLog -> flowLog.getFlowId().equalsIgnoreCase(flowId)).forEach(flowLog -> {
                                flowLog.setCloudbreakNodeId(node.getUuid());
                                updatedFlowLogs.add(flowLog);
                            }));
                }
                flowLogRepository.save(updatedFlowLogs);
            }
        }
    }

}
