package com.sequenceiq.cloudbreak.service.account;

import static com.sequenceiq.cloudbreak.controller.exception.NotFoundException.notFound;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.google.common.collect.Sets;
import com.sequenceiq.cloudbreak.cloud.CloudConstant;
import com.sequenceiq.cloudbreak.common.model.user.IdentityUser;
import com.sequenceiq.cloudbreak.common.model.user.IdentityUserRole;
import com.sequenceiq.cloudbreak.controller.exception.BadRequestException;
import com.sequenceiq.cloudbreak.domain.AccountPreferences;
import com.sequenceiq.cloudbreak.repository.AccountPreferencesRepository;
import com.sequenceiq.cloudbreak.service.AuthorizationService;

@Service
public class SimpleAccountPreferencesService implements AccountPreferencesService {

    private static final long ZERO = 0L;

    @Value("${cb.enabledplatforms:}")
    private String enabledPlatforms;

    @Inject
    private AccountPreferencesRepository repository;

    @Inject
    private List<CloudConstant> cloudConstants;

    @Inject
    private AuthorizationService authorizationService;

    @Override
    public AccountPreferences save(AccountPreferences accountPreferences) {
        return repository.save(accountPreferences);
    }

    @Override
    public AccountPreferences saveOne(IdentityUser user, AccountPreferences accountPreferences) {
        accountPreferences.setAccount(user.getAccount());
        authorizationService.hasReadPermission(accountPreferences);
        return repository.save(accountPreferences);
    }

    @Override
    public AccountPreferences get(Long id) {
        return repository.findById(id).orElseThrow(notFound("Account preferences", id));
    }

    @Override
    public Boolean isPlatformSelectionDisabled() {
        return !StringUtils.isEmpty(enabledPlatforms);
    }

    @Override
    public Set<String> enabledPlatforms() {
        Set<String> platforms;
        if (enabledPlatforms.isEmpty()) {
            platforms = cloudConstants.stream()
                    .map(cloudConstant -> cloudConstant.platform().value())
                    .collect(Collectors.toSet());
        } else {
            platforms = Sets.newHashSet(enabledPlatforms.split(","));
        }
        return platforms;
    }

    @Override
    public Map<String, Boolean> platformEnablement() {
        Map<String, Boolean> result = new HashMap<>();
        if (StringUtils.isEmpty(enabledPlatforms)) {
            for (CloudConstant cloudConstant : cloudConstants) {
                result.put(cloudConstant.platform().value(), true);
            }
        } else {
            for (String platform : enabledPlatforms()) {
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
    public AccountPreferences getOneById(Long id, IdentityUser user) {
        AccountPreferences accountPreferences = get(id);
        if (!user.getRoles().contains(IdentityUserRole.ADMIN)) {
            throw new BadRequestException("AccountPreferences are only available for admin users!");
        } else if (!accountPreferences.getAccount().equals(user.getAccount())) {
            throw new BadRequestException("AccountPreferences are only available for the owner admin user!");
        }
        authorizationService.hasReadPermission(accountPreferences);
        return accountPreferences;
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
