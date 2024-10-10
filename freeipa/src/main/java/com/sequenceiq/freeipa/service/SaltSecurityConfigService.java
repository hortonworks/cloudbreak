package com.sequenceiq.freeipa.service;

import jakarta.inject.Inject;

import org.springframework.stereotype.Service;

import com.sequenceiq.freeipa.entity.SaltSecurityConfig;
import com.sequenceiq.freeipa.repository.SaltSecurityConfigRepository;

@Service
public class SaltSecurityConfigService {

    @Inject
    private SaltSecurityConfigRepository saltSecurityConfigRepository;

    public void save(SaltSecurityConfig saltSecurityConfig) {
        saltSecurityConfigRepository.save(saltSecurityConfig);
    }
}
