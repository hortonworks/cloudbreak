package com.sequenceiq.cloudbreak.service.stackstatus;

import java.util.Optional;

import javax.inject.Inject;

import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.domain.stack.StackStatus;
import com.sequenceiq.cloudbreak.repository.StackStatusRepository;

@Service
public class StackStatusService {

    @Inject
    private StackStatusRepository repository;

    public Optional<StackStatus> findFirstByStackIdOrderByCreatedDesc(long stackId) {
        return repository.findFirstByStackIdOrderByCreatedDesc(stackId);
    }

}
