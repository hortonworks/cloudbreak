package com.sequenceiq.cloudbreak.controller;

import java.util.Map;

import javax.inject.Inject;
import javax.transaction.Transactional;
import javax.transaction.Transactional.TxType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.convert.ConversionService;
import org.springframework.stereotype.Controller;

import com.google.common.collect.ImmutableMap;
import com.sequenceiq.cloudbreak.api.endpoint.v1.AccountPreferencesEndpoint;
import com.sequenceiq.cloudbreak.api.model.AccountPreferencesRequest;
import com.sequenceiq.cloudbreak.api.model.AccountPreferencesResponse;
import com.sequenceiq.cloudbreak.common.model.user.IdentityUser;
import com.sequenceiq.cloudbreak.common.model.user.IdentityUserRole;
import com.sequenceiq.cloudbreak.domain.AccountPreferences;
import com.sequenceiq.cloudbreak.service.RestRequestThreadLocalService;
import com.sequenceiq.cloudbreak.service.account.AccountPreferencesService;
import com.sequenceiq.cloudbreak.service.account.ScheduledAccountPreferencesValidator;

@Controller
@Transactional(TxType.NEVER)
public class AccountPreferencesController implements AccountPreferencesEndpoint {

    @Autowired
    private AccountPreferencesService accountPreferencesService;

    @Autowired
    private ScheduledAccountPreferencesValidator validator;

    @Autowired
    @Qualifier("conversionService")
    private ConversionService conversionService;

    @Inject
    private RestRequestThreadLocalService restRequestThreadLocalService;

    @Override
    public AccountPreferencesResponse get() {
        IdentityUser user = restRequestThreadLocalService.getIdentityUser();
        AccountPreferences preferences = accountPreferencesService.getByUser(user);
        return convert(preferences);
    }

    @Override
    public AccountPreferencesResponse put(AccountPreferencesRequest updateRequest) {
        IdentityUser user = restRequestThreadLocalService.getIdentityUser();
        return convert(accountPreferencesService.saveOne(user, convert(updateRequest)));
    }

    @Override
    public AccountPreferencesResponse post(AccountPreferencesRequest updateRequest) {
        IdentityUser user = restRequestThreadLocalService.getIdentityUser();
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

    @Override
    public Response validate() {
        IdentityUser user = restRequestThreadLocalService.getIdentityUser();
        if (user.getRoles().contains(IdentityUserRole.ADMIN)) {
            validator.validate();
        }
        return Response.status(Status.NO_CONTENT).build();
    }

    private AccountPreferencesResponse convert(AccountPreferences preferences) {
        return conversionService.convert(preferences, AccountPreferencesResponse.class);
    }

    private AccountPreferences convert(AccountPreferencesRequest preferences) {
        return conversionService.convert(preferences, AccountPreferences.class);
    }
}
