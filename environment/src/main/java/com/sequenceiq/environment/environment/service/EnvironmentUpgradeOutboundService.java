package com.sequenceiq.environment.environment.service;

import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.StackV4Endpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.upgrade.StackOutboundTypeValidationV4Response;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.common.api.type.OutboundType;
import com.sequenceiq.environment.api.v1.environment.model.response.OutboundTypeValidationResponse;
import com.sequenceiq.environment.environment.service.freeipa.FreeIpaService;

@Service
public class EnvironmentUpgradeOutboundService {

    private final FreeIpaService freeIpaService;

    private final StackV4Endpoint stackV4Endpoint;

    public EnvironmentUpgradeOutboundService(FreeIpaService freeIpaService, StackV4Endpoint stackV4Endpoint) {
        this.freeIpaService = freeIpaService;
        this.stackV4Endpoint = stackV4Endpoint;
    }

    public OutboundTypeValidationResponse validateOutboundTypes(String crn) {
        OutboundType ipaOutboundType = freeIpaService.getNetworkOutbound(crn);
        String initiatorUserCrn = ThreadBasedUserCrnProvider.getUserCrn();
        return ThreadBasedUserCrnProvider.doAsInternalActor(
                () -> {
                    StackOutboundTypeValidationV4Response defaultOutboundResponse = stackV4Endpoint.validateStackOutboundTypes(0L, crn,
                            initiatorUserCrn);
                    OutboundTypeValidationResponse response = new OutboundTypeValidationResponse();
                    response.setMessage(defaultOutboundResponse.getMessage());
                    response.setIpaOutboundType(ipaOutboundType);
                    response.setStackOutboundTypeMap(defaultOutboundResponse.getStackOutboundTypeMap());
                    return response;
                });
    }
}
