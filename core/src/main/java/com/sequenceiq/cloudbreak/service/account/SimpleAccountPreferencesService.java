package com.sequenceiq.cloudbreak.service.account;

import javax.inject.Inject;

import java.util.Collections;
import java.util.concurrent.locks.Lock;

import com.google.common.util.concurrent.Striped;
import com.sequenceiq.cloudbreak.controller.BadRequestException;
import com.sequenceiq.cloudbreak.domain.AccountPreferences;
import com.sequenceiq.cloudbreak.domain.CbUser;
import com.sequenceiq.cloudbreak.common.type.CbUserRole;
import com.sequenceiq.cloudbreak.repository.AccountPreferencesRepository;
import org.springframework.security.access.prepost.PostAuthorize;
import org.springframework.stereotype.Service;

@Service
public class SimpleAccountPreferencesService implements AccountPreferencesService {
    private static final long ZERO = 0L;
    private static final int STRIPES = 10;

    @Inject
    private AccountPreferencesRepository repository;

    private Striped<Lock> locks = Striped.lazyWeakLock(STRIPES);

    @Override
    public AccountPreferences save(AccountPreferences accountPreferences) {
        return repository.save(accountPreferences);
    }

    @Override
    @PostAuthorize("hasPermission(returnObject,'read')")
    public AccountPreferences saveOne(CbUser user, AccountPreferences accountPreferences) {
        accountPreferences.setAccount(user.getAccount());
        return repository.save(accountPreferences);
    }

    @Override
    public AccountPreferences get(Long id) {
        return repository.findOne(id);
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
            return accountPreferences;
        } finally {
            lock.unlock();
        }
    }

    @Override
    @PostAuthorize("hasPermission(returnObject,'read')")
    public AccountPreferences getOneById(Long id, CbUser user) {
        AccountPreferences accountPreferences = repository.findOne(id);
        if (!user.getRoles().contains(CbUserRole.ADMIN)) {
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
    @PostAuthorize("hasPermission(returnObject,'read')")
    public AccountPreferences getOneByAccount(CbUser user) {
        String account = user.getAccount();
        Lock lock = locks.get(account);
        lock.lock();
        try {
            AccountPreferences accountPreferences = repository.findByAccount(account);

            if (accountPreferences == null) {
                accountPreferences = createDefaultAccountPreferences(account);
            }
            if (!user.getRoles().contains(CbUserRole.ADMIN)) {
                throw new BadRequestException("AccountPreferences are only available for admin users!");
            } else {
                return accountPreferences;
            }
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void delete(CbUser user) {
        AccountPreferences preferences = getOneByAccount(user);
        repository.delete(preferences);
    }

    private AccountPreferences createDefaultAccountPreferences(String account) {
        AccountPreferences defaultPreferences = new AccountPreferences();
        defaultPreferences.setAccount(account);
        defaultPreferences.setMaxNumberOfClusters(ZERO);
        defaultPreferences.setMaxNumberOfNodesPerCluster(ZERO);
        defaultPreferences.setMaxNumberOfClustersPerUser(ZERO);
        defaultPreferences.setAllowedInstanceTypes(Collections.<String>emptyList());
        defaultPreferences.setClusterTimeToLive(ZERO);
        defaultPreferences.setUserTimeToLive(ZERO);
        return repository.save(defaultPreferences);
    }

}
