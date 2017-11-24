package com.sequenceiq.cloudbreak.service.account;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.prepost.PostAuthorize;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.cloud.CloudConstant;
import com.sequenceiq.cloudbreak.common.model.user.IdentityUser;
import com.sequenceiq.cloudbreak.common.model.user.IdentityUserRole;
import com.sequenceiq.cloudbreak.controller.BadRequestException;
import com.sequenceiq.cloudbreak.domain.AccountPreferences;
import com.sequenceiq.cloudbreak.repository.AccountPreferencesRepository;

@Service
public class SimpleAccountPreferencesService implements AccountPreferencesService {

    private static final long ZERO = 0L;

    @Value("${cb.enabledplatforms:}")
    private String enabledPlatforms;

    @Inject
    private AccountPreferencesRepository repository;

    @Inject
    private List<CloudConstant> cloudConstants;

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
    public Map<String, Boolean> platformEnablement() {
        Map<String, Boolean> result = new HashMap<>();
        if (StringUtils.isEmpty(enabledPlatforms)) {
            for (CloudConstant cloudConstant : cloudConstants) {
                result.put(cloudConstant.platform().value(), true);
            }
        } else {
            for (String platform : enabledPlatforms.split(",")) {
                result.put(platform, true);
            }
            for (CloudConstant cloudConstant : cloudConstants) {
                if (!result.keySet().contains(cloudConstant.platform().value())) {
                    result.put(cloudConstant.platform().value(), false);
                }
            }
        }
        return result;
    }

    @Override
    public AccountPreferences getByAccount(String account) {
        AccountPreferences accountPreferences = repository.findByAccount(account);
        if (accountPreferences == null) {
            accountPreferences = createDefaultAccountPreferences(account);
        }
        if (!StringUtils.isEmpty(enabledPlatforms)) {
            accountPreferences.setPlatforms(enabledPlatforms);
        }
        return accountPreferences;
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
        return defaultPreferences;
    }
}
