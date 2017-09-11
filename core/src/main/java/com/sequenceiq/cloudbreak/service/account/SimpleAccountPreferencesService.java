package com.sequenceiq.cloudbreak.service.account;

import java.util.Collections;
import java.util.concurrent.locks.Lock;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.prepost.PostAuthorize;
import org.springframework.stereotype.Service;

import com.google.common.util.concurrent.Striped;
import com.sequenceiq.cloudbreak.common.model.user.IdentityUser;
import com.sequenceiq.cloudbreak.common.model.user.IdentityUserRole;
import com.sequenceiq.cloudbreak.controller.BadRequestException;
import com.sequenceiq.cloudbreak.domain.AccountPreferences;
import com.sequenceiq.cloudbreak.repository.AccountPreferencesRepository;

@Service
public class SimpleAccountPreferencesService implements AccountPreferencesService {

    private static final long ZERO = 0L;

    private static final int STRIPES = 10;

    @Value("${cb.enabledplatforms:}")
    private String enabledPlatforms;

    @Inject
    private AccountPreferencesRepository repository;

    private final Striped<Lock> locks = Striped.lazyWeakLock(STRIPES);

    @Override
    public AccountPreferences save(AccountPreferences accountPreferences) {
        return repository.save(accountPreferences);
    }

    @Override
    @PostAuthorize("hasPermission(returnObject,'read')")
    public AccountPreferences saveOne(IdentityUser user, AccountPreferences accountPreferences) {
        accountPreferences.setAccount(user.getAccount());
        return repository.save(accountPreferences);
    }

    @Override
    public AccountPreferences get(Long id) {
        return repository.findOne(id);
    }

    @Override
    public Boolean isPlatformSelectionDisabled() {
        return !StringUtils.isEmpty(enabledPlatforms);
    }

    @Override
    public AccountPreferences getByAccount(String account) {
        Lock lock = locks.get(account);
        lock.lock();
        try {
            AccountPreferences accountPreferences = repository.findByAccount(account);
            if (accountPreferences == null) {
                accountPreferences = createDefaultAccountPreferences(account);
            }
            if (!StringUtils.isEmpty(enabledPlatforms)) {
                accountPreferences.setPlatforms(enabledPlatforms);
            }
            return accountPreferences;
        } finally {
            lock.unlock();
        }
    }

    @Override
    @PostAuthorize("hasPermission(returnObject,'read')")
    public AccountPreferences getOneById(Long id, IdentityUser user) {
        AccountPreferences accountPreferences = repository.findOne(id);
        if (!user.getRoles().contains(IdentityUserRole.ADMIN)) {
            throw new BadRequestException("AccountPreferences are only available for admin users!");
        } else if (accountPreferences == null) {
            throw new BadRequestException(String.format("AccountPreferences could not find with id: %s", id));
        } else if (!accountPreferences.getAccount().equals(user.getAccount())) {
            throw new BadRequestException("AccountPreferences are only available for the owner admin user!");
        } else {
            return accountPreferences;
        }
    }

    @Override
    public AccountPreferences getOneByAccount(IdentityUser user) {
        String account = user.getAccount();
        return getByAccount(account);
    }

    @Override
    public void delete(IdentityUser user) {
        AccountPreferences preferences = getOneByAccount(user);
        repository.delete(preferences);
    }

    private AccountPreferences createDefaultAccountPreferences(String account) {
        AccountPreferences defaultPreferences = new AccountPreferences();
        defaultPreferences.setAccount(account);
        defaultPreferences.setMaxNumberOfClusters(ZERO);
        defaultPreferences.setMaxNumberOfNodesPerCluster(ZERO);
        defaultPreferences.setMaxNumberOfClustersPerUser(ZERO);
        defaultPreferences.setAllowedInstanceTypes(Collections.emptyList());
        defaultPreferences.setClusterTimeToLive(ZERO);
        defaultPreferences.setUserTimeToLive(ZERO);
        return repository.save(defaultPreferences);
    }
}
