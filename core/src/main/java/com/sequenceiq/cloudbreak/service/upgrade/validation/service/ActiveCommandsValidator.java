package com.sequenceiq.cloudbreak.service.upgrade.validation.service;

import java.util.List;
import java.util.Set;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cluster.api.ClusterApi;
import com.sequenceiq.cloudbreak.cluster.model.ClusterManagerCommand;
import com.sequenceiq.cloudbreak.common.exception.UpgradeValidationFailedException;
import com.sequenceiq.cloudbreak.service.cluster.ClusterApiConnectors;

@Component
public class ActiveCommandsValidator implements ServiceUpgradeValidator {

    private static final Logger LOGGER = LoggerFactory.getLogger(ActiveCommandsValidator.class);

    @Value("${cb.upgrade.validation.cm.interruptableCommands}")
    private Set<String> interruptableCommands;

    @Inject
    private ClusterApiConnectors clusterApiConnectors;

    @Override
    public void validate(ServiceUpgradeValidationRequest validationRequest) {
        ClusterApi connector = clusterApiConnectors.getConnector(validationRequest.stack());
        List<ClusterManagerCommand> activeCommands = connector.clusterStatusService().getActiveCommandsList();
        if (activeCommands != null) {
            List<ClusterManagerCommand> nonInterruptableActiveCommands = activeCommands.stream()
                    .filter(command -> !interruptableCommands.contains(command.getName()))
                    .toList();
            if (!nonInterruptableActiveCommands.isEmpty()) {
                throw new UpgradeValidationFailedException("There are active commands running on CM that are not interruptable, upgrade is not possible. " +
                        "Active commands: " + nonInterruptableActiveCommands);
            }
        }
    }
}
