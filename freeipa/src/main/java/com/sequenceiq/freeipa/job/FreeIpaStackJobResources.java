package com.sequenceiq.freeipa.job;

import java.util.Optional;

import com.sequenceiq.cloudbreak.quartz.model.JobResource;
import com.sequenceiq.freeipa.entity.Stack;

/**
 * Maps a FreeIPA {@link Stack} to {@link JobResource}
 */
public final class FreeIpaStackJobResources {

    private FreeIpaStackJobResources() {
    }

    public static JobResource fromStack(Stack stack) {
        return new FreeIpaStackBackedJobResource(stack);
    }

    private record FreeIpaStackBackedJobResource(Stack stack) implements JobResource {

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
