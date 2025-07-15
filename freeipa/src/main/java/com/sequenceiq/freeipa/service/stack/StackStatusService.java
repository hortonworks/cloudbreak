package com.sequenceiq.freeipa.service.stack;

import static com.sequenceiq.cloudbreak.common.exception.NotFoundException.notFound;

import java.util.List;

import jakarta.inject.Inject;

import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.common.metrics.status.StackCountByStatusView;
import com.sequenceiq.common.api.type.Tunnel;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.Status;
import com.sequenceiq.freeipa.entity.StackStatus;
import com.sequenceiq.freeipa.repository.StackStatusRepository;

@Service
public class StackStatusService {

    @Inject
    private StackStatusRepository repository;

    public StackStatus findFirstByStackIdOrderByCreatedDesc(long stackId) {
        return repository.findFirstByStackIdOrderByCreatedDesc(stackId).orElseThrow(notFound("stackStatus", stackId));
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
