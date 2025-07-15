package com.sequenceiq.cloudbreak.service.stackstatus;

import java.util.List;
import java.util.Optional;

import jakarta.inject.Inject;

import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status;
import com.sequenceiq.cloudbreak.common.metrics.status.StackCountByStatusView;
import com.sequenceiq.cloudbreak.domain.stack.StackStatus;
import com.sequenceiq.cloudbreak.repository.StackStatusRepository;
import com.sequenceiq.common.api.type.Tunnel;

@Service
public class StackStatusService {

    @Inject
    private StackStatusRepository repository;

    public Optional<StackStatus> findFirstByStackIdOrderByCreatedDesc(long stackId) {
        return repository.findFirstByStackIdOrderByCreatedDesc(stackId);
    }

    public List<StackStatus> findAllStackStatusesById(long stackId) {
        return repository.findAllByStackIdOrderByCreatedAsc(stackId);
    }

    public List<StackStatus> findAllStackStatusesById(long stackId, long createdAfter) {
        return repository.findAllByStackIdAndCreatedGreaterThanEqualOrderByCreatedDesc(stackId, createdAfter);
    }

    public List<StackCountByStatusView> countStacksByStatusAndCloudPlatform(String cloudPlatform) {
        return repository.countStacksByStatusAndCloudPlatform(cloudPlatform);
    }

    public List<StackCountByStatusView> countStacksByStatusAndTunnel(Tunnel tunnel) {
        return repository.countStacksByStatusAndTunnel(tunnel);
    }

    public void cleanupStatus(long stackId, Status preservedStatus) {
        repository.deleteAllByStackIdAndStatusNot(stackId, preservedStatus);
    }

}
