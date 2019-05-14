package com.sequenceiq.environment.environment.service;

import javax.inject.Inject;
import javax.inject.Named;

import org.springframework.stereotype.Service;

import com.sequenceiq.environment.environment.domain.network.BaseNetwork;
import com.sequenceiq.environment.environment.repository.network.BaseNetworkRepository;

@Service
public class EnvironmentNetworkService {
    @Inject
    @Named("baseNetworkRepository")
    private BaseNetworkRepository networkRepository;

    @SuppressWarnings("unchecked")
    public BaseNetwork save(BaseNetwork awsNetwork) {
        Object saved = networkRepository.save(awsNetwork);
        return (BaseNetwork) saved;
    }
}
