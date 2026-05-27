package com.sequenceiq.freeipa.controller;

import jakarta.inject.Inject;

import org.springframework.stereotype.Controller;

import com.sequenceiq.authorization.annotation.AccountIdNotNeeded;
import com.sequenceiq.authorization.annotation.InternalOnly;
import com.sequenceiq.freeipa.api.v1.support.SupportV1Endpoint;
import com.sequenceiq.freeipa.api.v1.support.response.FreeIpaPlatformSupportRequirements;
import com.sequenceiq.freeipa.service.cloud.PlatformParameterService;
import com.sequenceiq.freeipa.service.stack.instance.DefaultInstanceTypeProvider;

@Controller
public class SupportV1Controller implements SupportV1Endpoint {

    @Inject
    private DefaultInstanceTypeProvider defaultInstanceTypeProvider;

    @Inject
    private PlatformParameterService platformParameterService;

    @Override
    @InternalOnly
    @AccountIdNotNeeded
    public FreeIpaPlatformSupportRequirements getInstanceTypesByPlatform(String cloudPlatform) {
        throw new UnsupportedOperationException("This endpoint is not supported anymore. " +
                "Please use the /region endpoint with the appropriate query parameters to " +
                "get the supported instance types for FreeIPA.");
    }
}
