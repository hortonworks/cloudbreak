package com.sequenceiq.cloudbreak.monitoring;

import java.util.Optional;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.common.domain.IdAware;

@Service
public interface MonitoringEnablementService<S extends IdAware> {

    @Cacheable(cacheNames = "monitoringEnablementCache", key = "{ #entity.getId() }")
    Optional<Boolean> computeMonitoringEnabled(S entity);
}
