package com.sequenceiq.flow.core.stats;

import static java.util.stream.Collectors.groupingBy;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Queue;
import java.util.TreeMap;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.google.common.base.Splitter;
import com.google.common.collect.EvictingQueue;
import com.sequenceiq.cloudbreak.common.event.PayloadContext;
import com.sequenceiq.cloudbreak.common.service.TransactionService;
import com.sequenceiq.flow.api.model.FlowProgressResponse;
import com.sequenceiq.flow.api.model.operation.OperationFlowsView;
import com.sequenceiq.flow.api.model.operation.OperationType;
import com.sequenceiq.flow.converter.FlowProgressResponseConverter;
import com.sequenceiq.flow.core.PayloadContextProvider;
import com.sequenceiq.flow.core.cache.FlowStat;
import com.sequenceiq.flow.domain.FlowLog;
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

    private final FlowOperationStatsRepository flowOperationStatsRepository;

    private final TransactionService transactionService;

    private final PayloadContextProvider payloadContextProvider;

    private final FlowProgressResponseConverter flowProgressResponseConverter;

    public FlowOperationStatisticsService(FlowOperationStatsRepository flowOperationStatsRepository, PayloadContextProvider payloadContextProvider,
            TransactionService transactionService, FlowProgressResponseConverter flowProgressResponseConverter) {
        this.flowOperationStatsRepository = flowOperationStatsRepository;
        this.transactionService = transactionService;
        this.payloadContextProvider = payloadContextProvider;
        this.flowProgressResponseConverter = flowProgressResponseConverter;
    }

    public Optional<OperationFlowsView> createOperationResponse(String resourceCrn, List<FlowLog> flowLogs) {
        Double avgProgressTime = 0.0d;
        OperationType operationType;
        if (flowLogs.isEmpty()) {
            operationType = OperationType.UNKNOWN;
        } else {
            FlowLog flowLog = flowLogs.get(0);
            operationType = flowLog.getOperationType();
            if (!operationType.equals(OperationType.UNKNOWN)) {
                PayloadContext payloadContext = payloadContextProvider.getPayloadContext(flowLog.getResourceId());
                avgProgressTime = getExpectedAverageTimeForOperation(operationType, payloadContext.getCloudPlatform());
            }
        }
        Map<String, List<FlowLog>> flowLogsPerType = flowLogs.stream()
                .filter(fl -> fl.getFlowType() != null)
                .collect(groupingBy(fl -> fl.getFlowType().getName()));
        Map<String, FlowProgressResponse> response = new HashMap<>();
        Map<Long, String> createdMap = new TreeMap<>();
        Integer progressFromHistory = DEFAULT_PROGRESS;
        for (Map.Entry<String, List<FlowLog>> entry : flowLogsPerType.entrySet()) {
            FlowProgressResponse progressResponse = flowProgressResponseConverter.convert(entry.getValue(), resourceCrn);
            response.put(entry.getKey(), progressResponse);
            createdMap.put(progressResponse.getCreated(), entry.getKey());
        }
        List<String> typeOrderList = new ArrayList<>(createdMap.values());
        if (!createdMap.isEmpty()) {
            Long operationCreated = createdMap.keySet().stream()
                    .mapToLong(v -> v)
                    .min().getAsLong();
            progressFromHistory = getProgressFromHistory(avgProgressTime, operationCreated);
        }
        if (CollectionUtils.isEmpty(response.entrySet())) {
            LOGGER.debug("Not found any historical flow data for requested resource (crn: {})", resourceCrn);
            return Optional.empty();
        }
        return Optional.of(OperationFlowsView.Builder.newBuilder()
                .withOperationType(operationType)
                .withFlowTypeProgressMap(response)
                .withTypeOrderList(typeOrderList)
                .withProgressFromHistory(progressFromHistory)
                .build());
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
        } catch (TransactionService.TransactionExecutionException e) {
            LOGGER.warn("Cannot store flow operation statistics.", e);
        } catch (Exception e) {
            LOGGER.warn("Unexpected error happened during storing flow operation statistics", e);
        }
    }

    private Integer getProgressFromHistory(Double expectedAvgTime, Long created) {
        if (expectedAvgTime != null && expectedAvgTime != 0.0) {
            Long currentTime = new Date().getTime();
            Double diffTime = getRoundedTimeInSeconds(created, currentTime);
            int percentForAverage = (int) ((diffTime / expectedAvgTime) * DONE_PROGRESS);
            return percentForAverage >= ALMOST_DONE_PROGRESS ? ALMOST_DONE_PROGRESS : percentForAverage;
        } else {
            return DEFAULT_PROGRESS;
        }
    }

    private Double getExpectedAverageTimeForOperation(OperationType operationType, String cloudPlatform) {
        Optional<FlowOperationStats> flowOpStats = flowOperationStatsRepository.findFirstByOperationTypeAndCloudPlatform(operationType, cloudPlatform);
        if (flowOpStats.isPresent()) {
            FlowOperationStats flowOperationStats = flowOpStats.get();
            String durationHistory = flowOperationStats.getDurationHistory();
            if (StringUtils.isNotBlank(durationHistory)) {
                return Splitter.on(",").splitToList(durationHistory).stream().mapToDouble(Double::parseDouble).average().orElse(0.0);
            }
        }
        return DEFAULT_GUESSED_AVG_TIME_SEC;
    }

    private Double getRoundedTimeInSeconds(Long from, Long to) {
        return Double.valueOf(new DecimalFormat("#.###").format((to - from) / MILLISEC_TO_SEC_DOUBLE_DEVIDER));
    }
}
