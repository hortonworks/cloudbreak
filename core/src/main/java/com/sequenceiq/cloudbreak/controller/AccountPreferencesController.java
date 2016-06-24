package com.sequenceiq.cloudbreak.controller;

import javax.ws.rs.core.Response;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.convert.ConversionService;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.AccountPreferencesEndpoint;
import com.sequenceiq.cloudbreak.api.model.AccountPreferencesJson;
import com.sequenceiq.cloudbreak.common.type.CbUserRole;
import com.sequenceiq.cloudbreak.domain.AccountPreferences;
import com.sequenceiq.cloudbreak.domain.CbUser;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
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
    public AccountPreferencesJson get() {
        CbUser user = authenticatedUserService.getCbUser();
        MDCBuilder.buildUserMdcContext(user);
        AccountPreferences preferences = service.getOneByAccount(user);
        return convert(preferences);
    }

    @Override
    public void put(AccountPreferencesJson updateRequest) {
        CbUser user = authenticatedUserService.getCbUser();
        MDCBuilder.buildUserMdcContext(user);
        service.saveOne(user, convert(updateRequest));
    }

    @Override
    public void post(AccountPreferencesJson updateRequest) {
        CbUser user = authenticatedUserService.getCbUser();
        MDCBuilder.buildUserMdcContext(user);
        service.saveOne(user, convert(updateRequest));
    }

    @Override
    public Response validate() {
        CbUser user = authenticatedUserService.getCbUser();
        MDCBuilder.buildUserMdcContext(user);
        if (user.getRoles().contains(CbUserRole.ADMIN)) {
            validator.validate();
        }
        return Response.status(Response.Status.ACCEPTED).build();
    }

    private AccountPreferencesJson convert(AccountPreferences preferences) {
        return conversionService.convert(preferences, AccountPreferencesJson.class);
    }

    private AccountPreferences convert(AccountPreferencesJson preferences) {
        return conversionService.convert(preferences, AccountPreferences.class);
    }
}
