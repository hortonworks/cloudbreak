package com.sequenceiq.cloudbreak.controller.validation.loadbalancer;

import java.util.Optional;

import javax.inject.Inject;

import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.google.common.base.Strings;
import com.sequenceiq.cloudbreak.cloud.model.CloudSubnet;
import com.sequenceiq.cloudbreak.converter.v4.environment.network.SubnetSelector;
import com.sequenceiq.cloudbreak.validation.ValidationResult;
import com.sequenceiq.cloudbreak.validation.ValidationResult.ValidationResultBuilder;
import com.sequenceiq.cloudbreak.validation.Validator;
import com.sequenceiq.common.api.type.PublicEndpointAccessGateway;
import com.sequenceiq.environment.api.v1.environment.model.response.EnvironmentNetworkResponse;

@Component
public class EndpointGatewayNetworkValidator implements Validator<Pair<String, EnvironmentNetworkResponse>> {

    static final String NO_BASE_SUBNET = "No cluster subnet id specified. Endpoint gateway subnet cannot be determined.";

    static final String NO_BASE_SUBNET_META = "Could not determine availability zone of cluster subnet %s";

    static final String NO_USABLE_SUBNET_IN_ENDPOINT_GATEWAY = "Unable to find public subnet in availability zone %s in " +
        "provided endoint gateway subnet list.";

    static final String NO_USABLE_SUBNET_IN_CLUSTER = "Unable to find public subnet in availability zone %s in cluster subnets.";

    private static final Logger LOGGER = LoggerFactory.getLogger(EndpointGatewayNetworkValidator.class);

    @Inject
    private SubnetSelector subnetSelector;

    @Override
    public ValidationResult validate(Pair<String, EnvironmentNetworkResponse> subject) {
        String baseSubnetId = subject.getLeft();
        EnvironmentNetworkResponse network = subject.getRight();

        ValidationResultBuilder resultBuilder = ValidationResult.builder();
        if (network == null) {
            LOGGER.debug("No network provided; public endpoint access gateway is disabled.");
        } else {
            if (PublicEndpointAccessGateway.ENABLED.equals(network.getPublicEndpointAccessGateway())) {
                if (Strings.isNullOrEmpty(baseSubnetId)) {
                    resultBuilder.error(NO_BASE_SUBNET);
                } else {
                    Optional<CloudSubnet> baseSubnet = subnetSelector.findSubnetById(network.getSubnetMetas(), baseSubnetId);
                    if (baseSubnet.isEmpty()) {
                        resultBuilder.error(String.format(NO_BASE_SUBNET_META, baseSubnetId));
                    } else {
                        String error;
                        if (!MapUtils.isEmpty(network.getGatewayEndpointSubnetMetas())) {
                            LOGGER.debug("Attempting to validate endpoint gateway subnet using provided endpoint subnets.");
                            error = NO_USABLE_SUBNET_IN_ENDPOINT_GATEWAY;
                        } else {
                            LOGGER.debug("Attempting to validate endpoint gateway subnet using cluster subnets.");
                            error = NO_USABLE_SUBNET_IN_CLUSTER;
                        }
                        Optional<CloudSubnet> endpointGatewaySubnet = subnetSelector.chooseSubnetForEndpointGateway(network, baseSubnetId);
                        resultBuilder.ifError(endpointGatewaySubnet::isEmpty,
                            String.format(error, baseSubnet.get().getAvailabilityZone()));
                    }
                }
            }
        }
        return resultBuilder.build();
    }
}
