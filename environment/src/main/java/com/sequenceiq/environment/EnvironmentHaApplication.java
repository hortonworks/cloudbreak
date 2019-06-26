package com.sequenceiq.environment;

import static com.sequenceiq.environment.environment.EnvironmentStatus.ARCHIVED;
import static com.sequenceiq.environment.environment.EnvironmentStatus.DELETE_FAILED;
import static com.sequenceiq.environment.environment.EnvironmentStatus.DELETE_INITIATED;
import static com.sequenceiq.environment.environment.EnvironmentStatus.FREEIPA_DELETE_IN_PROGRESS;
import static com.sequenceiq.environment.environment.EnvironmentStatus.NETWORK_DELETE_IN_PROGRESS;
import static com.sequenceiq.environment.environment.EnvironmentStatus.RDBMS_DELETE_IN_PROGRESS;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.service.ha.HaApplication;
import com.sequenceiq.environment.environment.EnvironmentStatus;
import com.sequenceiq.environment.environment.dto.EnvironmentDto;
import com.sequenceiq.environment.environment.flow.ReactorFlowManager;
import com.sequenceiq.environment.environment.service.EnvironmentService;
import com.sequenceiq.environment.store.EnvironmentInMemoryStateStore;

@Primary
@Component
public class EnvironmentHaApplication implements HaApplication {

    public static final List<EnvironmentStatus> DELETION_STATUSES
            = List.of(DELETE_INITIATED, NETWORK_DELETE_IN_PROGRESS, FREEIPA_DELETE_IN_PROGRESS, RDBMS_DELETE_IN_PROGRESS, DELETE_FAILED, ARCHIVED);

    private final EnvironmentService environmentService;

    private final ReactorFlowManager reactorFlowManager;

    public EnvironmentHaApplication(EnvironmentService environmentService, ReactorFlowManager reactorFlowManager) {
        this.environmentService = environmentService;
        this.reactorFlowManager = reactorFlowManager;
    }

    @Override
    public Set<Long> getDeletingResources(Set<Long> resourceIds) {
        List<EnvironmentDto> environments = environmentService.findAllByIdInAndStatusIn(resourceIds, DELETION_STATUSES);
        return environments.stream().map(EnvironmentDto::getId).collect(Collectors.toSet());
    }

    @Override
    public Set<Long> getAllDeletingResources() {
        List<EnvironmentDto> environments = environmentService.findAllByStatusIn(DELETION_STATUSES);
        return environments.stream().map(EnvironmentDto::getId).collect(Collectors.toSet());
    }

    @Override
    public void cleanupInMemoryStore(Long resourceId) {
        EnvironmentInMemoryStateStore.delete(resourceId);
    }

    @Override
    public void cancelRunningFlow(Long resourceId) {
        environmentService.findById(resourceId).ifPresent(environmentDto -> {
            reactorFlowManager.cancelRunningFlows(resourceId, environmentDto.getName());
        });
    }
}
