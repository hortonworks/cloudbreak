package com.sequenceiq.datalake.service.sdx;

import java.util.stream.Collectors;

import jakarta.inject.Inject;

import org.apache.commons.lang3.EnumUtils;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.RangerRazEnabledV4Response;
import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.validation.ValidationResult;
import com.sequenceiq.cloudbreak.validation.ValidationResult.ValidationResultBuilder;
import com.sequenceiq.datalake.configuration.PlatformConfig;
import com.sequenceiq.datalake.entity.SdxCluster;
import com.sequenceiq.datalake.flow.SdxReactorFlowManager;
import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;

@Service
public class RangerRazService {

    @Inject
    private StackService stackService;

    @Inject
    private EnvironmentService environmentService;

    @Inject
    private PlatformConfig platformConfig;

    @Inject
    private SdxVersionRuleEnforcer sdxVersionRuleEnforcer;

    @Inject
    private EntitlementService entitlementService;

    @Inject
    private SdxService sdxService;

    @Inject
    private SdxReactorFlowManager sdxReactorFlowManager;

    public void updateRangerRazEnabled(SdxCluster sdxCluster) {
        RangerRazEnabledV4Response response = stackService.rangerRazEnabledInternal(sdxCluster.getCrn());
        if (response.isRangerRazEnabled()) {
            if (!sdxCluster.isRangerRazEnabled()) {
                DetailedEnvironmentResponse environmentResponse = environmentService.getByCrn(sdxCluster.getEnvCrn());
                validateRazEnablement(sdxCluster.getRuntime(), response.isRangerRazEnabled(),
                        environmentResponse);
                sdxCluster.setRangerRazEnabled(true);
                sdxService.save(sdxCluster);
            }
        } else {
            throw new BadRequestException(String.format("Ranger raz is not installed on the datalake: %s!", sdxCluster.getClusterName()));
        }
    }

    public void validateRazEnablement(String runtime, boolean razEnabled, DetailedEnvironmentResponse environment) {
        ValidationResultBuilder validationBuilder = new ValidationResultBuilder();
        if (razEnabled) {
            CloudPlatform cloudPlatform = EnumUtils.getEnumIgnoreCase(CloudPlatform.class, environment.getCloudPlatform());
            if (!platformConfig.getRazSupportedPlatforms().contains(cloudPlatform)) {
                validationBuilder.error(String.format("Provisioning Ranger Raz is only valid for %s",
                        platformConfig.getRazSupportedPlatforms().stream()
                                .map(CloudPlatform::getDislayName)
                                .collect(Collectors.joining(", "))));
            } else if (!sdxVersionRuleEnforcer.isRazSupported(runtime, cloudPlatform)) {
                validationBuilder.error(String.format("Provisioning Ranger Raz on %s is only valid for Cloudera Runtime version greater than or " +
                                "equal to %s and not %s", cloudPlatform.getDislayName(),
                        sdxVersionRuleEnforcer.getSupportedRazVersionForPlatform(cloudPlatform), runtime));
            }
        }
        ValidationResult validationResult = validationBuilder.build();
        if (validationResult.hasError()) {
            throw new BadRequestException(validationResult.getFormattedErrors());
        }
    }
}
