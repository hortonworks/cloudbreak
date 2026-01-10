package com.sequenceiq.cloudbreak.job.stackpatcher;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.domain.stack.StackPatch;
import com.sequenceiq.cloudbreak.domain.stack.StackPatchType;
import com.sequenceiq.cloudbreak.domain.stack.StackPatchTypeStatus;
import com.sequenceiq.cloudbreak.job.AbstractStackJobInitializer;
import com.sequenceiq.cloudbreak.job.stackpatcher.config.ExistingStackPatcherConfig;
import com.sequenceiq.cloudbreak.quartz.model.JobResource;
import com.sequenceiq.cloudbreak.service.stackpatch.StackPatchService;

@Component
public class ExistingStackPatcherJobInitializer extends AbstractStackJobInitializer {

    private static final Logger LOGGER = LoggerFactory.getLogger(ExistingStackPatcherJobInitializer.class);

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

        List<JobResource> stacks = getAliveJobResources();
        Set<Long> stackIds = stacks.stream()
                .map(JobResource::getLocalId)
                .map(Long::valueOf)
                .collect(Collectors.toSet());
        enabledStackPatchTypes.forEach(stackPatchType -> {
            LOGGER.info("Scheduling stack patcher jobs for {}", stackPatchType);
            Map<Long, List<StackPatch>> stackPatchesByStack = stackPatchService.findAllByTypeForStackIds(stackPatchType, stackIds).stream()
                    .collect(Collectors.groupingBy(StackPatch::getStackId));

            for (JobResource stack : stacks) {
                Long stackId = Long.valueOf(stack.getLocalId());
                List<StackPatch> stackPatches = stackPatchesByStack.get(stackId);
                boolean stackPatchIsFinalized = stackPatches != null && stackPatches.stream().anyMatch(stackPatch -> stackPatch.getStatus().isFinal());
                if (!stackPatchIsFinalized) {
                    ExistingStackPatcherJobAdapter jobAdapter = new ExistingStackPatcherJobAdapter(stack, stackPatchType);
                    stackPatchService.getOrCreate(stackId, stackPatchType);
                    jobService.schedule(jobAdapter);
                }
            }
        });

        List<StackPatch> deprecatedStackPatches = stackPatchService.findAllByTypes(deprecatedStackPatchTypes);
        stackPatchService.deleteAll(deprecatedStackPatches);
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
