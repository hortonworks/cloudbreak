package com.sequenceiq.cloudbreak.service.environment;

import javax.inject.Inject;

import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.aspect.DisabledBaseRepository;
import com.sequenceiq.cloudbreak.domain.environment.Environment;
import com.sequenceiq.cloudbreak.repository.environment.EnvironmentRepository;
import com.sequenceiq.cloudbreak.service.ResourceArchivator;

@Service
public class EnvironmentArchivatorService extends ResourceArchivator<Environment, Long> {

    @Inject
    private EnvironmentRepository environmentRepository;

    @Override
    protected DisabledBaseRepository<Environment, Long> repository() {
        return environmentRepository;
    }
}
