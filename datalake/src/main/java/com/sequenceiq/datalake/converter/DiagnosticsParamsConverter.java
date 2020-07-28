package com.sequenceiq.datalake.converter;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.diagnostics.model.DiagnosticsCollectionRequest;
import com.sequenceiq.common.api.telemetry.model.DiagnosticsDestination;
import com.sequenceiq.common.api.telemetry.model.VmLog;

@Component
public class DiagnosticsParamsConverter {

    private static final String PARAM_DESTINATION = "destination";

    private static final String PARAMS_ISSUE = "issue";

    private static final String PARAM_DESCRIPTION = "description";

    private static final String PARAM_HOSTS = "hosts";

    private static final String PARAMS_INSTANCE_GROUPS = "instanceGroups";

    private static final String PARAMS_LABELS = "labels";

    private static final String PARAMS_START_TIME = "startTime";

    private static final String PARAMS_END_TIME = "endTime";

    private static final String PARAMS_ADDITIONAL_LOGS = "additionalLogs";

    private static final String PARAMS_STACK_CRN = "stackCrn";

    public Map<String, Object> convertFromRequest(DiagnosticsCollectionRequest request) {
        Map<String, Object> props = new HashMap<>();
        props.put(PARAMS_STACK_CRN, request.getStackCrn());
        props.put(PARAM_DESTINATION, request.getDestination());
        props.put(PARAM_DESCRIPTION, request.getDescription());
        props.put(PARAM_HOSTS, request.getHosts());
        props.put(PARAMS_INSTANCE_GROUPS, request.getInstaceGroups());
        props.put(PARAMS_ISSUE, request.getIssue());
        props.put(PARAMS_LABELS, request.getLabels());
        props.put(PARAMS_START_TIME, request.getStartTime());
        props.put(PARAMS_END_TIME, request.getEndTime());
        props.put(PARAMS_ADDITIONAL_LOGS, request.getAdditionalLogs());
        return props;
    }

    public DiagnosticsCollectionRequest convertToRequest(Map<String, Object> props) {
        DiagnosticsCollectionRequest request = new DiagnosticsCollectionRequest();
        request.setDestination(Optional.ofNullable(props.get(PARAM_DESTINATION))
                .map(v -> DiagnosticsDestination.valueOf(v.toString())).orElse(null));
        request.setStackCrn(Optional.ofNullable(props.get(PARAMS_STACK_CRN)).map(Object::toString).orElse(null));
        request.setHosts((Set<String>) Optional.ofNullable(props.get(PARAM_HOSTS)).orElse(null));
        request.setInstaceGroups((Set<String>) Optional.ofNullable(props.get(PARAMS_INSTANCE_GROUPS)).orElse(null));
        request.setLabels((List<String>) Optional.ofNullable(props.get(PARAMS_LABELS)).orElse(null));
        request.setAdditionalLogs((List<VmLog>) Optional.ofNullable(props.get(PARAMS_ADDITIONAL_LOGS)).orElse(null));
        request.setDescription(Optional.ofNullable(props.get(PARAM_DESCRIPTION)).map(Object::toString).orElse(null));
        request.setIssue(Optional.ofNullable(props.get(PARAMS_ISSUE)).map(Object::toString).orElse(null));
        request.setStartTime((Date) Optional.ofNullable(props.get(PARAMS_START_TIME)).orElse(null));
        request.setEndTime((Date) Optional.ofNullable(props.get(PARAMS_END_TIME)).orElse(null));
        return request;
    }

}
