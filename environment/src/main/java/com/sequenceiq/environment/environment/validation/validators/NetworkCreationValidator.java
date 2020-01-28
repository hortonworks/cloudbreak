package com.sequenceiq.environment.environment.validation.validators;

import java.util.Map;
import java.util.Objects;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.model.CloudSubnet;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.validation.ValidationResult.ValidationResultBuilder;
import com.sequenceiq.environment.environment.domain.Environment;
import com.sequenceiq.environment.environment.validation.network.EnvironmentNetworkValidator;
import com.sequenceiq.environment.network.dto.NetworkDto;

@Component
public class NetworkCreationValidator {

    private static final Logger LOGGER = LoggerFactory.getLogger(NetworkCreationValidator.class);

    private final Map<CloudPlatform, EnvironmentNetworkValidator> environmentNetworkValidatorsByCloudPlatform;

    public NetworkCreationValidator(
            Map<CloudPlatform, EnvironmentNetworkValidator> envNetworkValidators
    ) {
        this.environmentNetworkValidatorsByCloudPlatform = envNetworkValidators;
    }

    public ValidationResultBuilder validateNetworkCreation(Environment environment, NetworkDto network, Map<String, CloudSubnet> subnetMetas) {
        ValidationResultBuilder resultBuilder = new ValidationResultBuilder();
        resultBuilder.prefix("Cannot create environment");
        validateNetwork(environment, network, subnetMetas, resultBuilder);
        validateNetworkIdAndCidr(environment, network, resultBuilder);
        return resultBuilder;
    }

    public ValidationResultBuilder validateNetworkEdit(Environment environment, NetworkDto network, Map<String, CloudSubnet> subnetMetas) {
        ValidationResultBuilder resultBuilder = new ValidationResultBuilder();
        resultBuilder.prefix("Cannot edit environment");
        validateNetwork(environment, network, subnetMetas, resultBuilder);
        return resultBuilder;
    }

    private void validateNetwork(Environment environment, NetworkDto network, Map<String, CloudSubnet> subnetMetas, ValidationResultBuilder resultBuilder) {
        if (network != null) {
            EnvironmentNetworkValidator environmentNetworkValidator =
                    environmentNetworkValidatorsByCloudPlatform.get(CloudPlatform.valueOf(environment.getCloudPlatform().toUpperCase()));
            if (environmentNetworkValidator != null) {
                environmentNetworkValidator.validateDuringRequest(network, subnetMetas, resultBuilder);
            }
        }
    }

    private void validateNetworkIdAndCidr(Environment environment, NetworkDto network, ValidationResultBuilder resultBuilder) {
        if (Objects.nonNull(network) && StringUtils.isNotEmpty(network.getNetworkCidr()) && StringUtils.isNotEmpty(network.getNetworkId())) {
            String message = String.format("The %s network id ('%s') must not be defined if cidr ('%s') is defined!",
                    environment.getCloudPlatform(), network.getNetworkId(), network.getNetworkCidr());
            LOGGER.info(message);
            resultBuilder.error(message);
        }
    }
}
