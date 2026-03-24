package com.sequenceiq.cloudbreak.job;

import java.util.Optional;

import com.sequenceiq.cloudbreak.quartz.model.JobResource;
import com.sequenceiq.cloudbreak.view.StackView;

/**
 * Maps an in-memory {@link StackView} to {@link JobResource}
 */
public final class StackViewJobResources {

    private StackViewJobResources() {
    }

    public static JobResource fromStackView(StackView stack) {
        return new StackViewBackedJobResource(stack);
    }

    private record StackViewBackedJobResource(StackView stack) implements JobResource {

        @Override
        public String getLocalId() {
            return String.valueOf(stack.getId());
        }

        @Override
        public String getRemoteResourceId() {
            return stack.getResourceCrn();
        }

        @Override
        public String getName() {
            return stack.getName();
        }

        @Override
        public Optional<String> getProvider() {
            return Optional.ofNullable(stack.getCloudPlatform());
        }
    }
}
