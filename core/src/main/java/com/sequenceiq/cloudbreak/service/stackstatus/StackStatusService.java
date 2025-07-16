package com.sequenceiq.cloudbreak.service.stackstatus;

import java.util.List;
import java.util.Optional;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status;
import com.sequenceiq.cloudbreak.common.metrics.status.StackCountByStatusView;
import com.sequenceiq.cloudbreak.domain.stack.StackStatus;
import com.sequenceiq.cloudbreak.quartz.statuscleanup.StackStatusCleanupService;
import com.sequenceiq.cloudbreak.repository.StackStatusRepository;
import com.sequenceiq.common.api.type.Tunnel;

@Service
public class StackStatusService implements StackStatusCleanupService {

    private static final Logger LOGGER = LoggerFactory.getLogger(StackStatusService.class);

    private static final String ID_FIELD_NAME = "id";

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

    public void cleanupByPreservedStatus(long stackId, Status preservedStatus) {
        repository.deleteAllByStackIdAndStatusNot(stackId, preservedStatus);
    }

    @Override
    public void cleanupByTimestamp(int limit, long timestampBefore) {
        PageRequest pageRequest = PageRequest.of(0, limit, Sort.by(ID_FIELD_NAME).ascending());
        List<StackStatus> removableStatuses = repository.findAllByCreatedLessThan(timestampBefore, pageRequest).getContent();
        LOGGER.debug("Removing stack statuses, count: {}.", removableStatuses.size());
        repository.deleteAll(removableStatuses);
    }
}
