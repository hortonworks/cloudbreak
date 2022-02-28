package com.sequenceiq.cloudbreak.job.stackpatcher;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.StackPatchType;
import com.sequenceiq.cloudbreak.job.AbstractStackJobInitializer;
import com.sequenceiq.cloudbreak.job.stackpatcher.config.ExistingStackPatcherConfig;

@Component
public class ExistingStackPatcherJobInitializer extends AbstractStackJobInitializer {

    private static final Logger LOGGER = LoggerFactory.getLogger(ExistingStackPatcherJobInitializer.class);

    @Inject
    private ExistingStackPatcherConfig config;

    @Inject
    private ExistingStackPatcherJobService jobService;

    @Override
    public void initJobs() {
        Set<StackPatchType> enabledStackPatchTypes = getEnabledStackPatchTypes();
        LOGGER.info("Existing stack patch types enabled: {}", enabledStackPatchTypes);

        Set<Stack> stacks = getAliveAndNotDeleteInProgressStacksStream().collect(Collectors.toSet());
        enabledStackPatchTypes.forEach(stackPatchType -> {
            LOGGER.info("Scheduling stack patcher jobs for {}", stackPatchType);
            stacks.forEach(stack -> jobService.schedule(new ExistingStackPatcherJobAdapter(stack, stackPatchType)));
        });
    }

    private Set<StackPatchType> getEnabledStackPatchTypes() {
        return config.getPatchConfigs().entrySet().stream()
                .filter(entry -> entry.getValue().isEnabled())
                .map(Map.Entry::getKey)
                .collect(Collectors.toSet());
    }
}
