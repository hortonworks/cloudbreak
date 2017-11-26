package com.sequenceiq.cloudbreak.service.stack;

import java.util.Map;
import java.util.function.Supplier;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.google.common.base.Suppliers;
import com.sequenceiq.cloudbreak.cloud.PlatformParameters;
import com.sequenceiq.cloudbreak.cloud.model.Platform;

@Component
public class CloudParameterCache {
    @Inject
    private CloudParameterService cloudParameterService;

    private final Supplier<Map<Platform, PlatformParameters>> platformParameters = Suppliers.memoize(() -> cloudParameterService.getPlatformParameters());

    public Map<Platform, PlatformParameters> getPlatformParameters() {
        return platformParameters.get();
    }
}
