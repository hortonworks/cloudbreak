package com.sequenceiq.freeipa.service.stack;

import static com.sequenceiq.cloudbreak.common.exception.NotFoundException.notFound;

import java.util.List;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.common.metrics.status.StackCountByStatusView;
import com.sequenceiq.cloudbreak.quartz.statuscleanup.StackStatusCleanupService;
import com.sequenceiq.common.api.type.Tunnel;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.Status;
import com.sequenceiq.freeipa.entity.StackStatus;
import com.sequenceiq.freeipa.repository.StackStatusRepository;

@Service
public class StackStatusService implements StackStatusCleanupService {

    private static final Logger LOGGER = LoggerFactory.getLogger(StackStatusService.class);

    private static final String ID_FIELD_NAME = "id";

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

    @Override
    public void cleanupByTimestamp(int limit, long timestampBefore) {
        PageRequest pageRequest = PageRequest.of(0, limit, Sort.by(ID_FIELD_NAME).ascending());
        List<StackStatus> removableStatuses = repository.findAllByCreatedLessThan(timestampBefore, pageRequest).getContent();
        LOGGER.debug("Removing stack statuses, count: {}.", removableStatuses.size());
        repository.deleteAll(removableStatuses);
    }

}
