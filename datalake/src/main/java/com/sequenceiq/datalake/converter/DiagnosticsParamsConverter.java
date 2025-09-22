package com.sequenceiq.datalake.converter;

import java.math.BigDecimal;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.diagnostics.model.CmDiagnosticsCollectionRequest;
import com.sequenceiq.cloudbreak.api.endpoint.v4.diagnostics.model.DiagnosticsCollectionRequest;
import com.sequenceiq.common.api.telemetry.model.DiagnosticsDestination;
import com.sequenceiq.common.api.telemetry.model.VmLog;

@Component
public class DiagnosticsParamsConverter {

    private static final String PARAM_DESTINATION = "destination";

    private static final String PARAMS_ISSUE = "issue";

    private static final String PARAMS_UUID = "uuid";

    private static final String PARAMS_TICKET = "ticket";

    private static final String PARAM_DESCRIPTION = "description";

    private static final String PARAMS_COMMENTS = "comments";

    private static final String PARAM_HOSTS = "hosts";

    private static final String PARAMS_HOST_GROUPS = "hostGroups";

    private static final String PARAMS_EXCLUDE_HOSTS = "excludeHosts";

    private static final String PARAMS_LABELS = "labels";

    private static final String PARAMS_ROLES = "roles";

    private static final String PARAMS_START_TIME = "startTime";

    private static final String PARAMS_END_TIME = "endTime";

    private static final String PARAMS_ADDITIONAL_LOGS = "additionalLogs";

    private static final String PARAMS_STACK_CRN = "stackCrn";

    private static final String PARAMS_INCLUDE_SALT_LOGS = "includeSaltLogs";

    private static final String PARAMS_INCLUDE_SAR_OUTPUT = "includeSarOutput";

    private static final String PARAMS_INCLUDE_NGINX_REPORT = "includeNginxReport";

    private static final String PARAMS_INCLUDE_SELINUX_REPORT = "includeSeLinuxReport";

    private static final String PARAMS_UPDATE_PACKAGE = "updatePackage";

    private static final String PARAMS_SKIP_VALIDATION = "skipValidation";

    private static final String PARAMS_SKIP_WORKSPACE_CLEANUP_ON_STARTUP = "skipWorkspaceCleanupOnStartup";

    private static final String PARAMS_SKIP_UNRESPONSIVE_HOSTS = "skipUnresponsiveHosts";

    private static final String PARAMS_ENABLE_MONITOR_METRICS_COLLECTION = "enableMonitorMetricsCollection";

    private static final String PARAMS_BUNDLE_SIZE_BYTES = "bundleSizeBytes";

    public Map<String, Object> convertFromRequest(DiagnosticsCollectionRequest request) {
        Map<String, Object> props = new HashMap<>();
        props.put(PARAMS_STACK_CRN, request.getStackCrn());
        props.put(PARAM_DESTINATION, request.getDestination());
        props.put(PARAM_DESCRIPTION, request.getDescription());
        props.put(PARAM_HOSTS, request.getHosts());
        props.put(PARAMS_EXCLUDE_HOSTS, request.getExcludeHosts());
        props.put(PARAMS_HOST_GROUPS, request.getHostGroups());
        props.put(PARAMS_ISSUE, request.getIssue());
        props.put(PARAMS_LABELS, request.getLabels());
        props.put(PARAMS_START_TIME, request.getStartTime());
        props.put(PARAMS_END_TIME, request.getEndTime());
        props.put(PARAMS_ADDITIONAL_LOGS, request.getAdditionalLogs());
        props.put(PARAMS_INCLUDE_SALT_LOGS, request.getIncludeSaltLogs());
        props.put(PARAMS_INCLUDE_SAR_OUTPUT, request.getIncludeSarOutput());
        props.put(PARAMS_INCLUDE_NGINX_REPORT, request.getIncludeNginxReport());
        props.put(PARAMS_INCLUDE_SELINUX_REPORT, request.getIncludeSeLinuxReport());
        props.put(PARAMS_UPDATE_PACKAGE, request.getUpdatePackage());
        props.put(PARAMS_SKIP_VALIDATION, request.getSkipValidation());
        props.put(PARAMS_SKIP_WORKSPACE_CLEANUP_ON_STARTUP, request.getSkipWorkspaceCleanupOnStartup());
        props.put(PARAMS_SKIP_UNRESPONSIVE_HOSTS, request.getSkipUnresponsiveHosts());
        return props;
    }

    public DiagnosticsCollectionRequest convertToRequest(Map<String, Object> props) {
        DiagnosticsCollectionRequest request = new DiagnosticsCollectionRequest();
        request.setDestination(Optional.ofNullable(props.get(PARAM_DESTINATION))
                .map(v -> DiagnosticsDestination.valueOf(v.toString())).orElse(null));
        request.setStackCrn(Optional.ofNullable(props.get(PARAMS_STACK_CRN)).map(Object::toString).orElse(null));
        request.setHosts((Set<String>) Optional.ofNullable(props.get(PARAM_HOSTS)).orElse(null));
        request.setExcludeHosts((Set<String>) Optional.ofNullable(props.get(PARAMS_EXCLUDE_HOSTS)).orElse(null));
        request.setHostGroups((Set<String>) Optional.ofNullable(props.get(PARAMS_HOST_GROUPS)).orElse(null));
        request.setLabels((List<String>) Optional.ofNullable(props.get(PARAMS_LABELS)).orElse(null));
        request.setAdditionalLogs((List<VmLog>) Optional.ofNullable(props.get(PARAMS_ADDITIONAL_LOGS)).orElse(null));
        request.setDescription(Optional.ofNullable(props.get(PARAM_DESCRIPTION)).map(Object::toString).orElse(null));
        request.setIssue(Optional.ofNullable(props.get(PARAMS_ISSUE)).map(Object::toString).orElse(null));
        request.setStartTime((Date) Optional.ofNullable(props.get(PARAMS_START_TIME)).orElse(null));
        request.setEndTime((Date) Optional.ofNullable(props.get(PARAMS_END_TIME)).orElse(null));
        request.setIncludeSaltLogs((Boolean) Optional.ofNullable(props.get(PARAMS_INCLUDE_SALT_LOGS)).orElse(false));
        request.setIncludeSarOutput((Boolean) Optional.ofNullable(props.get(PARAMS_INCLUDE_SAR_OUTPUT)).orElse(false));
        request.setIncludeNginxReport((Boolean) Optional.ofNullable(props.get(PARAMS_INCLUDE_NGINX_REPORT)).orElse(false));
        request.setIncludeSeLinuxReport((Boolean) Optional.ofNullable(props.get(PARAMS_INCLUDE_SELINUX_REPORT)).orElse(false));
        request.setUpdatePackage((Boolean) Optional.ofNullable(props.get(PARAMS_UPDATE_PACKAGE)).orElse(false));
        request.setSkipValidation((Boolean) Optional.ofNullable(props.get(PARAMS_SKIP_VALIDATION)).orElse(false));
        request.setSkipUnresponsiveHosts((Boolean) Optional.ofNullable(props.get(PARAMS_SKIP_UNRESPONSIVE_HOSTS)).orElse(false));
        request.setSkipWorkspaceCleanupOnStartup((Boolean) Optional.ofNullable(props.get(PARAMS_SKIP_WORKSPACE_CLEANUP_ON_STARTUP)).orElse(false));
        request.setUuid(Optional.ofNullable(props.get(PARAMS_UUID)).map(Object::toString).orElse(null));
        return request;
    }

    public Map<String, Object> convertFromCmRequest(CmDiagnosticsCollectionRequest request) {
        Map<String, Object> props = new HashMap<>();
        props.put(PARAMS_STACK_CRN, request.getStackCrn());
        props.put(PARAM_DESTINATION, request.getDestination());
        props.put(PARAMS_UPDATE_PACKAGE, request.getUpdatePackage());
        props.put(PARAMS_SKIP_VALIDATION, request.getSkipValidation());
        props.put(PARAMS_START_TIME, request.getStartTime());
        props.put(PARAMS_END_TIME, request.getEndTime());
        props.put(PARAMS_COMMENTS, request.getComments());
        props.put(PARAMS_TICKET, request.getTicket());
        props.put(PARAMS_ENABLE_MONITOR_METRICS_COLLECTION, request.getEnableMonitorMetricsCollection());
        props.put(PARAMS_ROLES, request.getRoles());
        props.put(PARAMS_BUNDLE_SIZE_BYTES, request.getBundleSizeBytes());
        return props;
    }

    public CmDiagnosticsCollectionRequest convertToCmRequest(Map<String, Object> props) {
        CmDiagnosticsCollectionRequest request = new CmDiagnosticsCollectionRequest();
        request.setStackCrn(Optional.ofNullable(props.get(PARAMS_STACK_CRN)).map(Object::toString).orElse(null));
        request.setTicket(Optional.ofNullable(props.get(PARAMS_TICKET)).map(Object::toString).orElse(null));
        request.setComments(Optional.ofNullable(props.get(PARAMS_COMMENTS)).map(Object::toString).orElse(null));
        request.setBundleSizeBytes(Optional.ofNullable(props.get(PARAMS_BUNDLE_SIZE_BYTES))
                .map(n -> BigDecimal.valueOf(Long.parseLong(n.toString()))).orElse(null));
        request.setDestination(Optional.ofNullable(props.get(PARAM_DESTINATION))
                .map(v -> DiagnosticsDestination.valueOf(v.toString())).orElse(null));
        request.setRoles((List<String>) Optional.ofNullable(props.get(PARAMS_ROLES)).orElse(null));
        request.setStartTime((Date) Optional.ofNullable(props.get(PARAMS_START_TIME)).orElse(null));
        request.setEndTime((Date) Optional.ofNullable(props.get(PARAMS_END_TIME)).orElse(null));
        request.setEnableMonitorMetricsCollection((Boolean) Optional.ofNullable(props.get(PARAMS_ENABLE_MONITOR_METRICS_COLLECTION)).orElse(false));
        request.setUpdatePackage((Boolean) Optional.ofNullable(props.get(PARAMS_UPDATE_PACKAGE)).orElse(false));
        request.setSkipValidation((Boolean) Optional.ofNullable(props.get(PARAMS_SKIP_VALIDATION)).orElse(false));
        return request;
    }

}
