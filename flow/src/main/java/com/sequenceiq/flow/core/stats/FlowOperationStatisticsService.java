package com.sequenceiq.flow.core.stats;

import java.text.DecimalFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Queue;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.google.common.base.Splitter;
import com.google.common.collect.EvictingQueue;
import com.sequenceiq.cloudbreak.common.event.PayloadContext;
import com.sequenceiq.cloudbreak.common.service.TransactionService;
import com.sequenceiq.flow.api.model.operation.OperationType;
import com.sequenceiq.flow.core.cache.FlowStat;
import com.sequenceiq.flow.domain.FlowOperationStats;
import com.sequenceiq.flow.repository.FlowOperationStatsRepository;

@Component
public class FlowOperationStatisticsService {

    private static final Logger LOGGER = LoggerFactory.getLogger(FlowOperationStatisticsService.class);

    private static final double MILLISEC_TO_SEC_DOUBLE_DEVIDER = 1000d;

    private static final double DEFAULT_GUESSED_AVG_TIME_SEC = 720d;

    private static final Integer ALMOST_DONE_PROGRESS = 99;

    private static final Integer DONE_PROGRESS = 100;

    private static final Integer DEFAULT_PROGRESS = -1;

    private static final Integer MAX_FLOW_STAT_SIZE = 10;

    private final Map<String, Double> cloudOperationAverageTimeMap = new HashMap<>();

    private final FlowOperationStatsRepository flowOperationStatsRepository;

    private final TransactionService transactionService;

    public FlowOperationStatisticsService(FlowOperationStatsRepository flowOperationStatsRepository, TransactionService transactionService) {
        this.flowOperationStatsRepository = flowOperationStatsRepository;
        this.transactionService = transactionService;
    }

    public Double getExpectedAverageTimeForOperation(OperationType operationType, String cloudPlatform) {
        return cloudOperationAverageTimeMap.getOrDefault(createOperationCloudPlatformKey(operationType, cloudPlatform), DEFAULT_GUESSED_AVG_TIME_SEC);
    }

    public synchronized void updateOperationAverageTime(OperationType operationType, String cloudPlatform, String durationHistory) {
        if (StringUtils.isNotBlank(durationHistory)) {
            Double newExpectedAvgTime = Splitter.on(",").splitToList(durationHistory).stream()
                    .mapToDouble(Double::parseDouble).average().orElse(0.0);
            cloudOperationAverageTimeMap.put(createOperationCloudPlatformKey(operationType, cloudPlatform), newExpectedAvgTime);
        }
    }

    @PostConstruct
    public void load() {
        LOGGER.debug("Load all flow statistics ...");
        flowOperationStatsRepository.findAll()
                .forEach(flowOpStat ->
                        updateOperationAverageTime(flowOpStat.getOperationType(), flowOpStat.getCloudPlatform(), flowOpStat.getDurationHistory())
                );
        LOGGER.debug("Flow statistics map size: {}", cloudOperationAverageTimeMap.size());
    }

    public synchronized void save(FlowStat flowStat) {
        if (OperationType.UNKNOWN.equals(flowStat.getOperationType()) || flowStat.getPayloadContext() == null) {
            return;
        }
        if (flowStat.isRestored()) {
            LOGGER.debug("Flow was restored, so statistics won't be saved about that. (operation: {})", flowStat.getOperationType());
            return;
        }
        try {
            OperationType operationType = flowStat.getOperationType();
            PayloadContext payloadContext = flowStat.getPayloadContext();
            Optional<FlowOperationStats> flowOpStatOpt = flowOperationStatsRepository.findFirstByOperationTypeAndCloudPlatform(
                    operationType, payloadContext.getCloudPlatform());
            final FlowOperationStats flowOperationStats;
            if (flowOpStatOpt.isPresent()) {
                flowOperationStats = flowOpStatOpt.get();
            } else {
                flowOperationStats = new FlowOperationStats();
                flowOperationStats.setOperationType(operationType);
                flowOperationStats.setCloudPlatform(payloadContext.getCloudPlatform());
            }
            String durationHistory = flowOperationStats.getDurationHistory();
            Queue<Double> durationHistoryQueue = EvictingQueue.create(MAX_FLOW_STAT_SIZE);
            if (StringUtils.isNotBlank(durationHistory)) {
                Splitter.on(",").splitToList(durationHistory)
                        .forEach(
                                s -> {
                                    durationHistoryQueue.add(Double.parseDouble(s));
                                }
                        );
            }
            Double elapsedOperationTime = getRoundedTimeInSeconds(flowStat.getStartTime(), new Date().getTime());
            durationHistoryQueue.add(elapsedOperationTime);
            durationHistory = StringUtils.join(durationHistoryQueue.stream()
                    .map(Object::toString).collect(Collectors.toList()), ",");
            flowOperationStats.setDurationHistory(durationHistory);
            transactionService.required(() -> flowOperationStatsRepository.save(flowOperationStats));
            updateOperationAverageTime(flowOperationStats.getOperationType(), flowOperationStats.getCloudPlatform(), flowOperationStats.getDurationHistory());
        } catch (TransactionService.TransactionExecutionException e) {
            LOGGER.warn("Cannot store flow operation statistics.", e);
        } catch (Exception e) {
            LOGGER.warn("Unexpected error happened during storing flow operation statistics", e);
        }
    }

    public Integer getProgressFromHistory(FlowStat flowStat) {
        if (flowStat != null && flowStat.getPayloadContext() != null && flowStat.getOperationType() != null) {
            Double expectedAvgTime = getExpectedAverageTimeForOperation(flowStat.getOperationType(), flowStat.getPayloadContext().getCloudPlatform());
            if (expectedAvgTime != null && expectedAvgTime != 0.0) {
                Long currentTime = new Date().getTime();
                Long startTime = flowStat.getStartTime();
                Double diffTime = getRoundedTimeInSeconds(startTime, currentTime);
                int percentForAverage = (int) ((diffTime / expectedAvgTime) * DONE_PROGRESS);
                return percentForAverage >= ALMOST_DONE_PROGRESS ? ALMOST_DONE_PROGRESS : percentForAverage;
            }
        }
        return DEFAULT_PROGRESS;
    }

    private Double getRoundedTimeInSeconds(Long from, Long to) {
        return Double.valueOf(new DecimalFormat("#.###").format((to - from) / MILLISEC_TO_SEC_DOUBLE_DEVIDER));
    }

    private String createOperationCloudPlatformKey(OperationType operationType, String cloudPlatform) {
        return String.format("%s_%s", operationType.name(), cloudPlatform.toUpperCase());
    }
}
