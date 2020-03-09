package com.sequenceiq.cloudbreak.core.flow2.externaldatabase.provision.handler;

import java.util.Map;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.database.DatabaseAvailabilityType;
import com.sequenceiq.cloudbreak.cloud.model.CloudSubnet;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.exception.BadRequestException;
import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;

@Component
public class EnvironmentValidator {

    private static final Logger LOGGER = LoggerFactory.getLogger(EnvironmentValidator.class);

    public void checkValidEnvironment(String stackName, DatabaseAvailabilityType externalDatabase, DetailedEnvironmentResponse environment) {
        LOGGER.debug("Checking environment validity for stack {}", stackName);
        if (CloudPlatform.AWS.name().equalsIgnoreCase(environment.getCloudPlatform())) {
            checkAwsEnvironment(stackName, externalDatabase, environment);
        }
    }

    private void checkAwsEnvironment(String stackName, DatabaseAvailabilityType externalDatabase, DetailedEnvironmentResponse environment) {
        if (externalDatabase == DatabaseAvailabilityType.HA) {
            LOGGER.info("Checking external HA Database prerequisites");
            String message;
            if (environment.getNetwork().getSubnetMetas().size() < 2) {
                message = String.format("Cannot create external HA database for stack: %s, not enough subnets in the vpc", stackName);
                LOGGER.debug(message);
                throw new BadRequestException(message);
            }

            Map<String, Long> zones = environment.getNetwork().getSubnetMetas().values().stream()
                    .collect(Collectors.groupingBy(CloudSubnet::getAvailabilityZone, Collectors.counting()));
            if (zones.size() < 2) {
                message = String.format("Cannot create external HA database for stack: %s, "
                        + "vpc subnets must cover at least two different availability zones", stackName);
                LOGGER.debug(message);
                throw new BadRequestException(message);
            }
            LOGGER.info("Prerequisites PASSED");
        }
    }
}
