package com.sequenceiq.cloudbreak.controller;

import java.util.Map;

import javax.inject.Inject;
import javax.transaction.Transactional;
import javax.transaction.Transactional.TxType;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.convert.ConversionService;
import org.springframework.stereotype.Controller;

import com.google.common.collect.ImmutableMap;
import com.sequenceiq.cloudbreak.api.endpoint.v1.AccountPreferencesEndpoint;
import com.sequenceiq.cloudbreak.api.model.AccountPreferencesRequest;
import com.sequenceiq.cloudbreak.api.model.AccountPreferencesResponse;
import com.sequenceiq.cloudbreak.common.model.user.CloudbreakUser;
import com.sequenceiq.cloudbreak.domain.AccountPreferences;
import com.sequenceiq.cloudbreak.service.RestRequestThreadLocalService;
import com.sequenceiq.cloudbreak.service.account.AccountPreferencesService;

@Controller
@Transactional(TxType.NEVER)
public class AccountPreferencesController implements AccountPreferencesEndpoint {

    @Autowired
    private AccountPreferencesService accountPreferencesService;

    @Autowired
    @Qualifier("conversionService")
    private ConversionService conversionService;

    @Inject
    private RestRequestThreadLocalService restRequestThreadLocalService;

    @Override
    public AccountPreferencesResponse get() {
        CloudbreakUser user = restRequestThreadLocalService.getCloudbreakUser();
        AccountPreferences preferences = accountPreferencesService.getByUser(user);
        return convert(preferences);
    }

    @Override
    public AccountPreferencesResponse put(AccountPreferencesRequest updateRequest) {
        CloudbreakUser user = restRequestThreadLocalService.getCloudbreakUser();
        return convert(accountPreferencesService.saveOne(user, convert(updateRequest)));
    }

    @Override
    public AccountPreferencesResponse post(AccountPreferencesRequest updateRequest) {
        CloudbreakUser user = restRequestThreadLocalService.getCloudbreakUser();
        return convert(accountPreferencesService.saveOne(user, convert(updateRequest)));
    }

    @Override
    public Map<String, Boolean> isPlatformSelectionDisabled() {
        return ImmutableMap.of("disabled", accountPreferencesService.isPlatformSelectionDisabled());
    }

    @Override
    public Map<String, Boolean> platformEnablement() {
        return accountPreferencesService.platformEnablement();
    }

    private AccountPreferencesResponse convert(AccountPreferences preferences) {
        return conversionService.convert(preferences, AccountPreferencesResponse.class);
    }

    private AccountPreferences convert(AccountPreferencesRequest preferences) {
        return conversionService.convert(preferences, AccountPreferences.class);
    }
}
