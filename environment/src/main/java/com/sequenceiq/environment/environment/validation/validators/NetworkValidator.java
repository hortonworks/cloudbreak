package com.sequenceiq.environment.environment.validation.validators;

import java.util.Locale;
import java.util.Map;
import java.util.Objects;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.google.common.annotations.VisibleForTesting;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.validation.ValidationResult.ValidationResultBuilder;
import com.sequenceiq.environment.environment.domain.Environment;
import com.sequenceiq.environment.environment.dto.EnvironmentDto;
import com.sequenceiq.environment.environment.dto.EnvironmentDtoConverter;
import com.sequenceiq.environment.environment.dto.EnvironmentValidationDto;
import com.sequenceiq.environment.environment.validation.ValidationType;
import com.sequenceiq.environment.environment.validation.network.EnvironmentNetworkValidator;
import com.sequenceiq.environment.network.dto.NetworkDto;

@Component
public class NetworkValidator {

    private static final Logger LOGGER = LoggerFactory.getLogger(NetworkValidator.class);

    private final Map<CloudPlatform, EnvironmentNetworkValidator> environmentNetworkValidatorsByCloudPlatform;

    private final EnvironmentDtoConverter environmentDtoConverter;

    private final SubnetUsageValidator subnetUsageValidator;

    public NetworkValidator(Map<CloudPlatform, EnvironmentNetworkValidator> envNetworkValidators, EnvironmentDtoConverter environmentDtoConverter,
            SubnetUsageValidator subnetUsageValidator) {
        environmentNetworkValidatorsByCloudPlatform = envNetworkValidators;
        this.environmentDtoConverter = environmentDtoConverter;
        this.subnetUsageValidator = subnetUsageValidator;
    }

    public ValidationResultBuilder validateNetworkCreation(Environment environment, NetworkDto network) {
        ValidationResultBuilder resultBuilder = new ValidationResultBuilder();
        resultBuilder.prefix("Cannot create environment");
        validateNetwork(environment, network, resultBuilder);
        validateNetworkIdAndCidr(environment, network, resultBuilder);
        validateNetworkCidrAndEndpointGatewaySubnet(network, resultBuilder);
        return resultBuilder;
    }

    public ValidationResultBuilder validateNetworkEdit(Environment environment, NetworkDto network) {
        ValidationResultBuilder resultBuilder = new ValidationResultBuilder();
        resultBuilder.prefix("Cannot edit environment");
        subnetUsageValidator.validate(environment, network, resultBuilder);
        validateNetworkToEdit(environment, network, resultBuilder);
        return resultBuilder;
    }

    @VisibleForTesting
    void validateNetwork(Environment environment, NetworkDto network, ValidationResultBuilder resultBuilder) {
        CloudPlatform cloudPlatform = CloudPlatform.valueOf(environment.getCloudPlatform().toUpperCase(Locale.ROOT));
        EnvironmentNetworkValidator environmentNetworkValidator =
                environmentNetworkValidatorsByCloudPlatform.get(cloudPlatform);
        if (environmentNetworkValidator != null) {
            environmentNetworkValidator.checkNullable(cloudPlatform, network, resultBuilder);
            if (network != null) {
                environmentNetworkValidator.validateDuringRequest(network, resultBuilder);
            }
        }
    }

    private void validateNetworkToEdit(Environment environment, NetworkDto network, ValidationResultBuilder resultBuilder) {
        if (network != null) {
            EnvironmentNetworkValidator environmentNetworkValidator =
                    environmentNetworkValidatorsByCloudPlatform.get(CloudPlatform.valueOf(environment.getCloudPlatform().toUpperCase(Locale.ROOT)));
            if (environmentNetworkValidator != null) {
                EnvironmentDto environmentDto = environmentDtoConverter.environmentToDto(environment);
                EnvironmentValidationDto environmentValidationDto = EnvironmentValidationDto.builder()
                        .withValidationType(ValidationType.ENVIRONMENT_EDIT)
                        .withEnvironmentDto(environmentDto)
                        .build();
                environmentNetworkValidator.validateForNetworkEdit(environmentValidationDto, network, resultBuilder);
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

    private void validateNetworkCidrAndEndpointGatewaySubnet(NetworkDto network, ValidationResultBuilder resultBuilder) {
        if (Objects.nonNull(network) && StringUtils.isNotEmpty(network.getNetworkCidr()) &&
                CollectionUtils.isNotEmpty(network.getEndpointGatewaySubnetIds())) {

            String message = String.format("The Endpoint Gateway Subnet IDs must not be defined if CIDR ('%s') is present!", network.getNetworkCidr());
            LOGGER.info(message);
            resultBuilder.error(message);
        }
    }
}
