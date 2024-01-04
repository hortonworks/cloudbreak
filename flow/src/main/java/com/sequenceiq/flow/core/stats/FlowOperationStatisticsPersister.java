package com.sequenceiq.flow.core.stats;

import java.text.DecimalFormat;
import java.util.Date;
import java.util.Optional;
import java.util.Queue;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.google.common.base.Splitter;
import com.google.common.collect.EvictingQueue;
import com.sequenceiq.cloudbreak.common.service.TransactionService;
import com.sequenceiq.flow.api.model.operation.OperationType;
import com.sequenceiq.flow.core.cache.FlowStat;
import com.sequenceiq.flow.domain.FlowOperationStats;
import com.sequenceiq.flow.repository.FlowOperationStatsRepository;

@Service
public class FlowOperationStatisticsPersister {

    private static final Logger LOGGER = LoggerFactory.getLogger(FlowOperationStatisticsPersister.class);

    private static final int MAX_FLOW_STAT_SIZE = 10;

    private static final double MILLISEC_TO_SEC_DOUBLE_DEVIDER = 1000d;

    @Inject
    private FlowOperationStatsRepository flowOperationStatsRepository;

    @Inject
    private TransactionService transactionService;

    public synchronized void save(FlowStat flowStat) {
        if (!OperationType.UNKNOWN.equals(flowStat.getOperationType()) && flowStat.getPayloadContext() != null) {
            if (flowStat.isRestored()) {
                LOGGER.debug("Flow was restored, so statistics won't be saved about that. (operation: {})", flowStat.getOperationType());
            } else {
                try {
                    FlowOperationStats flowOperationStats = fetchOrCreateFlowOperationStats(flowStat);
                    String durationHistory = calculateDurationHistory(flowStat, flowOperationStats);
                    flowOperationStats.setDurationHistory(durationHistory);
                    transactionService.required(() -> flowOperationStatsRepository.save(flowOperationStats));
                } catch (TransactionService.TransactionExecutionException e) {
                    LOGGER.warn("Cannot store flow operation statistics.", e);
                } catch (Exception e) {
                    LOGGER.warn("Unexpected error happened during storing flow operation statistics", e);
                }
            }
        }
    }

    private FlowOperationStats fetchOrCreateFlowOperationStats(FlowStat flowStat) {
        String cloudplatform = flowStat.getPayloadContext().getCloudPlatform();
        Optional<FlowOperationStats> flowOpStatOpt = flowOperationStatsRepository.findFirstByOperationTypeAndCloudPlatform(
                flowStat.getOperationType(), cloudplatform);
        if (flowOpStatOpt.isPresent()) {
            return flowOpStatOpt.get();
        } else {
            FlowOperationStats flowOperationStats = new FlowOperationStats();
            flowOperationStats.setOperationType(flowStat.getOperationType());
            flowOperationStats.setCloudPlatform(cloudplatform);
            return flowOperationStats;
        }
    }

    private String calculateDurationHistory(FlowStat flowStat, FlowOperationStats flowOperationStats) {
        Queue<Double> durationHistoryQueue = EvictingQueue.create(MAX_FLOW_STAT_SIZE);
        if (StringUtils.isNotBlank(flowOperationStats.getDurationHistory())) {
            Splitter.on(",").splitToList(flowOperationStats.getDurationHistory())
                    .forEach(s -> durationHistoryQueue.add(Double.parseDouble(s)));
        }
        Double elapsedOperationTime = getRoundedTimeInSeconds(flowStat.getStartTime(), new Date().getTime());
        durationHistoryQueue.add(elapsedOperationTime);
        return StringUtils.join(durationHistoryQueue.stream()
                .map(Object::toString).collect(Collectors.toList()), ",");
    }

    private Double getRoundedTimeInSeconds(Long from, Long to) {
        return Double.valueOf(new DecimalFormat("#.###").format((to - from) / MILLISEC_TO_SEC_DOUBLE_DEVIDER));
    }
}
