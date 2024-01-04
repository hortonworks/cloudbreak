package com.sequenceiq.cloudbreak.service.telemetry;

import java.util.Map;
import java.util.Set;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.orchestrator.model.SaltPillarProperties;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;
import com.sequenceiq.cloudbreak.telemetry.TelemetryComponentType;
import com.sequenceiq.cloudbreak.telemetry.orchestrator.TelemetryConfigProvider;
import com.sequenceiq.cloudbreak.telemetry.orchestrator.TelemetrySaltPillarDecorator;

@Service
public class TelemetryService implements TelemetryConfigProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(TelemetryService.class);

    @Inject
    private StackDtoService stackDtoService;

    @Inject
    private TelemetrySaltPillarDecorator telemetrySaltPillarDecorator;

    @Override
    public Map<String, SaltPillarProperties> createTelemetryConfigs(Long stackId, Set<TelemetryComponentType> components) {
        StackDto stack = stackDtoService.getById(stackId);
        LOGGER.debug("Generating telemetry configs for stack '{}'", stack.getResourceCrn());
        return telemetrySaltPillarDecorator.generatePillarConfigMap(stack);
    }
}
