package com.sequenceiq.cloudbreak.service.saltsecurityconf;

import java.util.Optional;

import jakarta.inject.Inject;

import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.domain.SaltSecurityConfig;
import com.sequenceiq.cloudbreak.repository.SaltSecurityConfigRepository;

@Service
public class SaltSecurityConfigService {

    @Inject
    private SaltSecurityConfigRepository repository;

    public SaltSecurityConfig save(SaltSecurityConfig saltSecurityConfig) {
        return repository.save(saltSecurityConfig);
    }

    public Optional<SaltSecurityConfig> getById(Long id) {
        return repository.findById(id);
    }

}
