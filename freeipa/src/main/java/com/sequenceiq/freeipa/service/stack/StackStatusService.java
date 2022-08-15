package com.sequenceiq.freeipa.service.stack;

import static com.sequenceiq.cloudbreak.common.exception.NotFoundException.notFound;

import javax.inject.Inject;

import org.springframework.stereotype.Service;

import com.sequenceiq.freeipa.entity.StackStatus;
import com.sequenceiq.freeipa.repository.StackStatusRepository;

@Service
public class StackStatusService {

    @Inject
    private StackStatusRepository repository;

    public StackStatus findFirstByStackIdOrderByCreatedDesc(long stackId) {
        return repository.findFirstByStackIdOrderByCreatedDesc(stackId).orElseThrow(notFound("stackStatus", stackId));
    }

}
