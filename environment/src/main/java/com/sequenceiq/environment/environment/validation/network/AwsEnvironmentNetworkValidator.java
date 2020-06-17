package com.sequenceiq.environment.environment.validation.network;

import static com.sequenceiq.cloudbreak.common.mappable.CloudPlatform.AWS;

import java.util.Collection;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.cloud.aws.service.SubnetCollectorService;
import com.sequenceiq.cloudbreak.cloud.model.CloudSubnet;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.validation.ValidationResult.ValidationResultBuilder;
import com.sequenceiq.common.api.type.Tunnel;
import com.sequenceiq.environment.environment.dto.EnvironmentDto;
import com.sequenceiq.environment.network.CloudNetworkService;
import com.sequenceiq.environment.network.dao.domain.RegistrationType;
import com.sequenceiq.environment.network.dto.NetworkDto;

@Component
public class AwsEnvironmentNetworkValidator implements EnvironmentNetworkValidator {

    private static final Logger LOGGER = LoggerFactory.getLogger(AwsEnvironmentNetworkValidator.class);

    private final CloudNetworkService cloudNetworkService;

    private final EntitlementService entitlementService;

    private final SubnetCollectorService subnetCollectorService;

    public AwsEnvironmentNetworkValidator(CloudNetworkService cloudNetworkService, EntitlementService entitlementService,
            SubnetCollectorService subnetCollectorService) {
        this.cloudNetworkService = cloudNetworkService;
        this.entitlementService = entitlementService;
        this.subnetCollectorService = subnetCollectorService;
    }

    @Override
    public void validateDuringFlow(EnvironmentDto environmentDto, NetworkDto networkDto, ValidationResultBuilder resultBuilder) {
        String message;
        if (networkDto != null && networkDto.getRegistrationType() == RegistrationType.EXISTING) {
            Map<String, CloudSubnet> cloudSubnetMetadata = cloudNetworkService.retrieveSubnetMetadata(environmentDto, networkDto);
            if (StringUtils.isEmpty(networkDto.getNetworkCidr()) && StringUtils.isEmpty(networkDto.getNetworkId())) {
                message = "Either the AWS network id or cidr needs to be defined!";
                LOGGER.info(message);
                resultBuilder.error(message);
                return;
            }
            if (networkDto.getSubnetMetas().size() != cloudSubnetMetadata.size()) {
                message = String.format("Subnets of the environment (%s) are not found in the vpc (%s).",
                        environmentDto.getName(), String.join(", ", getSubnetDiff(networkDto.getSubnetIds(), cloudSubnetMetadata.keySet())));
                LOGGER.info(message);
                resultBuilder.error(message);
                return;
            }

            boolean onlyUsePublicSubnets = onlyUsePublicSubnets(environmentDto);
            Collection<CloudSubnet> usableSubnets = collectUsableSubnets(cloudSubnetMetadata, onlyUsePublicSubnets);
            LOGGER.info("Validating cloud subnets: {}", usableSubnets);

            if (usableSubnets.size() < 2) {
                message = String.format("There should be at least two %ssubnets in the network",
                        onlyUsePublicSubnets ? "public " : "");
                LOGGER.info(message);
                resultBuilder.error(message);
                return;
            }
            Map<String, Long> zones = usableSubnets.stream()
                    .collect(Collectors.groupingBy(CloudSubnet::getAvailabilityZone, Collectors.counting()));
            if (zones.size() < 2) {
                message = String.format("The %ssubnets in the vpc should be present at least in two different availability zones",
                        onlyUsePublicSubnets ? "public " : "");
                LOGGER.info(message);
                resultBuilder.error(message);
            }
        }
    }

    private boolean onlyUsePublicSubnets(EnvironmentDto environmentDto) {
        Tunnel tunnel = environmentDto.getExperimentalFeatures().getTunnel();
        boolean internalTenant = entitlementService.internalTenant(environmentDto.getCreator(), environmentDto.getAccountId());
        return tunnel != Tunnel.CCM && !internalTenant;
    }

    private Collection<CloudSubnet> collectUsableSubnets(Map<String, CloudSubnet> cloudSubnetMetadata, boolean onlyUsePublicSubnets) {
        Collection<CloudSubnet> usableSubnets;
        if (onlyUsePublicSubnets) {
            LOGGER.info("Filtering subnets because only public subnets are usable");
            usableSubnets = subnetCollectorService.collectPublicSubnets(cloudSubnetMetadata.values());
        } else {
            usableSubnets = cloudSubnetMetadata.values();
        }
        return usableSubnets;
    }

    @Override
    public void validateDuringRequest(NetworkDto networkDto, ValidationResultBuilder resultBuilder) {
        if (networkDto != null && isNetworkExisting(networkDto)) {
            LOGGER.debug("Validation - existing - AWS network param(s) during request time");
            if (networkDto.getAws() != null) {
                if (StringUtils.isEmpty(networkDto.getAws().getVpcId())) {
                    resultBuilder.error(missingParamErrorMessage("VPC identifier(vpcId)", getCloudPlatform().name()));
                }
            } else {
                resultBuilder.error(missingParamsErrorMsg(AWS));
            }
        }
    }

    @Override
    public CloudPlatform getCloudPlatform() {
        return AWS;
    }

}
