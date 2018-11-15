package com.sequenceiq.cloudbreak.controller;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.transaction.Transactional;
import javax.transaction.Transactional.TxType;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.convert.ConversionService;
import org.springframework.stereotype.Controller;

import com.google.common.collect.ImmutableMap;
import com.sequenceiq.cloudbreak.api.endpoint.v1.AccountPreferencesEndpoint;
import com.sequenceiq.cloudbreak.api.model.AccountPreferencesResponse;
import com.sequenceiq.cloudbreak.api.model.SupportedExternalDatabaseServiceEntryResponse;
import com.sequenceiq.cloudbreak.service.RestRequestThreadLocalService;
import com.sequenceiq.cloudbreak.service.account.PreferencesService;
import com.sequenceiq.cloudbreak.validation.externaldatabase.SupportedDatabaseProvider;

@Controller
@Transactional(TxType.NEVER)
public class AccountPreferencesController implements AccountPreferencesEndpoint {

    @Autowired
    private PreferencesService preferencesService;

    @Autowired
    @Qualifier("conversionService")
    private ConversionService conversionService;

    @Inject
    private RestRequestThreadLocalService restRequestThreadLocalService;

    @Override
    public AccountPreferencesResponse get() {
        AccountPreferencesResponse response = new AccountPreferencesResponse();
        response.setFeatureSwitches(preferencesService.getFeatureSwitches());
        Set<SupportedExternalDatabaseServiceEntryResponse> supportedExternalDatabases = SupportedDatabaseProvider.supportedExternalDatabases()
                .stream()
                .map(db -> conversionService.convert(db, SupportedExternalDatabaseServiceEntryResponse.class))
                .collect(Collectors.toSet());
        response.setSupportedExternalDatabases(supportedExternalDatabases);
        return response;
    }

    @Override
    public Map<String, Boolean> isPlatformSelectionDisabled() {
        return ImmutableMap.of("disabled", preferencesService.isPlatformSelectionDisabled());
    }

    @Override
    public Map<String, Boolean> platformEnablement() {
        return preferencesService.platformEnablement();
    }
}
