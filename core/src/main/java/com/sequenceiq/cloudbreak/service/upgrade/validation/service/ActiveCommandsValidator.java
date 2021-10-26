package com.sequenceiq.cloudbreak.service.upgrade.validation.service;

import java.util.List;

import javax.inject.Inject;

import org.apache.commons.collections4.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cluster.api.ClusterApi;
import com.sequenceiq.cloudbreak.common.exception.UpgradeValidationFailedException;
import com.sequenceiq.cloudbreak.service.cluster.ClusterApiConnectors;

@Component
public class ActiveCommandsValidator implements ServiceUpgradeValidator {

    private static final Logger LOGGER = LoggerFactory.getLogger(ActiveCommandsValidator.class);

    @Inject
    private ClusterApiConnectors clusterApiConnectors;

    @Override
    public void validate(ServiceUpgradeValidationRequest validationRequest) {
        ClusterApi connector = clusterApiConnectors.getConnector(validationRequest.getStack());
        List<String> activeCommands = connector.clusterStatusService().getActiveCommandsList();
        if (CollectionUtils.isNotEmpty(activeCommands)) {
            throw new UpgradeValidationFailedException("There are active commands running on CM, upgrade is not possible. Active commands: "
                    + String.join(",", activeCommands));
        }
    }
}
