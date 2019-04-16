package com.sequenceiq.cloudbreak.service.constraint;

import javax.inject.Inject;

import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.domain.Constraint;
import com.sequenceiq.cloudbreak.repository.ConstraintRepository;

@Service
public class ConstraintService {

    @Inject
    private ConstraintRepository repository;

    public Constraint save(Constraint constraint) {
        return repository.save(constraint);
    }

    public void deleteAll(Iterable<Constraint> constraints) {
        repository.deleteAll(constraints);
    }

}
