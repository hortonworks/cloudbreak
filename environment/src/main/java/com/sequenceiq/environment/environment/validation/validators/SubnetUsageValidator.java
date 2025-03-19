package com.sequenceiq.environment.environment.validation.validators;

import static java.lang.String.format;
import static java.util.stream.Collectors.groupingBy;
import static org.slf4j.LoggerFactory.getLogger;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.StackV4Endpoint;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.validation.ValidationResult;
import com.sequenceiq.common.api.UsedSubnetWithResourceResponse;
import com.sequenceiq.common.api.util.ResourceTypeConverter;
import com.sequenceiq.environment.environment.domain.Environment;
import com.sequenceiq.environment.network.dto.NetworkDto;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.FreeIpaV1Endpoint;
import com.sequenceiq.redbeams.api.endpoint.v4.databaseserver.DatabaseServerV4Endpoint;

@Component
public class SubnetUsageValidator {

    private static final Logger LOGGER = getLogger(SubnetUsageValidator.class);

    private final StackV4Endpoint stackV4Endpoint;

    private final FreeIpaV1Endpoint freeIpaV1Endpoint;

    private final DatabaseServerV4Endpoint databaseServerV4Endpoint;

    public SubnetUsageValidator(
            StackV4Endpoint stackV4Endpoint,
            FreeIpaV1Endpoint freeIpaV1Endpoint,
            DatabaseServerV4Endpoint databaseServerV4Endpoint) {
        this.stackV4Endpoint = stackV4Endpoint;
        this.freeIpaV1Endpoint = freeIpaV1Endpoint;
        this.databaseServerV4Endpoint = databaseServerV4Endpoint;
    }

    public void validate(Environment environment, NetworkDto network, ValidationResult.ValidationResultBuilder resultBuilder) {
        Collection<UsedSubnetWithResourceResponse> allUsedSubnetsWithResources = listAllUsedSubnets(environment.getResourceCrn());
        Map<String, List<UsedSubnetWithResourceResponse>> groupResourcesBySubnet = allUsedSubnetsWithResources.stream()
                .collect(groupingBy(UsedSubnetWithResourceResponse::getSubnetId));
        Set<String> allUsedSubnets = new HashSet<>(groupResourcesBySubnet.keySet());
        allUsedSubnets.removeAll(network.getSubnetMetas().keySet());
        LOGGER.debug("All used subnets: {}", groupResourcesBySubnet);
        if (!allUsedSubnets.isEmpty()) {
            Set<UsedSubnetWithResourceResponse> allRemovedSubnetWithResources = new HashSet<>();
            allUsedSubnets.forEach(s -> {
                List<UsedSubnetWithResourceResponse> usedSubnetWithResourceResponses = groupResourcesBySubnet.get(s);
                allRemovedSubnetWithResources.addAll(usedSubnetWithResourceResponses);
            });
            Map<String, List<UsedSubnetWithResourceResponse>> groupResourcesByType = allRemovedSubnetWithResources.stream()
                    .collect(groupingBy(UsedSubnetWithResourceResponse::getType));
            groupResourcesByType.forEach((type, resources) -> {
                Set<String> subnets = resources.stream().map(UsedSubnetWithResourceResponse::getSubnetId).collect(Collectors.toSet());
                resultBuilder.error(format("The %s uses the subnets of %s, cannot remove these from the environment",
                        ResourceTypeConverter.convertToHumanReadableName(type), subnets));
            });
        }
    }

    private Collection<UsedSubnetWithResourceResponse> listAllUsedSubnets(String environmentCrn) {
        Collection<UsedSubnetWithResourceResponse> usedSubnetInCore = ThreadBasedUserCrnProvider.doAsInternalActor(
                () -> stackV4Endpoint.getUsedSubnetsByEnvironment(0L, environmentCrn).getResponses());
        Collection<UsedSubnetWithResourceResponse> usedSubnetInFreeIpa = ThreadBasedUserCrnProvider.doAsInternalActor(
                () -> freeIpaV1Endpoint.getUsedSubnetsByEnvironment(environmentCrn).getResponses());
        Collection<UsedSubnetWithResourceResponse> usedSubnetInRedBeams = ThreadBasedUserCrnProvider.doAsInternalActor(
                () -> databaseServerV4Endpoint.getUsedSubnetsByEnvironment(environmentCrn).getResponses());
        List<UsedSubnetWithResourceResponse> allUsedSubnets = new ArrayList<>();
        allUsedSubnets.addAll(usedSubnetInCore);
        allUsedSubnets.addAll(usedSubnetInFreeIpa);
        // until the used subnet won't be implemented, this always will be empty, but the fetch skeleton left in the code
        allUsedSubnets.addAll(usedSubnetInRedBeams);
        LOGGER.debug("Collected used subnets: {}", allUsedSubnets);
        return allUsedSubnets;
    }
}
