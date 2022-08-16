package com.sequenceiq.cloudbreak.service.telemetry;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.cloudbreak.core.bootstrap.service.host.decorator.TelemetryDecorator;
import com.sequenceiq.cloudbreak.orchestrator.model.SaltPillarProperties;
import com.sequenceiq.cloudbreak.service.ComponentConfigProviderService;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;
import com.sequenceiq.cloudbreak.telemetry.TelemetryComponentType;
import com.sequenceiq.cloudbreak.telemetry.orchestrator.TelemetryConfigProvider;
import com.sequenceiq.cloudbreak.view.ClusterView;
import com.sequenceiq.cloudbreak.view.StackView;
import com.sequenceiq.common.api.telemetry.model.DataBusCredential;
import com.sequenceiq.common.api.telemetry.model.MonitoringCredential;
import com.sequenceiq.common.api.telemetry.model.Telemetry;

@Service
public class TelemetryService implements TelemetryConfigProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(TelemetryService.class);

    @Inject
    private StackDtoService stackDtoService;

    @Inject
    private ComponentConfigProviderService componentConfigProviderService;

    @Inject
    private TelemetryDecorator telemetryDecorator;

    @Override
    public Map<String, SaltPillarProperties> createTelemetryConfigs(Long stackId, Set<TelemetryComponentType> components) {
        StackView stack = stackDtoService.getStackViewById(stackId);
        ClusterView cluster = stackDtoService.getClusterViewByStackId(stackId);
        DataBusCredential dataBusCredential = convertOrNull(cluster.getName(), cluster.getDatabusCredential(), DataBusCredential.class);
        MonitoringCredential monitoringCredential = convertOrNull(cluster.getName(), cluster.getMonitoringCredential(), MonitoringCredential.class);
        Telemetry telemetry = componentConfigProviderService.getTelemetry(stackId);
        LOGGER.debug("Generating telemetry configs for stack '{}'", stack.getResourceCrn());
        return telemetryDecorator.decoratePillar(new HashMap<>(), stack, cluster, telemetry, dataBusCredential, monitoringCredential);
    }

    private <T> T convertOrNull(String clusterName, String value, Class<T> type) {
        if (StringUtils.isNotBlank(value)) {
            try {
                return new Json(value).get(type);
            } catch (IOException e) {
                LOGGER.error("Cannot read {} secrets from cluster entity. Continue without secrets", type.getSimpleName(), e);
                return null;
            }
        } else {
            LOGGER.debug("Not found any {} for cluster '{}'. Continue without secrets", type.getSimpleName(), clusterName);
            return null;
        }
    }
}
