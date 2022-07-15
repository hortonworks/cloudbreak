package com.sequenceiq.cloudbreak.cloud.aws.connector.resource.upgrade.operation;

import static com.sequenceiq.cloudbreak.cloud.aws.connector.resource.upgrade.operation.RdsState.AVAILABLE;
import static com.sequenceiq.cloudbreak.cloud.aws.connector.resource.upgrade.operation.RdsState.OTHER;
import static com.sequenceiq.cloudbreak.cloud.aws.connector.resource.upgrade.operation.RdsState.UNKNOWN;
import static com.sequenceiq.cloudbreak.cloud.aws.connector.resource.upgrade.operation.RdsState.UPGRADING;

import java.util.Map;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class RdsInstanceStatusesToRdsStateConverter {

    private static final Logger LOGGER = LoggerFactory.getLogger(RdsInstanceStatusesToRdsStateConverter.class);

    public RdsState convert(Map<String, String> dbArnToInstanceStatusesMap) {
        if (dbArnToInstanceStatusesMap.isEmpty()) {
            LOGGER.debug("There is no info from RDS instances, returning {}", UNKNOWN);
            return UNKNOWN;
        }

        Map<String, String> notAvailableInstances = getNotAvailableInstances(dbArnToInstanceStatusesMap);
        if (notAvailableInstances.isEmpty()) {
            LOGGER.debug("There are no unavailable RDS instances, returning {}", AVAILABLE);
            return AVAILABLE;
        }

        if (notAvailableInstances.values().stream().anyMatch(AwsRdsState.UPGRADING.getState()::equals)) {
            LOGGER.debug("At least one RDS instance is in upgrading state, returning {}", UPGRADING);
            return UPGRADING;
        }

        LOGGER.debug("Returning {} for the RDS instance states of {}", OTHER, dbArnToInstanceStatusesMap);
        return OTHER;
    }

    private Map<String, String> getNotAvailableInstances(Map<String, String> dbArnToInstanceStatuses) {
        Map<String, String> notAvailableInstances = dbArnToInstanceStatuses.entrySet().stream()
                .filter(entry -> !AwsRdsState.AVAILABLE.getState().equals(entry.getValue()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        LOGGER.debug("Not available instances on RDS were: {}, all instances: {}", notAvailableInstances, dbArnToInstanceStatuses);
        return notAvailableInstances;
    }

}