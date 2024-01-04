package com.sequenceiq.cloudbreak.service.stack;

import static com.sequenceiq.cloudbreak.cloud.model.HostName.hostName;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceStatus;
import com.sequenceiq.cloudbreak.cluster.status.ExtendedHostStatuses;
import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.cloudbreak.common.type.ComponentType;
import com.sequenceiq.cloudbreak.domain.stack.Component;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.service.ComponentConfigProviderService;
import com.sequenceiq.cloudbreak.view.InstanceMetadataView;
import com.sequenceiq.common.api.telemetry.model.Logging;
import com.sequenceiq.common.api.telemetry.model.Telemetry;

import io.micrometer.core.instrument.util.StringUtils;

@Service
public class ServiceStatusCheckerLogLocationDecorator {

    private static final Logger LOGGER = LoggerFactory.getLogger(ServiceStatusCheckerLogLocationDecorator.class);

    @Inject
    private ComponentConfigProviderService componentConfigProviderService;

    public Map<InstanceMetadataView, Optional<String>> decorate(Map<InstanceMetadataView, Optional<String>> instancesWithReason,
            ExtendedHostStatuses hostStatuses, StackDto stack) {
        Map<InstanceMetadataView, Optional<String>> result = instancesWithReason;
        boolean extendReason = instancesWithReason.entrySet().stream()
                .anyMatch(e -> hostStatuses.hasHostUnhealthyServices(hostName(e.getKey().getDiscoveryFQDN()))
                        && InstanceStatus.SERVICES_UNHEALTHY != e.getKey().getInstanceStatus());
        if (extendReason) {
            String logLocation = getLogLocationFromComponent(stack.getId());
            if (StringUtils.isNotBlank(logLocation)) {
                LOGGER.debug("Extend status reason with cloud storage link for instances with service health issues.");
                String locationMessage = String.format("Please check the logs at the following location: %s", logLocation);
                result = new HashMap<>();
                for (Map.Entry<InstanceMetadataView, Optional<String>> entry : instancesWithReason.entrySet()) {
                    if (hostStatuses.hasHostUnhealthyServices(hostName(entry.getKey().getDiscoveryFQDN()))) {
                        LOGGER.debug(String.format("Extend status reason with cloud storage link for instance: %s.", entry.getKey().getDiscoveryFQDN()));
                        result.put(entry.getKey(), entry.getValue().map(reason -> String.format("%s %s", reason, locationMessage)));
                    } else {
                        result.put(entry.getKey(), entry.getValue());
                    }
                }
            }
        }
        return result;
    }

    private String getLogLocationFromComponent(Long stackId) {
        String result = "";
        Component component = componentConfigProviderService
                .getComponent(stackId, ComponentType.TELEMETRY, ComponentType.TELEMETRY.name());
        if (component == null) {
            LOGGER.warn(String.format("Telemetry component is null for stack: %d", stackId));
            return result;
        }
        Json attributes = component.getAttributes();
        if (attributes == null) {
            LOGGER.warn(String.format("Attributes in the telemetry component is null for stack: %d", stackId));
            return result;
        }
        Telemetry telemetry = null;
        try {
            telemetry = attributes.get(Telemetry.class);
        } catch (IOException e) {
            LOGGER.warn(String.format("Cannot get Telemetry from Components for stack: %d", stackId), e);
        }
        if (telemetry == null) {
            LOGGER.warn(String.format("Telemetry in the telemetry component attributes is null for stack: %d", stackId));
            return result;
        }
        Logging logging = telemetry.getLogging();
        if (logging == null) {
            LOGGER.warn(String.format("Logging configuration in the telemetry component is null for stack: %d", stackId));
            return result;
        }

        return logging.getStorageLocation();
    }
}
