package com.sequenceiq.cloudbreak.service.ha;

import java.util.Set;

public interface HaApplication {
    Set<Long> getDeletingResources(Set<Long> resourceIds);

    Set<Long> getAllDeletingResources();

    void cleanupInMemoryStore(Long resourceId);

    void cancelRunningFlow(Long resourceId);
}
