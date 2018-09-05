package com.sequenceiq.cloudbreak.service.mpack;

import static com.sequenceiq.cloudbreak.controller.exception.NotFoundException.notFound;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.authorization.OrganizationResource;
import com.sequenceiq.cloudbreak.domain.ManagementPack;
import com.sequenceiq.cloudbreak.repository.ManagementPackRepository;
import com.sequenceiq.cloudbreak.repository.organization.OrganizationResourceRepository;
import com.sequenceiq.cloudbreak.service.AbstractOrganizationAwareResourceService;

@Service
public class ManagementPackService extends AbstractOrganizationAwareResourceService<ManagementPack> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ManagementPackService.class);

    @Inject
    private ManagementPackRepository mpackRepository;

    /**
     * @param id id of mpack
     * @return the mpack
     * @deprecated the queries for id are not supported on V3 API, this is only for backward compability. Should be remove with V1 API.
     */
    @Deprecated(since = "2.8.0", forRemoval = true)
    public ManagementPack getById(Long id) {
        return mpackRepository.findById(id).orElseThrow(notFound("Mpack", id));
    }

    @Override
    public OrganizationResourceRepository<ManagementPack, Long> repository() {
        return mpackRepository;
    }

    @Override
    public OrganizationResource resource() {
        return OrganizationResource.MPACK;
    }

    @Override
    protected void prepareDeletion(ManagementPack resource) {

    }

    @Override
    protected void prepareCreation(ManagementPack resource) {

    }
}
