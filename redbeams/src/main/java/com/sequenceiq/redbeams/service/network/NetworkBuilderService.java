package com.sequenceiq.redbeams.service.network;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.model.CloudSubnet;
import com.sequenceiq.cloudbreak.cloud.service.CloudParameterCache;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.common.mappable.ProviderParameterCalculator;
import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;
import com.sequenceiq.redbeams.api.endpoint.v4.stacks.NetworkV4StackRequest;
import com.sequenceiq.redbeams.domain.stack.DBStack;
import com.sequenceiq.redbeams.domain.stack.Network;
import com.sequenceiq.redbeams.service.EnvironmentService;
import com.sequenceiq.redbeams.service.UuidGeneratorService;

@Component
public class NetworkBuilderService {
    private static final Logger LOGGER = LoggerFactory.getLogger(NetworkBuilderService.class);

    @Inject
    private EnvironmentService environmentService;

    @Inject
    private SubnetListerService subnetListerService;

    @Inject
    private SubnetChooserService subnetChooserService;

    @Inject
    private ProviderParameterCalculator providerParameterCalculator;

    @Inject
    private NetworkParameterAdder networkParameterAdder;

    @Inject
    private UuidGeneratorService uuidGeneratorService;

    @Inject
    private CloudParameterCache cloudParameterCache;

    @Inject
    private NetworkService networkService;

    public Network buildNetwork(NetworkV4StackRequest source, DetailedEnvironmentResponse environmentResponse, CloudPlatform cloudPlatform,
            DBStack dbStack) {
        Network network = new Network();
        network.setName(generateNetworkName());

        Map<String, Object> parameters = source != null
                ? providerParameterCalculator.get(source).asMap()
                : getSubnetsFromEnvironment(environmentResponse, cloudPlatform, dbStack);

        networkParameterAdder.addParameters(parameters, environmentResponse, cloudPlatform, dbStack);

        if (parameters != null) {
            try {
                network.setAttributes(new Json(parameters));
            } catch (IllegalArgumentException e) {
                throw new BadRequestException("Invalid network parameters", e);
            }
        }
        return networkService.save(network);
    }

    public void updateNetworkSubnets(DBStack dbStack) {
        String cloudPlatform = dbStack.getCloudPlatform();
        if (cloudParameterCache.isDbSubnetsUpdateEnabled(cloudPlatform)) {
            DetailedEnvironmentResponse environment = environmentService.getByCrn(dbStack.getEnvironmentId());
            Map<String, Object> envSubnets = getSubnetsFromEnvironment(environment, CloudPlatform.valueOf(cloudPlatform), dbStack);
            Network network = networkService.getById(dbStack.getNetwork());
            Map<String, Object> networkAttributes = network.getAttributes().getMap();
            networkAttributes.putAll(envSubnets);
            network.setAttributes(new Json(networkAttributes));
            LOGGER.info("The subnets of the DB stack [{}] updated with [{}] subnets before db upgrade", dbStack.getName(), envSubnets);
            networkService.save(network);
        } else {
            LOGGER.info("The subnets of the DB stack [{}] is not enabled", dbStack.getName());
        }
    }

    private Map<String, Object> getSubnetsFromEnvironment(DetailedEnvironmentResponse environmentResponse, CloudPlatform cloudPlatform,
            DBStack dbStack) {
        List<CloudSubnet> subnets = subnetListerService.listSubnets(environmentResponse, cloudPlatform);
        List<CloudSubnet> chosenSubnet = subnetChooserService.chooseSubnets(subnets, cloudPlatform, dbStack);

        List<String> chosenSubnetIds = chosenSubnet
                .stream()
                .map(CloudSubnet::getId)
                .collect(Collectors.toList());
        List<String> chosenAzs = chosenSubnet
                .stream()
                .map(CloudSubnet::getAvailabilityZone)
                .collect(Collectors.toList());

        return networkParameterAdder.addSubnetIds(new HashMap<>(), chosenSubnetIds, chosenAzs, cloudPlatform);
    }

    private String generateNetworkName() {
        return String.format("n-%s", uuidGeneratorService.randomUuid());
    }
}
