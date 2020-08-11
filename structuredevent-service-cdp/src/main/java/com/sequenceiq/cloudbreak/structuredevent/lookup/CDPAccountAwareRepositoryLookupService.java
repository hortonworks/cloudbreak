package com.sequenceiq.cloudbreak.structuredevent.lookup;

import java.util.List;

import javax.inject.Inject;

import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.structuredevent.repository.CDPResourceRepository;

@Service
public class CDPAccountAwareRepositoryLookupService extends CDPRepositoryLookupService<CDPResourceRepository<?, ?>> {

    @Inject
    private List<CDPResourceRepository<?, ?>> repositoryList;

    @Override
    protected List<CDPResourceRepository<?, ?>> getRepositoryList() {
        return repositoryList;
    }
}
