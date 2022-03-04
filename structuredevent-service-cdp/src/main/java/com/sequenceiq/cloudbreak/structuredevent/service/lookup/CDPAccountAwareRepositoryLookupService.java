package com.sequenceiq.cloudbreak.structuredevent.service.lookup;

import java.util.List;

import javax.inject.Inject;

import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.common.dal.repository.AccountAwareResourceRepository;

@Service
public class CDPAccountAwareRepositoryLookupService extends CDPRepositoryLookupService<AccountAwareResourceRepository<?, ?>> {

    @Inject
    private List<AccountAwareResourceRepository<?, ?>> repositoryList;

    @Override
    protected List<AccountAwareResourceRepository<?, ?>> getRepositoryList() {
        return repositoryList;
    }
}
