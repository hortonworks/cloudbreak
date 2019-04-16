package com.sequenceiq.cloudbreak.service.securityconfig;

import java.util.Optional;

import javax.inject.Inject;

import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.domain.SecurityConfig;
import com.sequenceiq.cloudbreak.repository.SecurityConfigRepository;

@Service
public class SecurityConfigService {

    @Inject
    private SecurityConfigRepository repository;

    public SecurityConfig save(SecurityConfig securityConfig) {
        return repository.save(securityConfig);
    }

    public Optional<SecurityConfig> findOneByStackId(Long stackId) {
        return repository.findOneByStackId(stackId);
    }

}
