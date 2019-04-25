package com.sequenceiq.cloudbreak.service.environment;

import javax.inject.Inject;
import javax.inject.Named;

import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.domain.environment.BaseNetwork;
import com.sequenceiq.cloudbreak.repository.environment.BaseNetworkRepository;

@Service
public class EnvironmentNetworkService {
    @Inject
    @Named("baseNetworkRepository")
    private BaseNetworkRepository networkRepository;

    public BaseNetwork save(BaseNetwork awsNetwork) {
        Object saved = networkRepository.save(awsNetwork);
        return (BaseNetwork) saved;
    }
}
