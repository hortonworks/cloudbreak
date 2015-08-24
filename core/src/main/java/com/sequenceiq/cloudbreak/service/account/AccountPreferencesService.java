package com.sequenceiq.cloudbreak.service.account;


import com.sequenceiq.cloudbreak.domain.AccountPreferences;
import com.sequenceiq.cloudbreak.domain.CbUser;

public interface AccountPreferencesService {

    AccountPreferences save(AccountPreferences accountPreferences);

    AccountPreferences saveOne(CbUser user, AccountPreferences accountPreferences);

    AccountPreferences get(Long id);

    AccountPreferences getByAccount(String account);

    AccountPreferences getOneById(Long id, CbUser user);

    AccountPreferences getOneByAccount(CbUser user);

    void delete(CbUser user);

}
