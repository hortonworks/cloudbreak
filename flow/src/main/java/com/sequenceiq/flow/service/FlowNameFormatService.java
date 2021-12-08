package com.sequenceiq.flow.service;

import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.sequenceiq.flow.domain.FlowLogIdWithTypeAndTimestamp;

@Service
public class FlowNameFormatService {

    private static final String SDX = "sdx";

    private static final String DATALAKE = "datalake";

    private static final String FLOW_CONFIG_SUFFIX = "FlowConfig";

    private static final String REDBEAMS = "redbeams";

    private static final String EXTERNAL_DATABASE = "external database";

    public String formatFlows(Set<FlowLogIdWithTypeAndTimestamp> flows) {
        if (flows == null) {
            return "";
        }
        return flows.stream()
                .map(FlowLogIdWithTypeAndTimestamp::getFlowType)
                .map(t -> t.isOnClassPath() ? t.getClassValue().getSimpleName() : t.getSimpleName())
                .map(this::formatFlowName)
                .collect(Collectors.joining(","));
    }

    public String formatFlowName(String flowName) {
        return Optional.ofNullable(flowName)
                .map(name -> name.replace(FLOW_CONFIG_SUFFIX, ""))
                .map(name -> name.replaceAll("(\\p{javaUpperCase})", " $1"))
                .map(String::toLowerCase)
                .map(String::trim)
                .map(name -> name.replaceAll(SDX, DATALAKE))
                .map(name -> name.replaceAll(REDBEAMS, EXTERNAL_DATABASE))
                .orElse("");
    }

}
