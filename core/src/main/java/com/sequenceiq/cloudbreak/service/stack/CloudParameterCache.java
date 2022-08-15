package com.sequenceiq.cloudbreak.service.stack;

import java.util.Map;
import java.util.function.Supplier;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.google.common.base.Suppliers;
import com.sequenceiq.cloudbreak.cloud.PlatformParameters;
import com.sequenceiq.cloudbreak.cloud.PlatformParametersConsts;
import com.sequenceiq.cloudbreak.cloud.model.Platform;
import com.sequenceiq.cloudbreak.cloud.service.CloudParameterService;

@Component
public class CloudParameterCache {
    @Inject
    private CloudParameterService cloudParameterService;

    private final Supplier<Map<Platform, PlatformParameters>> platformParameters = Suppliers.memoize(() -> cloudParameterService.getPlatformParameters());

    public Map<Platform, PlatformParameters> getPlatformParameters() {
        return platformParameters.get();
    }

    public boolean isUpScalingSupported(String platform) {
        return isUpScalingSupported(Platform.platform(platform));
    }

    public boolean isVerticalScalingSupported(String platform) {
        return isVerticalScalingSupported(Platform.platform(platform));
    }

    public boolean isVerticalScalingSupported(Platform platform) {
        Boolean result = getSpecialParameters(platform).get(PlatformParametersConsts.VERTICAL_SCALING_SUPPORTED);
        return result == null || result;
    }

    public boolean isUpScalingSupported(Platform platform) {
        Boolean result = getSpecialParameters(platform).get(PlatformParametersConsts.UPSCALING_SUPPORTED);
        return result == null || result;
    }

    public boolean isDownScalingSupported(String platform) {
        return isDownScalingSupported(Platform.platform(platform));
    }

    public boolean isDownScalingSupported(Platform platform) {
        Boolean result = getSpecialParameters(platform).get(PlatformParametersConsts.DOWNSCALING_SUPPORTED);
        return result == null || result;
    }

    public boolean isStartStopSupported(String platform) {
        return isStartStopSupported(Platform.platform(platform));
    }

    public boolean isStartStopSupported(Platform platform) {
        Boolean result = getSpecialParameters(platform).get(PlatformParametersConsts.STARTSTOP_SUPPORTED);
        return result == null || result;
    }

    public boolean areRegionsSupported(String platform) {
        return getSpecialParameters(Platform.platform(platform)).get(PlatformParametersConsts.REGIONS_SUPPORTED);
    }

    public boolean isVolumeAttachmentSupported(String platform) {
        return getSpecialParameters(Platform.platform(platform)).get(PlatformParametersConsts.VOLUME_ATTACHMENT_SUPPORTED);
    }

    private Map<String, Boolean> getSpecialParameters(Platform platform) {
        PlatformParameters platformParameters = getPlatformParameters().get(platform);
        if (platformParameters == null) {
            throw new IllegalArgumentException(String.format("%s platform is not supported", platform));
        }
        return platformParameters.specialParameters().getSpecialParameters();
    }
}
