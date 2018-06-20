package com.sequenceiq.cloudbreak.service.mpack;

import java.util.Set;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.google.common.base.Preconditions;
import com.sequenceiq.cloudbreak.common.model.user.IdentityUser;
import com.sequenceiq.cloudbreak.common.model.user.IdentityUserRole;
import com.sequenceiq.cloudbreak.domain.ManagementPack;
import com.sequenceiq.cloudbreak.repository.ManagementPackRepository;
import com.sequenceiq.cloudbreak.service.AuthorizationService;

@Service
public class ManagementPackService {
    private static final Logger LOGGER = LoggerFactory.getLogger(ManagementPackService.class);

    @Inject
    private ManagementPackRepository mpackRepository;

    @Inject
    private AuthorizationService authorizationService;

    public Set<ManagementPack> retrievePrivateManagementPacks(IdentityUser user) {
        return mpackRepository.findByOwner(user.getUserId());
    }

    public ManagementPack getPrivateManagementPack(String name, IdentityUser user) {
        return mpackRepository.findOneByNameAndOwner(name, user.getUserId());
    }

    public ManagementPack getPublicManagementPack(String name, IdentityUser user) {
        return mpackRepository.findOneByNameBasedOnAccount(name, user.getAccount(), user.getUserId());
    }

    public ManagementPack getByName(String name, IdentityUser user) {
        return mpackRepository.findOneByNameAndAccount(name, user.getAccount());
    }

    public Set<ManagementPack> retrieveAccountManagementPacks(IdentityUser user) {
        return user.getRoles().contains(IdentityUserRole.ADMIN) ? mpackRepository.findByAccount(user.getAccount())
                : mpackRepository.findPublicInAccountForUser(user.getUserId(), user.getAccount());
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

    public ManagementPack create(IdentityUser user, ManagementPack mpack) {
        LOGGER.debug("Creating Management Pack: [User: '{}', Account: '{}']", user.getUsername(), user.getAccount());
        mpack.setOwner(user.getUserId());
        mpack.setAccount(user.getAccount());
        return mpackRepository.save(mpack);
    }

    public ManagementPack create(ManagementPack mpack) {
        Preconditions.checkNotNull(mpack.getOwner(), "Owner cannot be null");
        Preconditions.checkNotNull(mpack.getAccount(), "Account cannot be null");
        LOGGER.debug("Creating RDS configuration: [User: '{}', Account: '{}']", mpack.getOwner(), mpack.getAccount());
        return mpackRepository.save(mpack);
    }

    private void delete(ManagementPack mpack) {
        LOGGER.info("Deleting Management Pack. {} - {}", new Object[]{mpack.getId(), mpack.getName()});
        mpackRepository.delete(mpack);
    }
}
