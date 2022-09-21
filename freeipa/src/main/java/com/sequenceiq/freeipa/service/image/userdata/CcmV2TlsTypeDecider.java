package com.sequenceiq.freeipa.service.image.userdata;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.common.api.type.CcmV2TlsType;
import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;

@Component
public class CcmV2TlsTypeDecider {

    @Inject
    private EntitlementService entitlementService;

    public CcmV2TlsType decide(DetailedEnvironmentResponse environment) {
        return CcmV2TlsType.TWO_WAY_TLS == environment.getCcmV2TlsType() ||
                environment.getCcmV2TlsType() == null && !entitlementService.ccmV2UseOneWayTls(environment.getAccountId())
                ? CcmV2TlsType.TWO_WAY_TLS
                : CcmV2TlsType.ONE_WAY_TLS;
    }

}
