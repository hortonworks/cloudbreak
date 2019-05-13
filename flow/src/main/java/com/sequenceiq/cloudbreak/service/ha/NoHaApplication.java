package com.sequenceiq.cloudbreak.service.ha;

import java.util.Set;

import org.springframework.stereotype.Component;

@Component
public class NoHaApplication implements HaApplication {
    @Override
    public Set<Long> getDeletingResources(Set<Long> resourceIds) {
        return Set.of();
    }

    @Override
    public Set<Long> getAllDeletingResources() {
        return Set.of();
    }

    @Override
    public void cleanupInMemoryStore(Long resourceId) {

    }

    @Override
    public void cancelRunningFlow(Long resourceId) {

    }
}
