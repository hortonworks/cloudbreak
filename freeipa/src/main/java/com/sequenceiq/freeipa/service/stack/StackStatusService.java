package com.sequenceiq.freeipa.service.stack;

import javax.inject.Inject;

import com.sequenceiq.freeipa.entity.StackStatus;
import com.sequenceiq.freeipa.repository.StackStatusRepository;
import org.springframework.stereotype.Service;

import static com.sequenceiq.cloudbreak.exception.NotFoundException.notFound;

@Service
public class StackStatusService {

    @Inject
    private StackStatusRepository repository;

    public StackStatus findFirstByStackIdOrderByCreatedDesc(long stackId) {
        return repository.findFirstByStackIdOrderByCreatedDesc(stackId).orElseThrow(notFound("stackStatus", stackId));
    }

}