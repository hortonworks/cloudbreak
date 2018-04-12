package com.sequenceiq.cloudbreak.service.account;


import com.sequenceiq.cloudbreak.common.model.user.IdentityUser;
import com.sequenceiq.cloudbreak.domain.AccountPreferences;

import java.util.Map;
import java.util.Set;

public interface AccountPreferencesService {

    AccountPreferences save(AccountPreferences accountPreferences);

    AccountPreferences saveOne(IdentityUser user, AccountPreferences accountPreferences);

    AccountPreferences get(Long id);

    Boolean isPlatformSelectionDisabled();

    Map<String, Boolean> platformEnablement();

    Set<String> enabledPlatforms();

    AccountPreferences getByAccount(String account);

    AccountPreferences getOneById(Long id, IdentityUser user);

    AccountPreferences getOneByAccount(IdentityUser user);

    void delete(IdentityUser user);

}
