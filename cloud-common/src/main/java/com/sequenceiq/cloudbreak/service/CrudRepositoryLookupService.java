package com.sequenceiq.cloudbreak.service;

import java.util.List;

import javax.inject.Inject;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Service;

@Service
public class CrudRepositoryLookupService extends RepositoryLookupService<CrudRepository<?, ?>> {

    @Inject
    private List<CrudRepository<?, ?>> repositoryList;

    @Override
    protected List<CrudRepository<?, ?>> getRepositoryList() {
        return repositoryList;
    }
}
