package com.sequenceiq.datalake.controller.sdx;

import jakarta.inject.Inject;

import org.springframework.stereotype.Controller;

import com.sequenceiq.authorization.annotation.AccountIdNotNeeded;
import com.sequenceiq.authorization.annotation.InternalOnly;
import com.sequenceiq.datalake.configuration.CDPConfigService;
import com.sequenceiq.sdx.api.endpoint.SupportV1Endpoint;
import com.sequenceiq.sdx.api.model.support.DatalakePlatformSupportRequirements;

@Controller
public class SupportV1Controller implements SupportV1Endpoint {

    @Inject
    private CDPConfigService cdpConfigService;

    @Override
    @InternalOnly
    @AccountIdNotNeeded
    public DatalakePlatformSupportRequirements getInstanceTypesByPlatform(String cloudPlatform) {
        return cdpConfigService.collectRequirementByPlatform(cloudPlatform);
    }
}
