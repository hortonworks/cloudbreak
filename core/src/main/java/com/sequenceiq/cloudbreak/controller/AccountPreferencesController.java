package com.sequenceiq.cloudbreak.controller;

import java.util.Map;

import javax.inject.Inject;
import javax.transaction.Transactional;
import javax.transaction.Transactional.TxType;
import javax.ws.rs.core.Response;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.convert.ConversionService;
import org.springframework.stereotype.Component;

import com.google.common.collect.ImmutableMap;
import com.sequenceiq.cloudbreak.api.endpoint.v1.AccountPreferencesEndpoint;
import com.sequenceiq.cloudbreak.api.model.AccountPreferencesRequest;
import com.sequenceiq.cloudbreak.api.model.AccountPreferencesResponse;
import com.sequenceiq.cloudbreak.service.CloudPlarformService;
import com.sequenceiq.cloudbreak.service.RestRequestThreadLocalService;

@Component
@Transactional(TxType.NEVER)
public class AccountPreferencesController implements AccountPreferencesEndpoint {

    @Autowired
    @Qualifier("conversionService")
    private ConversionService conversionService;

    @Inject
    private RestRequestThreadLocalService restRequestThreadLocalService;

    @Inject
    private CloudPlarformService cloudPlarformService;

    @Override
    public AccountPreferencesResponse get() {
        throw new UnsupportedOperationException("AccountPreferences no longer exist.");
    }

    @Override
    public AccountPreferencesResponse put(AccountPreferencesRequest updateRequest) {
        throw new UnsupportedOperationException("AccountPreferences no longer exist.");
    }

    @Override
    public AccountPreferencesResponse post(AccountPreferencesRequest updateRequest) {
        throw new UnsupportedOperationException("AccountPreferences no longer exist.");
    }

    @Override
    public Map<String, Boolean> isPlatformSelectionDisabled() {
        return ImmutableMap.of("disabled", cloudPlarformService.isPlatformSelectionDisabled());
    }

    @Override
    public Map<String, Boolean> platformEnablement() {
        return cloudPlarformService.platformEnablement();
    }

    @Override
    public Response validate() {
        throw new UnsupportedOperationException("AccountPreferences no longer exist.");
    }
}