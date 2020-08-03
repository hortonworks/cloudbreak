package com.sequenceiq.common.model.diagnostics;

import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import com.sequenceiq.common.api.telemetry.model.DiagnosticsDestination;
import com.sequenceiq.common.api.telemetry.model.VmLog;

public class DiagnosticParameters {

    public static final String FILECOLLECTOR_ROOT = "filecollector";

    private String issue;

    private String description;

    private List<String> labels;

    private Date startTime;

    private Date endTime;

    private DiagnosticsDestination destination;

    private Set<String> hostGroups = new HashSet<>();

    private Set<String> hosts = new HashSet<>();

    private List<VmLog> additionalLogs = List.of();

    private Boolean includeSaltLogs = Boolean.FALSE;

    private Boolean updatePackage = Boolean.FALSE;

    private Boolean skipValidation = Boolean.FALSE;

    public Map<String, Object> toMap() {
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("destination", destination.toString());
        parameters.put("issue", issue);
        parameters.put("description", description);
        parameters.put("labelFilter", labels);
        parameters.put("startTime", Optional.ofNullable(startTime)
                .map(Date::getTime).orElse(null));
        parameters.put("endTime", Optional.ofNullable(endTime)
                .map(Date::getTime).orElse(null));
        parameters.put("hostGroups", Optional.ofNullable(hostGroups).orElse(null));
        parameters.put("hosts", Optional.ofNullable(hosts).orElse(null));
        parameters.put("includeSaltLogs", Optional.ofNullable(includeSaltLogs).orElse(false));
        parameters.put("updatePackage", Optional.ofNullable(updatePackage).orElse(false));
        parameters.put("skipValidation", Optional.ofNullable(skipValidation).orElse(false));
        parameters.put("additionalLogs", additionalLogs);
        Map<String, Object> fileCollector = new HashMap<>();
        fileCollector.put(FILECOLLECTOR_ROOT, parameters);
        return fileCollector;
    }

    public void setIssue(String issue) {
        this.issue = issue;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setLabels(List<String> labels) {
        this.labels = labels;
    }

    public void setStartTime(Date startTime) {
        this.startTime = startTime;
    }

    public void setEndTime(Date endTime) {
        this.endTime = endTime;
    }

    public void setDestination(DiagnosticsDestination destination) {
        this.destination = destination;
    }

    public void setHostGroups(Set<String> hostGroups) {
        this.hostGroups = hostGroups;
    }

    public void setHosts(Set<String> hosts) {
        this.hosts = hosts;
    }

    public void setAdditionalLogs(List<VmLog> additionalLogs) {
        this.additionalLogs = additionalLogs;
    }

    public void setIncludeSaltLogs(Boolean includeSaltLogs) {
        this.includeSaltLogs = includeSaltLogs;
    }

    public void setUpdatePackage(Boolean updatePackage) {
        this.updatePackage = updatePackage;
    }

    public void setSkipValidation(Boolean skipValidation) {
        this.skipValidation = skipValidation;
    }

    public String getIssue() {
        return issue;
    }

    public String getDescription() {
        return description;
    }

    public List<String> getLabels() {
        return labels;
    }

    public Date getStartTime() {
        return startTime;
    }

    public Date getEndTime() {
        return endTime;
    }

    public DiagnosticsDestination getDestination() {
        return destination;
    }

    public Set<String> getHostGroups() {
        return hostGroups;
    }

    public Set<String> getHosts() {
        return hosts;
    }

    public List<VmLog> getAdditionalLogs() {
        return additionalLogs;
    }

    public Boolean getIncludeSaltLogs() {
        return includeSaltLogs;
    }

    public Boolean getUpdatePackage() {
        return updatePackage;
    }

    public Boolean getSkipValidation() {
        return skipValidation;
    }

    public static DiagnosticParametersBuilder builder() {
        return new DiagnosticParametersBuilder();
    }

    public static class DiagnosticParametersBuilder {
        private DiagnosticParameters diagnosticParameters;

        DiagnosticParametersBuilder() {
            diagnosticParameters = new DiagnosticParameters();
        }

        public static DiagnosticParametersBuilder aDiagnosticParameters() {
            return new DiagnosticParametersBuilder();
        }

        public DiagnosticParametersBuilder withIssue(String issue) {
            diagnosticParameters.setIssue(issue);
            return this;
        }

        public DiagnosticParametersBuilder withDescription(String description) {
            diagnosticParameters.setDescription(description);
            return this;
        }

        public DiagnosticParametersBuilder withLabels(List<String> labels) {
            diagnosticParameters.setLabels(labels);
            return this;
        }

        public DiagnosticParametersBuilder withStartTime(Date startTime) {
            diagnosticParameters.setStartTime(startTime);
            return this;
        }

        public DiagnosticParametersBuilder withEndTime(Date endTime) {
            diagnosticParameters.setEndTime(endTime);
            return this;
        }

        public DiagnosticParametersBuilder withDestination(DiagnosticsDestination destination) {
            diagnosticParameters.setDestination(destination);
            return this;
        }

        public DiagnosticParametersBuilder withHostGroups(Set<String> hostGroups) {
            diagnosticParameters.setHostGroups(hostGroups);
            return this;
        }

        public DiagnosticParametersBuilder withHosts(Set<String> hosts) {
            diagnosticParameters.setHosts(hosts);
            return this;
        }

        public DiagnosticParametersBuilder withAdditionalLogs(List<VmLog> additionalLogs) {
            diagnosticParameters.setAdditionalLogs(additionalLogs);
            return this;
        }

        public DiagnosticParametersBuilder withIncludeSaltLogs(Boolean includeSaltLogs) {
            diagnosticParameters.setIncludeSaltLogs(includeSaltLogs);
            return this;
        }

        public DiagnosticParametersBuilder withUpdatePackage(Boolean updatePackage) {
            diagnosticParameters.setUpdatePackage(updatePackage);
            return this;
        }

        public DiagnosticParametersBuilder withSkipValidation(Boolean skipValidation) {
            diagnosticParameters.setSkipValidation(skipValidation);
            return this;
        }

        public DiagnosticParameters build() {
            return diagnosticParameters;
        }
    }
}
