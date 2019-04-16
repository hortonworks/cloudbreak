package com.sequenceiq.cloudbreak.service.tenant;

import java.util.Optional;

import javax.inject.Inject;

import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.domain.workspace.Tenant;
import com.sequenceiq.cloudbreak.repository.workspace.TenantRepository;

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
