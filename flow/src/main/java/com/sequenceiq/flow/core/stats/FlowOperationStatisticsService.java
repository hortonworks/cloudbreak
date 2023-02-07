package com.sequenceiq.flow.core.stats;

import static com.google.common.base.Preconditions.checkState;
import static java.util.stream.Collectors.groupingBy;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.google.common.base.Splitter;
import com.sequenceiq.cloudbreak.auth.crn.Crn;
import com.sequenceiq.cloudbreak.common.event.PayloadContext;
import com.sequenceiq.flow.api.model.FlowProgressResponse;
import com.sequenceiq.flow.api.model.operation.OperationFlowsView;
import com.sequenceiq.flow.api.model.operation.OperationType;
import com.sequenceiq.flow.converter.FlowProgressResponseConverter;
import com.sequenceiq.flow.core.PayloadContextProvider;
import com.sequenceiq.flow.domain.FlowLog;
import com.sequenceiq.flow.domain.FlowOperationStats;
import com.sequenceiq.flow.repository.FlowOperationStatsRepository;
import com.sequenceiq.flow.service.flowlog.FlowLogDBService;

@Component
public class FlowOperationStatisticsService {

    private static final Logger LOGGER = LoggerFactory.getLogger(FlowOperationStatisticsService.class);

    private static final double MILLISEC_TO_SEC_DOUBLE_DEVIDER = 1000d;

    private static final double DEFAULT_GUESSED_AVG_TIME_SEC = 720d;

    private static final Integer ALMOST_DONE_PROGRESS = 99;

    private static final Integer DONE_PROGRESS = 100;

    private static final Integer DEFAULT_PROGRESS = -1;

    private final FlowOperationStatsRepository flowOperationStatsRepository;

    private final PayloadContextProvider payloadContextProvider;

    private final FlowProgressResponseConverter flowProgressResponseConverter;

    private final FlowLogDBService flowLogDBService;

    public FlowOperationStatisticsService(FlowOperationStatsRepository flowOperationStatsRepository, PayloadContextProvider payloadContextProvider,
            FlowProgressResponseConverter flowProgressResponseConverter, FlowLogDBService flowLogDBService) {
        this.flowOperationStatsRepository = flowOperationStatsRepository;
        this.payloadContextProvider = payloadContextProvider;
        this.flowProgressResponseConverter = flowProgressResponseConverter;
        this.flowLogDBService = flowLogDBService;
    }

    public Optional<OperationFlowsView> getLastFlowOperationByResourceCrn(String resourceCrn) {
        checkState(Crn.isCrn(resourceCrn));
        List<FlowLog> flowLogs = flowLogDBService.getLatestFlowLogsByCrnInFlowChain(resourceCrn);
        return createOperationResponse(resourceCrn, flowLogs);
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
