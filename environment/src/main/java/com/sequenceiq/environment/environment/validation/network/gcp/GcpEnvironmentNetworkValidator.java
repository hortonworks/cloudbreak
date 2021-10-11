package com.sequenceiq.environment.environment.validation.network.gcp;

import static com.sequenceiq.cloudbreak.common.mappable.CloudPlatform.GCP;

import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.collections4.MapUtils;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.model.CloudSubnet;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.validation.ValidationResult;
import com.sequenceiq.environment.environment.dto.EnvironmentDto;
import com.sequenceiq.environment.environment.dto.EnvironmentValidationDto;
import com.sequenceiq.environment.environment.validation.network.EnvironmentNetworkValidator;
import com.sequenceiq.environment.network.CloudNetworkService;
import com.sequenceiq.environment.network.dto.GcpParams;
import com.sequenceiq.environment.network.dto.NetworkDto;

@Component
public class GcpEnvironmentNetworkValidator implements EnvironmentNetworkValidator {

    private static final Logger LOGGER = LoggerFactory.getLogger(GcpEnvironmentNetworkValidator.class);

    private final CloudNetworkService cloudNetworkService;

    public GcpEnvironmentNetworkValidator(CloudNetworkService cloudNetworkService) {
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
        checkSubnetsProvidedWhenExistingNetwork(resultBuilder, networkDto, networkDto.getGcp(),
                cloudNetworkService.retrieveSubnetMetadata(environmentDto, networkDto));
    }

    @Override
    public void validateDuringRequest(NetworkDto networkDto, ValidationResult.ValidationResultBuilder resultBuilder) {
        if (networkDto == null) {
            return;
        }

        if (StringUtils.isEmpty(networkDto.getNetworkCidr()) && StringUtils.isEmpty(networkDto.getNetworkId())) {
            String message = "Either the GCP network id or cidr needs to be defined!";
            LOGGER.info(message);
            resultBuilder.error(message);
        }

        GcpParams gcpParams = networkDto.getGcp();
        if (gcpParams != null) {
            checkSubnetsProvidedWhenExistingNetwork(resultBuilder, gcpParams, networkDto.getSubnetMetas());
            checkExistingNetworkParamsProvidedWhenSubnetsPresent(networkDto, resultBuilder);
            checkNetworkIdIsSpecifiedWhenSubnetIdsArePresent(resultBuilder, gcpParams, networkDto);
        } else if (StringUtils.isEmpty(networkDto.getNetworkCidr())) {
            resultBuilder.error(missingParamsErrorMsg(GCP));
        }
    }

    private void checkSubnetsProvidedWhenExistingNetwork(ValidationResult.ValidationResultBuilder resultBuilder, NetworkDto network,
        GcpParams gcpParams, Map<String, CloudSubnet> subnetMetas) {
        if (StringUtils.isNotEmpty(gcpParams.getNetworkId())) {
            if (CollectionUtils.isEmpty(network.getSubnetIds())) {
                String message = String.format("If networkId (%s) is given then subnet ids must exist on GCP as well.",
                        gcpParams.getNetworkId());
                LOGGER.info(message);
                resultBuilder.error(message);
            } else if (subnetMetas.size() != network.getSubnetIds().size()) {
                String message = String.format("If networkId (%s) is given then subnet ids must be specified and must exist on GCP as well. " +
                                " Given subnetids: [%s], exisiting ones: [%s]", gcpParams.getNetworkId(),
                        network.getSubnetIds().stream().map(s -> "\"" + s + "\"").collect(Collectors.joining(", ")),
                        subnetMetas.keySet().stream().map(s -> "\"" + s + "\"").collect(Collectors.joining(", ")));
                LOGGER.info(message);
                resultBuilder.error(message);
            }
        }
    }

    private void checkExistingNetworkParamsProvidedWhenSubnetsPresent(NetworkDto networkDto, ValidationResult.ValidationResultBuilder resultBuilder) {
        if (!networkDto.getSubnetIds().isEmpty()
                && StringUtils.isEmpty(networkDto.getGcp().getNetworkId())) {
            String message =
                    String.format("If %s subnet ids were provided then network id have to be specified, too.", GCP.name());
            LOGGER.info(message);
            resultBuilder.error(message);
        }
    }

    private void checkNetworkIdIsSpecifiedWhenSubnetIdsArePresent(ValidationResult.ValidationResultBuilder resultBuilder,
        GcpParams gcpParams, NetworkDto networkDto) {
        if (StringUtils.isEmpty(gcpParams.getNetworkId()) && CollectionUtils.isNotEmpty(networkDto.getSubnetIds())) {
            resultBuilder.error("If subnetIds are given, then networkId must be specified too.");
        }
    }

    private void checkSubnetsProvidedWhenExistingNetwork(ValidationResult.ValidationResultBuilder resultBuilder,
        GcpParams gcpParams, Map<String, CloudSubnet> subnetMetas) {
        if (StringUtils.isNotEmpty(gcpParams.getNetworkId())
                && MapUtils.isEmpty(subnetMetas)) {
            String message = String.format("If networkId (%s) is given then subnet ids must be specified as well.",
                    gcpParams.getNetworkId());
            LOGGER.info(message);
            resultBuilder.error(message);
        }
    }

    @Override
    public CloudPlatform getCloudPlatform() {
        return GCP;
    }
}
