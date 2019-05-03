package com.sequenceiq.freeipa.service;

import javax.inject.Inject;

import org.springframework.stereotype.Service;

import com.sequenceiq.freeipa.entity.SecurityConfig;
import com.sequenceiq.freeipa.repository.SecurityConfigRepository;

@Service
public class SecurityConfigService {

    @Inject
    private SecurityConfigRepository securityConfigRepository;

    public SecurityConfig save(SecurityConfig securityConfig) {
        return securityConfigRepository.save(securityConfig);
    }
}
