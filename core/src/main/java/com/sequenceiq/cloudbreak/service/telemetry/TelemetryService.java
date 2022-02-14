package com.sequenceiq.cloudbreak.service.telemetry;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.cloudbreak.core.bootstrap.service.host.decorator.TelemetryDecorator;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.orchestrator.model.SaltPillarProperties;
import com.sequenceiq.cloudbreak.service.ComponentConfigProviderService;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.cloudbreak.telemetry.TelemetryComponentType;
import com.sequenceiq.cloudbreak.telemetry.orchestrator.TelemetryConfigProvider;
import com.sequenceiq.common.api.telemetry.model.DataBusCredential;
import com.sequenceiq.common.api.telemetry.model.Telemetry;

@Service
public class TelemetryService implements TelemetryConfigProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(TelemetryService.class);

    @Inject
    private StackService stackService;

    @Inject
    private ComponentConfigProviderService componentConfigProviderService;

    @Inject
    private TelemetryDecorator telemetryDecorator;

    @Override
    public Map<String, SaltPillarProperties> createTelemetryConfigs(Long stackId, Set<TelemetryComponentType> components) {
        Stack stack = stackService.getById(stackId);
        Cluster cluster = stack.getCluster();
        DataBusCredential dataBusCredential = getDatabusCredential(cluster).orElse(null);
        Telemetry telemetry = componentConfigProviderService.getTelemetry(stackId);
        LOGGER.debug("Generating telemetry configs for stack '{}'", stack.getResourceCrn());
        return telemetryDecorator.decoratePillar(new HashMap<>(), stack, telemetry, dataBusCredential);
    }

    private Optional<DataBusCredential> getDatabusCredential(Cluster cluster) {
        if (StringUtils.isNotBlank(cluster.getDatabusCredential())) {
            try {
                return Optional.ofNullable(new Json(cluster.getDatabusCredential()).get(DataBusCredential.class));
            } catch (IOException e) {
                LOGGER.error("Cannot read DataBus secrets from cluster entity. Continue without databus secrets", e);
                return Optional.empty();
            }
        } else {
            LOGGER.debug("Not found any databus credential for cluster '{}'. Continue without databus secrets", cluster.getName());
            return Optional.empty();
        }

    }
}
