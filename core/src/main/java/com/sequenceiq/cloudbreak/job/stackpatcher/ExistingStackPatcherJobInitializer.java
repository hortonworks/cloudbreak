package com.sequenceiq.cloudbreak.job.stackpatcher;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.google.common.annotations.VisibleForTesting;
import com.sequenceiq.cloudbreak.domain.stack.StackPatch;
import com.sequenceiq.cloudbreak.domain.stack.StackPatchType;
import com.sequenceiq.cloudbreak.domain.stack.StackPatchTypeStatus;
import com.sequenceiq.cloudbreak.job.AbstractStackJobInitializer;
import com.sequenceiq.cloudbreak.job.stackpatcher.config.ExistingStackPatcherConfig;
import com.sequenceiq.cloudbreak.quartz.model.JobResource;
import com.sequenceiq.cloudbreak.service.stackpatch.StackPatchService;
import com.sequenceiq.cloudbreak.util.RandomUtil;

@Component
public class ExistingStackPatcherJobInitializer extends AbstractStackJobInitializer {

    private static final Logger LOGGER = LoggerFactory.getLogger(ExistingStackPatcherJobInitializer.class);

    @VisibleForTesting
    Supplier<LocalDateTime> nowSupplier = LocalDateTime::now;

    @VisibleForTesting
    Function<Long, Long> randomLongProvider = RandomUtil::getLong;

    @Inject
    private ExistingStackPatcherConfig config;

    @Inject
    private ExistingStackPatcherJobService jobService;

    @Inject
    private StackPatchService stackPatchService;

    @Override
    public void initJobs() {
        Set<StackPatchType> enabledStackPatchTypes = getEnabledStackPatchTypes();
        LOGGER.info("Existing stack patch types enabled: {}", enabledStackPatchTypes);

        Set<StackPatchType> deprecatedStackPatchTypes = getDeprecatedStackPatchTypes();
        LOGGER.info("Existing stack patch types deprecated: {}", deprecatedStackPatchTypes);

        Map<Long, JobResource> stacksById = getAliveJobResources().stream()
                .collect(Collectors.toMap(jobResource -> Long.valueOf(jobResource.getLocalId()), Function.identity()));

        for (int i = 0; i < stacksById.size(); i += config.getInitializationChunkSize()) {
            LOGGER.debug("Scheduling stack patchers for chunk {}", i / config.getInitializationChunkSize());
            Set<Long> stackIdsChunk = stacksById.entrySet().stream()
                    .skip(i)
                    .limit(config.getInitializationChunkSize())
                    .map(Map.Entry::getKey)
                    .collect(Collectors.toSet());
            scheduleForStackChunk(stackIdsChunk, enabledStackPatchTypes, stacksById);
        }

        List<StackPatch> deprecatedStackPatches = stackPatchService.findAllByTypes(deprecatedStackPatchTypes);
        stackPatchService.deleteAll(deprecatedStackPatches);
    }

    private void scheduleForStackChunk(
            Set<Long> stackIdsChunk,
            Set<StackPatchType> enabledStackPatchTypes,
            Map<Long, JobResource> stacksById
    ) {
        Map<StackPatchType, Map<Long, List<StackPatch>>> stackPatchesByStackPatchTypeAndStack = new EnumMap<>(StackPatchType.class);

        for (StackPatchType stackPatchType : enabledStackPatchTypes) {
            Map<Long, List<StackPatch>> stackPatchesByStack = stackPatchService.findAllByTypeForStackIds(stackPatchType, stackIdsChunk).stream()
                    .collect(Collectors.groupingBy(StackPatch::getStackId));
            stackPatchesByStackPatchTypeAndStack.put(stackPatchType, stackPatchesByStack);
        }

        for (Long stackId : stackIdsChunk) {
            scheduleForStack(stacksById.get(stackId), enabledStackPatchTypes, stackPatchesByStackPatchTypeAndStack);
        }
    }

    private void scheduleForStack(
            JobResource stack,
            Set<StackPatchType> enabledStackPatchTypes,
            Map<StackPatchType, Map<Long, List<StackPatch>>> stackPatchesByStackPatchTypeAndStack
    ) {
        Long stackId = Long.valueOf(stack.getLocalId());
        LOGGER.debug("Scheduling stack patchers for stackId: {}", stackId);
        LocalDateTime now = nowSupplier.get();
        long maxInitialStartDelay = TimeUnit.HOURS.toMinutes(config.getMaxInitialStartDelayInHours());

        long initialStartDelay = randomLongProvider.apply(maxInitialStartDelay);
        LocalDateTime jobFirstStart = now.plusMinutes(initialStartDelay);

        for (StackPatchType stackPatchType : enabledStackPatchTypes) {
            boolean stackPatchIsFinalized = stackPatchesByStackPatchTypeAndStack
                    .getOrDefault(stackPatchType, Map.of())
                    .getOrDefault(stackId, List.of())
                    .stream()
                    .anyMatch(stackPatch -> stackPatch.getStatus().isFinal());
            if (!stackPatchIsFinalized) {
                try {
                    LOGGER.debug("Scheduling stack patcher {} for stack {} with first start {}", stackPatchType, stackId, jobFirstStart);
                    ExistingStackPatcherJobAdapter jobAdapter = new ExistingStackPatcherJobAdapter(stack, stackPatchType);
                    stackPatchService.getOrCreate(stackId, stackPatchType);
                    jobService.schedule(jobAdapter, jobFirstStart);

                    jobFirstStart = calculateNextJobFirstStart(jobFirstStart, now, maxInitialStartDelay, enabledStackPatchTypes.size());
                } catch (Exception e) {
                    LOGGER.error("Failed to schedule stack patcher {} for stack {} with first start {}", stackPatchType, stackId, jobFirstStart, e);
                }
            }
        }
    }

    private LocalDateTime calculateNextJobFirstStart(
            LocalDateTime currentJobFirstStart,
            LocalDateTime now,
            long maxInitialStartDelay,
            long maxJobCount
    ) {
        long minutesBetweenFirstStarts = maxInitialStartDelay / maxJobCount;
        LocalDateTime nextJobFirstStart = currentJobFirstStart.plusMinutes(minutesBetweenFirstStarts);

        LocalDateTime latestFirstStart = now.plusMinutes(maxInitialStartDelay);
        if (nextJobFirstStart.isAfter(latestFirstStart)) {
            long overflow = ChronoUnit.MINUTES.between(latestFirstStart, nextJobFirstStart);
            nextJobFirstStart = now.plusMinutes(overflow);
        }
        return nextJobFirstStart;
    }

    private Set<StackPatchType> getEnabledStackPatchTypes() {
        return config.getPatchConfigs().entrySet().stream()
                .filter(entry -> entry.getValue().isEnabled())
                .map(Map.Entry::getKey)
                .filter(stackPatchType -> !StackPatchTypeStatus.DEPRECATED.equals(stackPatchType.getStatus()))
                .collect(Collectors.toSet());
    }

    private Set<StackPatchType> getDeprecatedStackPatchTypes() {
        return config.getPatchConfigs().entrySet().stream()
                .filter(entry -> !entry.getValue().isEnabled())
                .map(Map.Entry::getKey)
                .filter(stackPatchType -> StackPatchTypeStatus.DEPRECATED.equals(stackPatchType.getStatus()))
                .collect(Collectors.toSet());
    }
}
