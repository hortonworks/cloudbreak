package com.sequenceiq.cloudbreak.service.stack;

import com.google.common.base.Suppliers;
import com.sequenceiq.cloudbreak.cloud.PlatformParameters;
import com.sequenceiq.cloudbreak.cloud.PlatformParametersConsts;
import com.sequenceiq.cloudbreak.cloud.model.Platform;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.util.Map;
import java.util.function.Supplier;

@Component
public class CloudParameterCache {
    @Inject
    private CloudParameterService cloudParameterService;

    private final Supplier<Map<Platform, PlatformParameters>> platformParameters = Suppliers.memoize(() -> cloudParameterService.getPlatformParameters());

    public Map<Platform, PlatformParameters> getPlatformParameters() {
        return platformParameters.get();
    }

    public boolean isScalingSupported(String platform) {
        return isScalingSupported(Platform.platform(platform));
    }

    public boolean isScalingSupported(Platform platform) {
        Boolean result = getSpecialParameters(platform).get(PlatformParametersConsts.SCALING_SUPPORTED);
        return result == null || result;
    }

    public boolean isStartStopSupported(String platform) {
        return isStartStopSupported(Platform.platform(platform));
    }

    public boolean isStartStopSupported(Platform platform) {
        Boolean result = getSpecialParameters(platform).get(PlatformParametersConsts.STARTSTOP_SUPPORTED);
        return result == null || result;
    }

    private Map<String, Boolean> getSpecialParameters(Platform platform) {
        PlatformParameters platformParameters = getPlatformParameters().get(platform);
        if (platformParameters == null) {
            throw new IllegalArgumentException(String.format("%s platform is not supported", platform));
        }
        return platformParameters.specialParameters().getSpecialParameters();
    }
}
