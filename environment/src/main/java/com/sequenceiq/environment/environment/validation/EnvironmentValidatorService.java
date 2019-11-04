package com.sequenceiq.environment.environment.validation;

import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.model.CloudRegions;
import com.sequenceiq.cloudbreak.cloud.model.CloudSubnet;
import com.sequenceiq.cloudbreak.util.ValidationResult.ValidationResultBuilder;
import com.sequenceiq.environment.environment.domain.Environment;
import com.sequenceiq.environment.environment.validation.validators.EnvironmentRegionValidator;
import com.sequenceiq.environment.environment.validation.validators.NetworkCreationValidator;
import com.sequenceiq.environment.network.dto.NetworkDto;

@Component
public class EnvironmentValidatorService {

    private final EnvironmentRegionValidator environmentRegionValidator;

    private final NetworkCreationValidator networkCreationValidator;

    public EnvironmentValidatorService(EnvironmentRegionValidator environmentRegionValidator, NetworkCreationValidator networkCreationValidator) {
        this.environmentRegionValidator = environmentRegionValidator;
        this.networkCreationValidator = networkCreationValidator;
    }

    public ValidationResultBuilder validateRegionsAndLocation(String location, Set<String> requestedRegions,
            Environment environment, CloudRegions cloudRegions) {
        String cloudPlatform = environment.getCloudPlatform();
        ValidationResultBuilder regionValidationResult
                = environmentRegionValidator.validateRegions(requestedRegions, cloudRegions, cloudPlatform);
        ValidationResultBuilder locationValidationResult
                = environmentRegionValidator.validateLocation(location, requestedRegions, cloudRegions, cloudPlatform);
        return regionValidationResult.merge(locationValidationResult.build());
    }

    public ValidationResultBuilder validateNetworkCreation(Environment environment, NetworkDto network, Map<String, CloudSubnet> subnetMetas) {
        return networkCreationValidator.validateNetworkCreation(environment, network, subnetMetas);
    }
}
