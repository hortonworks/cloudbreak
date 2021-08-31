package com.sequenceiq.flow.core.cache;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.event.PayloadContext;
import com.sequenceiq.flow.api.model.operation.OperationFlowsView;
import com.sequenceiq.flow.api.model.operation.OperationType;
import com.sequenceiq.flow.core.PayloadContextProvider;
import com.sequenceiq.flow.core.config.FlowConfiguration;
import com.sequenceiq.flow.core.stats.FlowOperationStatisticsService;

@Component
public class FlowStatCache {

    private static final Logger LOGGER = LoggerFactory.getLogger(FlowStatCache.class);

    private static final int ONE_DAY = 1;

    private final Map<String, FlowStat> flowIdStatCache = new ConcurrentHashMap<>();

    private final Map<String, FlowStat> flowChainIdStatCache = new ConcurrentHashMap<>();

    private final Map<String, FlowStat> resourceCrnFlowStatCache = new ConcurrentHashMap<>();

    private final Map<String, FlowStat> resourceCrnFlowChainStatCache = new ConcurrentHashMap<>();

    private final PayloadContextProvider payloadContextProvider;

    private final FlowOperationStatisticsService flowOperationStatisticsService;

    public FlowStatCache(FlowOperationStatisticsService flowOperationStatisticsService, PayloadContextProvider payloadContextProvider) {
        this.flowOperationStatisticsService = flowOperationStatisticsService;
        this.payloadContextProvider = payloadContextProvider;
    }

    public void put(String flowId, String flowChainId, Long resourceId,
            String operationType, Class<? extends FlowConfiguration<?>> flowConfigType, boolean restored) {
        PayloadContext payloadContext = payloadContextProvider.getPayloadContext(resourceId);
        if (payloadContext != null) {
            FlowStat flowStat = new FlowStat();
            flowStat.setFlowId(flowId);
            flowStat.setFlowChainId(flowChainId);
            flowStat.setResourceId(resourceId);
            flowStat.setStartTime(new Date().getTime());
            flowStat.setFlowType(flowConfigType);
            flowStat.setOperationType(OperationType.valueOf(operationType));
            flowStat.setPayloadContext(payloadContext);
            flowStat.setRestored(restored);
            flowIdStatCache.put(flowId, flowStat);
            if (StringUtils.isNotBlank(payloadContext.getEnvironmentCrn())) {
                resourceCrnFlowStatCache.put(payloadContext.getEnvironmentCrn(), flowStat);
            }
            resourceCrnFlowStatCache.put(payloadContext.getResourceCrn(), flowStat);
        }
    }

    public void putByFlowChainId(String flowChainId, Long resourceId, String operationType, boolean restored) {
        if (!flowChainIdStatCache.containsKey(flowChainId)) {
            PayloadContext payloadContext = payloadContextProvider.getPayloadContext(resourceId);
            if (payloadContext != null) {
                FlowStat flowStat = new FlowStat();
                flowStat.setFlowChainId(flowChainId);
                flowStat.setResourceId(resourceId);
                flowStat.setStartTime(new Date().getTime());
                flowStat.setOperationType(OperationType.valueOf(operationType));
                flowStat.setPayloadContext(payloadContext);
                flowStat.setRestored(restored);
                flowChainIdStatCache.put(flowChainId, flowStat);
                if (StringUtils.isNotBlank(payloadContext.getEnvironmentCrn())) {
                    resourceCrnFlowChainStatCache.put(payloadContext.getEnvironmentCrn(), flowStat);
                }
                resourceCrnFlowChainStatCache.put(payloadContext.getResourceCrn(), flowStat);
            }
        }
    }

    public void remove(String flowId, boolean store) {
        if (flowIdStatCache.containsKey(flowId)) {
            FlowStat flowStat = flowIdStatCache.get(flowId);
            if (store && StringUtils.isBlank(flowStat.getFlowChainId())) {
                flowOperationStatisticsService.save(flowStat);
            }
            PayloadContext payloadContext = flowStat.getPayloadContext();
            resourceCrnFlowStatCache.remove(payloadContext.getResourceCrn());
            if (StringUtils.isNotBlank(payloadContext.getEnvironmentCrn())) {
                resourceCrnFlowStatCache.remove(payloadContext.getEnvironmentCrn());
            }
            flowIdStatCache.remove(flowId);
        }
    }

    public void removeByFlowChainId(String flowChainId, boolean store) {
        if (flowChainIdStatCache.containsKey(flowChainId)) {
            FlowStat flowStat = flowChainIdStatCache.get(flowChainId);
            if (store) {
                flowOperationStatisticsService.save(flowStat);
            }
            PayloadContext payloadContext = flowStat.getPayloadContext();
            resourceCrnFlowChainStatCache.remove(payloadContext.getResourceCrn());
            if (StringUtils.isNotBlank(payloadContext.getEnvironmentCrn())) {
                resourceCrnFlowChainStatCache.remove(payloadContext.getEnvironmentCrn());
            }
            flowChainIdStatCache.remove(flowChainId);
        }
    }

    public FlowStat getFlowStatByFlowId(String flowId) {
        return flowIdStatCache.get(flowId);
    }

    public FlowStat getFlowStatByFlowChainId(String flowChainId) {
        return flowChainIdStatCache.get(flowChainId);
    }

    public FlowStat getFlowStatByResourceCrn(String resourceCrn) {
        return resourceCrnFlowStatCache.get(resourceCrn);
    }

    public FlowStat getFlowChainStatByResourceCrn(String resourceCrn) {
        return resourceCrnFlowChainStatCache.get(resourceCrn);
    }

    public Map<String, FlowStat> getFlowIdStatCache() {
        return flowIdStatCache;
    }

    public Map<String, FlowStat> getFlowChainIdStatCache() {
        return flowChainIdStatCache;
    }

    public Map<String, FlowStat> getResourceCrnFlowStatCache() {
        return resourceCrnFlowStatCache;
    }

    public Map<String, FlowStat> getResourceCrnFlowChainStatCache() {
        return resourceCrnFlowChainStatCache;
    }

    public Optional<OperationFlowsView> getOperationFlowByResourceCrn(String resourceCrn) {
        if (getFlowChainStatByResourceCrn(resourceCrn) != null) {
            FlowStat flowStat = getFlowChainStatByResourceCrn(resourceCrn);
            if (OperationType.UNKNOWN.equals(flowStat.getOperationType())) {
                return Optional.empty();
            }
            Integer progressFromHistory = flowOperationStatisticsService.getProgressFromHistory(flowStat);
            return Optional.of(OperationFlowsView.Builder.newBuilder()
                    .withOperationType(flowStat.getOperationType())
                    .withInMemory(true)
                    .withProgressFromHistory(progressFromHistory)
                    .withOperationId(flowStat.getFlowChainId())
                    .build());
        } else if (getFlowStatByResourceCrn(resourceCrn) != null) {
            FlowStat flowStat = getFlowStatByResourceCrn(resourceCrn);
            if (OperationType.UNKNOWN.equals(flowStat.getOperationType())) {
                return Optional.empty();
            }
            Integer progressFromHistory = flowOperationStatisticsService.getProgressFromHistory(flowStat);
            return Optional.of(OperationFlowsView.Builder.newBuilder()
                    .withOperationType(flowStat.getOperationType())
                    .withInMemory(true)
                    .withProgressFromHistory(progressFromHistory)
                    .withOperationId(flowStat.getFlowId())
                    .build());
        }
        return Optional.empty();
    }

    public void cleanOldCacheEntries(Set<String> runningFlowIds) {
        LOGGER.debug("Executing flow stat cache cleanup. (In case of any error during flow finalization " +
                "- cache entries remained inside the stat cache)");
        List<FlowStat> oldFlowStatsByFlowId = flowIdStatCache.values().stream().filter(this::isStartTimeTooOld).collect(Collectors.toList());
        oldFlowStatsByFlowId.forEach(fs -> {
            if (!runningFlowIds.contains(fs.getFlowId())) {
                flowIdStatCache.remove(fs.getFlowId());
            }
        });
        List<FlowStat> oldFlowStatsByFlowChainId = flowChainIdStatCache.values().stream().filter(this::isStartTimeTooOld).collect(Collectors.toList());
        oldFlowStatsByFlowChainId.forEach(fs -> flowChainIdStatCache.remove(fs.getFlowChainId()));
        removeOldResourceCrnBasedFlowStat(resourceCrnFlowStatCache);
        removeOldResourceCrnBasedFlowStat(resourceCrnFlowChainStatCache);
        LOGGER.debug("Old flow stat cache entry cleanup finished");
    }

    private void removeOldResourceCrnBasedFlowStat(Map<String, FlowStat> resourceCrnBasedMap) {
        List<FlowStat> oldFlowStatsByResourceCrnForFlowChains = resourceCrnBasedMap.values().stream()
                .filter(this::isStartTimeTooOld).collect(Collectors.toList());
        oldFlowStatsByResourceCrnForFlowChains.forEach(fs -> {
            String resourceCrn = fs.getPayloadContext().getResourceCrn();
            LOGGER.debug("Remove old cache entry from flow stat cache with crn {}", resourceCrn);
            resourceCrnBasedMap.remove(resourceCrn);
            String environmentCrn = fs.getPayloadContext().getEnvironmentCrn();
            if (StringUtils.isNotBlank(environmentCrn) && resourceCrnBasedMap.containsKey(environmentCrn)
                    && isStartTimeTooOld(resourceCrnBasedMap.get(environmentCrn))) {
                LOGGER.debug("Remove old cache entry from flow stat cache with environment crn {}", environmentCrn);
                resourceCrnBasedMap.remove(environmentCrn);
            }
        });
    }

    private boolean isStartTimeTooOld(FlowStat flowStat) {
        Instant beforeTime = Instant.now().minus(ONE_DAY, ChronoUnit.DAYS);
        Instant startTime = new Date(flowStat.getStartTime()).toInstant();
        return startTime.isBefore(beforeTime);
    }
}
