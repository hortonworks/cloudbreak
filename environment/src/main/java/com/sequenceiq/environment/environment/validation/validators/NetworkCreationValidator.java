package com.sequenceiq.environment.environment.validation.validators;

import static com.sequenceiq.cloudbreak.common.mappable.CloudPlatform.AWS;

import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.model.CloudSubnet;
import com.sequenceiq.cloudbreak.validation.ValidationResult.ValidationResultBuilder;
import com.sequenceiq.environment.environment.domain.Environment;
import com.sequenceiq.environment.network.NetworkService;
import com.sequenceiq.environment.network.dto.NetworkDto;

@Component
public class NetworkCreationValidator {

    private static final Logger LOGGER = LoggerFactory.getLogger(NetworkCreationValidator.class);

    private final NetworkService networkService;

    public NetworkCreationValidator(NetworkService networkService) {
        this.networkService = networkService;
    }

    public ValidationResultBuilder validateNetworkCreation(Environment environment, NetworkDto network, Map<String, CloudSubnet> subnetMetas) {
        ValidationResultBuilder resultBuilder = new ValidationResultBuilder();
        if (AWS.name().equalsIgnoreCase(environment.getCloudPlatform()) && network != null && network.getNetworkCidr() == null) {
            String message;
            if (network.getSubnetIds().size() < 2) {
                message = "Cannot create environment, there should be at least two subnets in the network";
                LOGGER.info(message);
                resultBuilder.error(message);
            }
            if (subnetMetas.size() != network.getSubnetIds().size()) {
                message = String.format("Subnets of the environment (%s) are not found in the vpc (%s). ",
                        String.join(",", getSubnetDiff(network.getSubnetIds(), subnetMetas.keySet())),
                        networkService.getAwsVpcId(network).orElse(""));
                LOGGER.info(message);
                resultBuilder.error(message);
            }
            Map<String, Long> zones = subnetMetas.values().stream()
                    .collect(Collectors.groupingBy(CloudSubnet::getAvailabilityZone, Collectors.counting()));
            if (zones.size() < 2) {
                message = "Cannot create environment, the subnets in the vpc should be present at least in two different availability zones";
                LOGGER.info(message);
                resultBuilder.error(message);
            }
        }
            validateNetworkAndCidr(environment, network, resultBuilder);
        return resultBuilder;
    }

    private void validateNetworkAndCidr(Environment environment, NetworkDto network, ValidationResultBuilder resultBuilder) {
        if (Objects.nonNull(network) && StringUtils.isNotEmpty(network.getNetworkCidr())
                && StringUtils.isNotEmpty(network.getNetworkId())) {
            String message = String.format("The '%s' network id must not be defined if cidr is defined!", environment.getCloudPlatform());
            LOGGER.info(message);
            resultBuilder.error(message);
            }
    }

    private Set<String> getSubnetDiff(Set<String> envSubnets, Set<String> vpcSubnets) {
        Set<String> diff = new HashSet<>();
        for (String envSubnet : envSubnets) {
            if (!vpcSubnets.contains(envSubnet)) {
                diff.add(envSubnet);
            }
        }
        return diff;
    }
}
