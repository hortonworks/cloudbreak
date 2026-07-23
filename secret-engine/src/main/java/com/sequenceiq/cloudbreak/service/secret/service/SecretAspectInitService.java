package com.sequenceiq.cloudbreak.service.secret.service;

import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;

import org.springframework.stereotype.Service;

@Service
public class SecretAspectInitService {
    @Inject
    private SecretAspectService secretAspectService;

    @PostConstruct
    public void init() {
        secretAspectService.init();
    }
}
