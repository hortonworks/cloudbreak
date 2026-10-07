package com.sequenceiq.stackpatcher.job;

import static com.sequenceiq.cloudbreak.util.Benchmark.checkedMeasure;

import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.quartz.model.JobInitializer;
import com.sequenceiq.cloudbreak.quartz.model.JobResource;
import com.sequenceiq.cloudbreak.util.RandomUtil;
import com.sequenceiq.stackpatcher.config.ExistingStackPatcherConfig;
import com.sequenceiq.stackpatcher.config.StackPatchTypeConfig;
import com.sequenceiq.stackpatcher.domain.StackPatch;
import com.sequenceiq.stackpatcher.domain.StackPatchType;
import com.sequenceiq.stackpatcher.domain.StackPatchTypeStatus;
import com.sequenceiq.stackpatcher.service.ExistingStackPatchService;
import com.sequenceiq.stackpatcher.service.StackPatchResourceService;
import com.sequenceiq.stackpatcher.service.StackPatchService;

@Component
public class ExistingStackPatcherJobInitializer implements JobInitializer {

    private static final Logger LOGGER = LoggerFactory.getLogger(ExistingStackPatcherJobInitializer.class);

    private static final StackPatchTypeConfig DISABLED = new StackPatchTypeConfig();

    private Supplier<ZonedDateTime> nowSupplier = ZonedDateTime::now;

    private Function<Long, Long> randomLongProvider = RandomUtil::getLong;

    @Inject
    private ExistingStackPatcherConfig config;

    @Inject
    private ExistingStackPatcherJobService jobService;

    @Inject
    private StackPatchService stackPatchService;

    @Inject
    private StackPatchResourceService<?> stackPatchResourceService;

    @Inject
    private List<ExistingStackPatchService<?>> existingStackPatchServices;

    @Override
    public void initJobs() {
        Set<StackPatchType> enabledStackPatchTypes = getEnabledStackPatchTypes();
        LOGGER.info("Existing stack patch types enabled: {}", enabledStackPatchTypes);

        Set<StackPatchType> deprecatedStackPatchTypes = getDeprecatedStackPatchTypes();
        LOGGER.info("Existing stack patch types deprecated: {}", deprecatedStackPatchTypes);

        Map<Long, JobResource> resourcesById = checkedMeasure(() -> stackPatchResourceService.findAllSchedulableResources(), LOGGER,
                ":::Stack patcher::: Stacks are fetched from db in {}ms").stream()
                .collect(Collectors.toMap(jobResource -> Long.valueOf(jobResource.getLocalId()), Function.identity()));

        for (int i = 0; i < resourcesById.size(); i += config.getInitializationChunkSize()) {
            LOGGER.debug("Scheduling stack patchers for chunk {}", i / config.getInitializationChunkSize());
            Set<Long> resourceIdsChunk = resourcesById.entrySet().stream()
                    .skip(i)
                    .limit(config.getInitializationChunkSize())
                    .map(Map.Entry::getKey)
                    .collect(Collectors.toSet());
            scheduleForResourceChunk(resourceIdsChunk, enabledStackPatchTypes, resourcesById);
        }

        List<StackPatch> deprecatedStackPatches = stackPatchService.findAllByTypes(deprecatedStackPatchTypes);
        stackPatchService.deleteAll(deprecatedStackPatches);
    }

    private void scheduleForResourceChunk(Set<Long> resourceIdsChunk, Set<StackPatchType> enabledStackPatchTypes, Map<Long, JobResource> resourcesById) {
        Map<StackPatchType, Map<Long, List<StackPatch>>> stackPatchesByTypeAndResource = new EnumMap<>(StackPatchType.class);
        for (StackPatchType stackPatchType : enabledStackPatchTypes) {
            Map<Long, List<StackPatch>> stackPatchesByResource = stackPatchService.findAllByTypeForResourceIds(stackPatchType, resourceIdsChunk).stream()
                    .collect(Collectors.groupingBy(StackPatch::getResourceId));
            stackPatchesByTypeAndResource.put(stackPatchType, stackPatchesByResource);
        }
        for (Long resourceId : resourceIdsChunk) {
            scheduleForResource(resourcesById.get(resourceId), enabledStackPatchTypes, stackPatchesByTypeAndResource);
        }
    }

    private void scheduleForResource(JobResource resource, Set<StackPatchType> enabledStackPatchTypes,
            Map<StackPatchType, Map<Long, List<StackPatch>>> stackPatchesByTypeAndResource) {
        Long resourceId = Long.valueOf(resource.getLocalId());
        LOGGER.debug("Scheduling stack patchers for resourceId: {}", resourceId);
        ZonedDateTime now = nowSupplier.get();
        long maxInitialStartDelay = TimeUnit.HOURS.toMinutes(config.getMaxInitialStartDelayInHours());

        long initialStartDelay = randomLongProvider.apply(maxInitialStartDelay);
        ZonedDateTime jobFirstStart = now.plusMinutes(initialStartDelay);

        for (StackPatchType stackPatchType : enabledStackPatchTypes) {
            boolean stackPatchIsFinalized = stackPatchesByTypeAndResource
                    .getOrDefault(stackPatchType, Map.of())
                    .getOrDefault(resourceId, List.of())
                    .stream()
                    .anyMatch(stackPatch -> stackPatch.getStatus().isFinal());
            if (!stackPatchIsFinalized) {
                try {
                    LOGGER.debug("Scheduling stack patcher {} for stack {} with first start {}", stackPatchType, resourceId, jobFirstStart);
                    ExistingStackPatcherJobAdapter jobAdapter = new ExistingStackPatcherJobAdapter(resource, stackPatchType);
                    stackPatchService.getOrCreate(resourceId, stackPatchType);
                    jobService.schedule(jobAdapter, jobFirstStart);

                    jobFirstStart = calculateNextJobFirstStart(jobFirstStart, now, maxInitialStartDelay, enabledStackPatchTypes.size());
                } catch (Exception e) {
                    LOGGER.error("Failed to schedule stack patcher {} for stack {} with first start {}", stackPatchType, resourceId, jobFirstStart, e);
                }
            }
        }
    }

    private ZonedDateTime calculateNextJobFirstStart(ZonedDateTime currentJobFirstStart, ZonedDateTime now, long maxInitialStartDelay, long maxJobCount) {
        long minutesBetweenFirstStarts = maxInitialStartDelay / maxJobCount;
        ZonedDateTime nextJobFirstStart = currentJobFirstStart.plusMinutes(minutesBetweenFirstStarts);

        ZonedDateTime latestFirstStart = now.plusMinutes(maxInitialStartDelay);
        if (nextJobFirstStart.isAfter(latestFirstStart)) {
            long overflow = ChronoUnit.MINUTES.between(latestFirstStart, nextJobFirstStart);
            nextJobFirstStart = now.plusMinutes(overflow);
        }
        return nextJobFirstStart;
    }

    private Set<StackPatchType> getEnabledStackPatchTypes() {
        Map<StackPatchType, StackPatchTypeConfig> patchConfigs = getPatchConfigs();
        return existingStackPatchServices.stream()
                .map(ExistingStackPatchService::getStackPatchType)
                .filter(stackPatchType -> patchConfigs.getOrDefault(stackPatchType, DISABLED).isEnabled())
                .filter(stackPatchType -> !StackPatchTypeStatus.DEPRECATED.equals(stackPatchType.getStatus()))
                .collect(Collectors.toSet());
    }

    private Set<StackPatchType> getDeprecatedStackPatchTypes() {
        return getPatchConfigs().entrySet().stream()
                .filter(entry -> !entry.getValue().isEnabled())
                .map(Map.Entry::getKey)
                .filter(stackPatchType -> StackPatchTypeStatus.DEPRECATED.equals(stackPatchType.getStatus()))
                .collect(Collectors.toSet());
    }

    private Map<StackPatchType, StackPatchTypeConfig> getPatchConfigs() {
        return Objects.requireNonNullElse(config.getPatchConfigs(), Map.of());
    }
}
