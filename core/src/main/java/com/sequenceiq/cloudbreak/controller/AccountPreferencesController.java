package com.sequenceiq.cloudbreak.controller;

import java.util.Map;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.convert.ConversionService;
import org.springframework.stereotype.Component;

import com.google.common.collect.ImmutableMap;
import com.sequenceiq.cloudbreak.api.endpoint.v1.AccountPreferencesEndpoint;
import com.sequenceiq.cloudbreak.api.model.AccountPreferencesRequest;
import com.sequenceiq.cloudbreak.api.model.AccountPreferencesResponse;
import com.sequenceiq.cloudbreak.common.model.user.IdentityUser;
import com.sequenceiq.cloudbreak.common.model.user.IdentityUserRole;
import com.sequenceiq.cloudbreak.domain.AccountPreferences;
import com.sequenceiq.cloudbreak.service.account.AccountPreferencesService;
import com.sequenceiq.cloudbreak.service.account.ScheduledAccountPreferencesValidator;

@Component
public class AccountPreferencesController implements AccountPreferencesEndpoint {

    @Autowired
    private AccountPreferencesService service;

    @Autowired
    private ScheduledAccountPreferencesValidator validator;

    @Autowired
    private AuthenticatedUserService authenticatedUserService;

    @Autowired
    @Qualifier("conversionService")
    private ConversionService conversionService;

    @Override
    public AccountPreferencesResponse get() {
        IdentityUser user = authenticatedUserService.getCbUser();
        AccountPreferences preferences = service.getOneByAccount(user);
        return convert(preferences);
    }

    @Override
    public AccountPreferencesResponse put(AccountPreferencesRequest updateRequest) {
        IdentityUser user = authenticatedUserService.getCbUser();
        return convert(service.saveOne(user, convert(updateRequest)));
    }

    @Override
    public AccountPreferencesResponse post(AccountPreferencesRequest updateRequest) {
        IdentityUser user = authenticatedUserService.getCbUser();
        return convert(service.saveOne(user, convert(updateRequest)));
    }

    @Override
    public Map<String, Boolean> isPlatformSelectionDisabled() {
        return ImmutableMap.of("disabled", service.isPlatformSelectionDisabled());
    }

    @Override
    public Map<String, Boolean> platformEnablement() {
        return service.platformEnablement();
    }

    @Override
    public Response validate() {
        IdentityUser user = authenticatedUserService.getCbUser();
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
