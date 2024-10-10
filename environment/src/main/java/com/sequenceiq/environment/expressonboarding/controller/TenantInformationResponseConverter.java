package com.sequenceiq.environment.expressonboarding.controller;

import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.environment.api.v1.expressonboarding.model.response.TenantInformationResponse;

@Service
public class TenantInformationResponseConverter {

    public TenantInformationResponse tenantInformationResponse() {
        String accountId = ThreadBasedUserCrnProvider.getAccountId();

        TenantInformationResponse tenantInformationResponse = new TenantInformationResponse();
        tenantInformationResponse.setTenantId(accountId);
        return tenantInformationResponse;
    }
}
