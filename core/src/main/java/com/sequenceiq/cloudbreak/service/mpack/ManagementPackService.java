package com.sequenceiq.cloudbreak.service.mpack;

import java.util.Set;

import javax.inject.Inject;
import javax.transaction.Transactional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.google.common.base.Preconditions;
import com.sequenceiq.cloudbreak.common.model.user.IdentityUser;
import com.sequenceiq.cloudbreak.common.model.user.IdentityUserRole;
import com.sequenceiq.cloudbreak.controller.NotFoundException;
import com.sequenceiq.cloudbreak.domain.ManagementPack;
import com.sequenceiq.cloudbreak.repository.ManagementPackRepository;
import com.sequenceiq.cloudbreak.service.AuthorizationService;

@Service
@Transactional
public class ManagementPackService {
    private static final Logger LOGGER = LoggerFactory.getLogger(ManagementPackService.class);

    @Inject
    private ManagementPackRepository mpackRepository;

    @Inject
    private AuthorizationService authorizationService;

    public Set<ManagementPack> retrievePrivateRdsConfigs(IdentityUser user) {
        return mpackRepository.findByOwner(user.getUserId());
    }

    public ManagementPack getPrivateManagementPack(String name, IdentityUser user) {
        ManagementPack mpack = mpackRepository.findOneByNameAndOwner(name, user.getUserId());
        if (mpack == null) {
            throw new NotFoundException(String.format("Management pack '%s' not found.", name));
        }
        return mpack;
    }

    public ManagementPack getPublicManagementPack(String name, IdentityUser user) {
        ManagementPack mpack = mpackRepository.findOneByNameBasedOnAccount(name, user.getAccount(), user.getUserId());
        if (mpack == null) {
            throw new NotFoundException(String.format("Management pack '%s' not found.", name));
        }
        return mpack;
    }

    public ManagementPack getByName(String name, IdentityUser user) {
        ManagementPack mpack = mpackRepository.findOneByNameAndAccount(name, user.getAccount());
        if (mpack == null) {
            throw new NotFoundException(String.format("Management pack '%s' not found.", name));
        }
        authorizationService.hasReadPermission(mpack);
        return mpack;
    }

    public Set<ManagementPack> retrieveAccountManagementPacks(IdentityUser user) {
        return user.getRoles().contains(IdentityUserRole.ADMIN) ? mpackRepository.findByAccount(user.getAccount())
                : mpackRepository.findPublicInAccountForUser(user.getUserId(), user.getAccount());
    }

    public ManagementPack get(Long id) {
        ManagementPack mpack = mpackRepository.findOneById(id);
        if (mpack == null) {
            throw new NotFoundException(String.format("Management pack '%s' not found.", id));
        }
        authorizationService.hasReadPermission(mpack);
        return mpack;
    }

    public void delete(Long id, IdentityUser user) {
        ManagementPack mpack = mpackRepository.findOneByIdAndAccount(id, user.getAccount());
        if (mpack == null) {
            throw new NotFoundException(String.format("Management pack '%s' not found.", id));
        }
        delete(mpack);
    }

    public void delete(String name, IdentityUser user) {
        ManagementPack mpack = mpackRepository.findOneByNameBasedOnAccount(name, user.getAccount(), user.getUserId());
        if (mpack == null) {
            throw new NotFoundException(String.format("Management pack '%s' not found.", name));
        }
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
        authorizationService.hasWritePermission(mpack);
        mpackRepository.delete(mpack);
    }
}
