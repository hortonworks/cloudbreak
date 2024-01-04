package com.sequenceiq.cloudbreak.service.tenant;

import java.util.Optional;

import jakarta.inject.Inject;

import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.workspace.model.Tenant;
import com.sequenceiq.cloudbreak.workspace.repository.workspace.TenantRepository;

@Service
public class TenantService {

    @Inject
    private TenantRepository repository;

    public Optional<Tenant> findByName(String name) {
        return repository.findByName(name);
    }

    public Tenant save(Tenant tenant) {
        return repository.save(tenant);
    }

}
