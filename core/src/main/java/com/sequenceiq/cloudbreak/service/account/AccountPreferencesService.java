package com.sequenceiq.cloudbreak.service.account;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import com.google.common.collect.Sets;
import com.sequenceiq.cloudbreak.cloud.CloudConstant;
import com.sequenceiq.cloudbreak.common.model.user.IdentityUser;
import com.sequenceiq.cloudbreak.domain.AccountPreferences;
import com.sequenceiq.cloudbreak.repository.AccountPreferencesRepository;
import com.sequenceiq.cloudbreak.service.AuthorizationService;

@Service
public class AccountPreferencesService {

    private static final long ZERO = 0L;

    @Value("${cb.enabledplatforms:}")
    private String enabledPlatforms;

    @Inject
    private AccountPreferencesRepository accountPreferencesRepository;

    @Inject
    private List<CloudConstant> cloudConstants;

    @Inject
    private AuthorizationService authorizationService;

    public AccountPreferences save(AccountPreferences accountPreferences) {
        return accountPreferencesRepository.save(accountPreferences);
    }

    public AccountPreferences saveOne(IdentityUser user, AccountPreferences accountPreferences) {
        accountPreferences.setAccount(user.getAccount());
        return save(accountPreferences);
    }

    public Boolean isPlatformSelectionDisabled() {
        return !StringUtils.isEmpty(enabledPlatforms);
    }

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

    public AccountPreferences getByAccount(String account) {
        AccountPreferences accountPreferences = getSilently(account);
        if (accountPreferences == null) {
            accountPreferences = createDefaultAccountPreferences(account);
        }
        if (!StringUtils.isEmpty(enabledPlatforms)) {
            accountPreferences.setPlatforms(enabledPlatforms);
        }
        return accountPreferences;
    }

    public AccountPreferences getByUser(IdentityUser user) {
        String account = user.getAccount();
        return getByAccount(account);
    }

    public void delete(IdentityUser user) {
        AccountPreferences preferences = getByUser(user);
        accountPreferencesRepository.delete(preferences);
    }

    private AccountPreferences getSilently(String id) {
        try {
            return accountPreferencesRepository.findById(id).orElse(null);
        } catch (AccessDeniedException ignore) {
            return null;
        }
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
