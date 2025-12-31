package com.sequenceiq.freeipa.controller;

import java.util.Set;

import jakarta.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Controller;

import com.sequenceiq.authorization.annotation.AccountIdNotNeeded;
import com.sequenceiq.authorization.annotation.InternalOnly;
import com.sequenceiq.common.model.Architecture;
import com.sequenceiq.freeipa.api.v1.support.SupportV1Endpoint;
import com.sequenceiq.freeipa.api.v1.support.response.FreeIpaPlatformSupportRequirements;
import com.sequenceiq.freeipa.service.stack.instance.DefaultInstanceTypeProvider;

@Controller
public class SupportV1Controller implements SupportV1Endpoint {

    @Inject
    private DefaultInstanceTypeProvider defaultInstanceTypeProvider;

    @Override
    @InternalOnly
    @AccountIdNotNeeded
    public FreeIpaPlatformSupportRequirements getInstanceTypesByPlatform(String cloudPlatform) {
        FreeIpaPlatformSupportRequirements freeIpaPlatformSupportRequirements = new FreeIpaPlatformSupportRequirements();
        if (StringUtils.isNotBlank(cloudPlatform)) {
            String armInstance = defaultInstanceTypeProvider.getForPlatform(cloudPlatform, Architecture.ARM64);
            if (StringUtils.isNotBlank(armInstance)) {
                freeIpaPlatformSupportRequirements.setDefaultArmInstanceTypeRequirements(
                        Set.of(armInstance));
            }

            String x86Instance = defaultInstanceTypeProvider.getForPlatform(cloudPlatform, Architecture.X86_64);
            if (StringUtils.isNotBlank(x86Instance)) {
                freeIpaPlatformSupportRequirements.setDefaultX86InstanceTypeRequirements(
                        Set.of(x86Instance));
            }
        }
        return freeIpaPlatformSupportRequirements;
    }
}
