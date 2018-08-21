package com.sequenceiq.cloudbreak.service.mpack;

import static com.sequenceiq.cloudbreak.controller.exception.NotFoundException.notFound;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.authorization.OrganizationResource;
import com.sequenceiq.cloudbreak.common.model.user.IdentityUser;
import com.sequenceiq.cloudbreak.domain.ManagementPack;
import com.sequenceiq.cloudbreak.repository.ManagementPackRepository;
import com.sequenceiq.cloudbreak.repository.organization.OrganizationResourceRepository;
import com.sequenceiq.cloudbreak.service.AbstractOrganizationAwareResourceService;
import com.sequenceiq.cloudbreak.service.AuthenticatedUserService;

@Service
public class DefaultManagementPackService extends AbstractOrganizationAwareResourceService<ManagementPack> implements LegacyManagementPackService {

    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultManagementPackService.class);

    @Inject
    private ManagementPackRepository mpackRepository;

    @Inject
    private AuthenticatedUserService authenticatedUserService;

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
    protected OrganizationResourceRepository<ManagementPack, Long> repository() {
        return mpackRepository;
    }

    @Override
    protected OrganizationResource resource() {
        return OrganizationResource.MPACK;
    }

    @Override
    protected void prepareDeletion(ManagementPack resource) {

    }

    @Override
    protected void prepareCreation(ManagementPack resource) {
        IdentityUser identityUser = authenticatedUserService.getCbUser();
        resource.setOwner(identityUser.getUserId());
        resource.setAccount(identityUser.getAccount());
    }
}
