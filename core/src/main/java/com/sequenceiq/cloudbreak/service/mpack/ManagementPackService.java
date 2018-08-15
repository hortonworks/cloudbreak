package com.sequenceiq.cloudbreak.service.mpack;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.google.common.base.Preconditions;
import com.sequenceiq.cloudbreak.authorization.OrganizationResource;
import com.sequenceiq.cloudbreak.common.model.user.IdentityUser;
import com.sequenceiq.cloudbreak.domain.ManagementPack;
import com.sequenceiq.cloudbreak.repository.ManagementPackRepository;
import com.sequenceiq.cloudbreak.repository.OrganizationResourceRepository;
import com.sequenceiq.cloudbreak.service.AbstractOrganizationAwareResourceService;
import com.sequenceiq.cloudbreak.service.AuthorizationService;
import com.sequenceiq.cloudbreak.service.organization.OrganizationService;

@Service
public class ManagementPackService extends AbstractOrganizationAwareResourceService<ManagementPack> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ManagementPackService.class);

    @Inject
    private ManagementPackRepository mpackRepository;

    @Inject
    private AuthorizationService authorizationService;

    @Inject
    private OrganizationService organizationService;

    public ManagementPack getByName(String name, IdentityUser user) {
        return mpackRepository.findOneByNameAndAccount(name, user.getAccount());
    }

    public ManagementPack get(Long id) {
        return mpackRepository.findOneById(id);
    }

    public void delete(Long id, IdentityUser user) {
        ManagementPack mpack = mpackRepository.findOneByIdAndAccount(id, user.getAccount());
        delete(mpack);
    }

    public void delete(String name, IdentityUser user) {
        ManagementPack mpack = mpackRepository.findOneByNameBasedOnAccount(name, user.getAccount(), user.getUserId());
        delete(mpack);
    }

    public ManagementPack create(IdentityUser identityUser, ManagementPack mpack) {
        LOGGER.debug("Creating Management Pack: [User: '{}', Account: '{}']", identityUser.getUsername(), identityUser.getAccount());
        mpack.setOwner(identityUser.getUserId());
        mpack.setAccount(identityUser.getAccount());
        return createInDefaultOrganization(mpack);
    }

    public ManagementPack create(ManagementPack mpack) {
        Preconditions.checkNotNull(mpack.getOwner(), "Owner cannot be null");
        Preconditions.checkNotNull(mpack.getAccount(), "Account cannot be null");
        Preconditions.checkNotNull(mpack.getOrganization(), "Organization cannot be null");
        LOGGER.debug("Creating Management Pack: [User: '{}', Account: '{}', Organization: '{}']",
                mpack.getOwner(), mpack.getAccount(), mpack.getOrganization().getId());
        return mpackRepository.save(mpack);
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
    protected boolean canDelete(ManagementPack resource) {
        return true;
    }

    @Override
    protected void prepareCreation(ManagementPack resource) {

    }
}
