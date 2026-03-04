package com.sequenceiq.environment.environment.validation.network.openstack;

import static com.sequenceiq.cloudbreak.common.mappable.CloudPlatform.OPENSTACK;

import java.util.Map;
import java.util.Set;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.model.CloudSubnet;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.validation.ValidationResult;
import com.sequenceiq.cloudbreak.validation.ValidationResult.ValidationResultBuilder;
import com.sequenceiq.environment.environment.dto.EnvironmentDto;
import com.sequenceiq.environment.environment.dto.EnvironmentValidationDto;
import com.sequenceiq.environment.environment.validation.network.EnvironmentNetworkValidator;
import com.sequenceiq.environment.network.CloudNetworkService;
import com.sequenceiq.environment.network.dto.NetworkDto;
import com.sequenceiq.environment.network.dto.OpenStackParams;

@Component
public class OpenstackEnvironmentNetworkValidator implements EnvironmentNetworkValidator {

    private static final Logger LOGGER = LoggerFactory.getLogger(OpenstackEnvironmentNetworkValidator.class);

    private final CloudNetworkService cloudNetworkService;

    public OpenstackEnvironmentNetworkValidator(CloudNetworkService cloudNetworkService) {
        this.cloudNetworkService = cloudNetworkService;
    }

    @Override
    public void validateDuringFlow(EnvironmentValidationDto environmentValidationDto, NetworkDto networkDto,
            ValidationResult.ValidationResultBuilder resultBuilder) {
        if (environmentValidationDto == null || environmentValidationDto.getEnvironmentDto() == null || networkDto == null) {
            LOGGER.warn("Neither EnvironmentDto nor NetworkDto could be null!");
            resultBuilder.error("Internal validation error");
            return;
        }

        EnvironmentDto environmentDto = environmentValidationDto.getEnvironmentDto();

        if (!isNetworkExisting(networkDto)) {
            return;
        }

        if (StringUtils.isEmpty(networkDto.getNetworkCidr()) && StringUtils.isEmpty(networkDto.getNetworkId())) {
            addNetworkIdOrCidrMessage("Either the OpenStack network ID or CIDR needs to be defined!", resultBuilder);
            return;
        }

        Map<String, CloudSubnet> cloudSubnetMetadata = cloudNetworkService.retrieveSubnetMetadata(environmentDto, networkDto);
        if (subnetsNotFound(resultBuilder, "Subnet IDs", networkDto.getSubnetMetas(), cloudSubnetMetadata)) {
            return;
        }

        if (CollectionUtils.isNotEmpty(networkDto.getEndpointGatewaySubnetIds())) {
            Map<String, CloudSubnet> endpointGatewaySubnets = cloudNetworkService.retrieveEndpointGatewaySubnetMetadata(environmentDto, networkDto);
            subnetsNotFound(resultBuilder, "Endpoint gateway subnet IDs", networkDto.getEndpointGatewaySubnetMetas(), endpointGatewaySubnets);
        }
    }

    private boolean subnetsNotFound(ValidationResultBuilder resultBuilder, String context,
            Map<String, CloudSubnet> requestedSubnetMetas, Map<String, CloudSubnet> providerSubnetMetas) {
        if (MapUtils.isEmpty(requestedSubnetMetas)) {
            return false;
        }
        if (requestedSubnetMetas.size() != providerSubnetMetas.size()) {
            String message = String.format("%s of the environment are not found on the OpenStack side. Missing IDs: %s",
                    context,
                    String.join(", ", requestedSubnetMetas.keySet().stream()
                            .filter(id -> !providerSubnetMetas.containsKey(id))
                            .toList()));
            LOGGER.info(message);
            resultBuilder.error(message);
            return true;
        }
        return false;
    }

    @Override
    public void validateDuringRequest(NetworkDto networkDto, ValidationResult.ValidationResultBuilder resultBuilder) {
        if (networkDto == null) {
            return;
        }

        if (StringUtils.isEmpty(networkDto.getNetworkCidr()) && StringUtils.isEmpty(networkDto.getNetworkId())) {
            addNetworkIdOrCidrMessage("Either the OpenStack network id or cidr needs to be defined!", resultBuilder);
        }

        OpenStackParams osParams = networkDto.getOpenstack();
        if (osParams != null) {
            checkSubnetsProvidedWhenExistingNetwork(resultBuilder, osParams, networkDto.getSubnetMetas(), networkDto.getSubnetIds());
            checkExistingNetworkParamsProvidedWhenSubnetsPresent(networkDto, resultBuilder);
            checkNetworkIdIsSpecifiedWhenSubnetIdsArePresent(resultBuilder, osParams, networkDto);
        } else if (StringUtils.isEmpty(networkDto.getNetworkCidr())) {
            resultBuilder.error(missingParamsErrorMsg(OPENSTACK));
        }
    }

    private void addNetworkIdOrCidrMessage(String message, ValidationResultBuilder resultBuilder) {
        LOGGER.info(message);
        resultBuilder.error(message);
    }

    private void checkSubnetsProvidedWhenExistingNetwork(ValidationResultBuilder resultBuilder, OpenStackParams osParams,
            Map<String, CloudSubnet> subnetMetas, Set<String> subnetIds) {
        if (StringUtils.isNotEmpty(osParams.getNetworkId())) {
            if (CollectionUtils.isEmpty(subnetIds)) {
                addNetworkIdOrCidrMessage(String.format("If networkId (%s) is given then subnet IDs must be specified as well.",
                        osParams.getNetworkId()), resultBuilder);
            } else if (MapUtils.isEmpty(subnetMetas) || subnetMetas.size() != subnetIds.size()) {
                addNetworkIdOrCidrMessage(String.format(
                        "If networkId (%s) is given then subnet IDs must be specified and resolvable. Given IDs: [%s], resolved: [%s]",
                        osParams.getNetworkId(), String.join(", ", subnetIds), String.join(", ", subnetMetas.keySet())), resultBuilder);
            }
        }
    }

    private void checkExistingNetworkParamsProvidedWhenSubnetsPresent(NetworkDto networkDto, ValidationResultBuilder resultBuilder) {
        if (CollectionUtils.isNotEmpty(networkDto.getSubnetIds())) {
            if (networkDto.getOpenstack() == null || StringUtils.isEmpty(networkDto.getOpenstack().getNetworkId())) {
                addNetworkIdOrCidrMessage("If subnet IDs are specified then OpenStack networkId must also be specified.", resultBuilder);
            }
        }
    }

    private void checkNetworkIdIsSpecifiedWhenSubnetIdsArePresent(ValidationResultBuilder resultBuilder, OpenStackParams osParams, NetworkDto networkDto) {
        if (CollectionUtils.isNotEmpty(networkDto.getSubnetIds()) && StringUtils.isEmpty(osParams.getNetworkId())) {
            addNetworkIdOrCidrMessage("OpenStack networkId is required when subnet IDs are provided.", resultBuilder);
        }
    }

    @Override
    public CloudPlatform getCloudPlatform() {
        return OPENSTACK;
    }
}
