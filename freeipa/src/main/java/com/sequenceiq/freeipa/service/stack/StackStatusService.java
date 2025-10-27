package com.sequenceiq.freeipa.service.stack;

import static com.sequenceiq.cloudbreak.common.exception.NotFoundException.notFound;

import java.util.List;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.common.metrics.status.StackCountByStatusView;
import com.sequenceiq.common.api.type.Tunnel;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.Status;
import com.sequenceiq.freeipa.entity.StackStatus;
import com.sequenceiq.freeipa.repository.StackStatusRepository;

@Service
public class StackStatusService {

    private static final Logger LOGGER = LoggerFactory.getLogger(StackStatusService.class);

    private static final String ID_FIELD_NAME = "id";

    @Value("${stackstatuscleanup.limit:100}")
    private int statusCleanupLimit;

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

    public void cleanupByPreservedStatus(long stackId, Status preservedStatus) {
        repository.deleteAllByStackIdAndStatusNot(stackId, preservedStatus);
    }

    public void cleanupByStackId(Long stackId) {
        List<StackStatus> stackStatusesById = repository.findAllByStackIdOrderByCreatedDesc(stackId);
        List<StackStatus> removableStatuses = stackStatusesById.stream()
                .skip(statusCleanupLimit)
                .toList();
        LOGGER.debug("Removing stack statuses, count: {}.", removableStatuses.size());
        repository.deleteAllInBatch(removableStatuses);
    }

}
