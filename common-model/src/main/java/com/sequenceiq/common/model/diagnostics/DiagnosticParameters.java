package com.sequenceiq.common.model.diagnostics;

import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;

import com.sequenceiq.common.api.telemetry.model.DiagnosticsDestination;
import com.sequenceiq.common.api.telemetry.model.VmLog;

public class DiagnosticParameters {

    public static final String DEFAULT_ROOT = "filecollector";

    public static final String TELEMETRY_ROOT = "telemetry";

    public static final String HOSTS_FILTER = "hosts";

    public static final String EXCLUDE_HOSTS_FILTER = "excludeHosts";

    public static final String HOST_GROUPS_FILTER = "hostGroups";

    private String root;

    private String issue;

    private String description;

    private List<String> labels;

    private Date startTime;

    private Date endTime;

    private DiagnosticsDestination destination = DiagnosticsDestination.LOCAL;

    private Set<String> hostGroups = new HashSet<>();

    private Set<String> hosts = new HashSet<>();

    private Set<String> excludeHosts = new HashSet<>();

    private List<VmLog> additionalLogs = List.of();

    private Boolean includeSaltLogs = Boolean.FALSE;

    private Boolean includeSarOutput = Boolean.FALSE;

    private Boolean includeNginxReport = Boolean.FALSE;

    private Boolean includeSeLinuxReport = Boolean.FALSE;

    private Boolean updatePackage = Boolean.FALSE;

    private Boolean skipValidation = Boolean.FALSE;

    private Boolean skipUnresponsiveHosts = Boolean.FALSE;

    private Boolean skipWorkspaceCleanupOnStartup = Boolean.FALSE;

    private String uuid;

    private String accountId;

    private String clusterType;

    private String clusterVersion;

    private String dbusUrl;

    private String dbusS3Url;

    private String supportBundleDbusAccessKey;

    private String supportBundleDbusPrivateKey;

    private String supportBundleDbusAccessKeyType;

    private String supportBundleDbusAppName;

    private String supportBundleDbusStreamName;

    private String statusReason;

    private CloudStorageDiagnosticsParameters cloudStorageDiagnosticsParameters;

    public Map<String, Object> toMap() {
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("destination", Optional.ofNullable(destination)
                .map(Enum::toString).orElse(null));
        parameters.put("issue", issue);
        parameters.put("description", description);
        parameters.put("labelFilter", labels);
        parameters.put("startTime", Optional.ofNullable(startTime)
                .map(Date::getTime).orElse(null));
        parameters.put("endTime", Optional.ofNullable(endTime)
                .map(Date::getTime).orElse(null));
        parameters.put(HOST_GROUPS_FILTER, hostGroups);
        parameters.put(HOSTS_FILTER, hosts);
        parameters.put(EXCLUDE_HOSTS_FILTER, excludeHosts);
        parameters.put("includeSaltLogs", Optional.ofNullable(includeSaltLogs).orElse(false));
        parameters.put("includeSarOutput", Optional.ofNullable(includeSarOutput).orElse(false));
        parameters.put("includeNginxReport", Optional.ofNullable(includeNginxReport).orElse(false));
        parameters.put("includeSeLinuxReport", Optional.ofNullable(includeSeLinuxReport).orElse(false));
        parameters.put("updatePackage", Optional.ofNullable(updatePackage).orElse(false));
        parameters.put("skipValidation", Optional.ofNullable(skipValidation).orElse(false));
        parameters.put("skipUnresponsiveHosts", Optional.ofNullable(skipUnresponsiveHosts).orElse(false));
        parameters.put("skipWorkspaceCleanupOnStartup", Optional.ofNullable(skipWorkspaceCleanupOnStartup).orElse(false));
        parameters.put("additionalLogs", additionalLogs);
        parameters.put("uuid", uuid);
        parameters.put("accountId", accountId);
        parameters.put("clusterType", clusterType);
        parameters.put("clusterVersion", clusterVersion);
        parameters.put("mode", null);
        parameters.put("dbusUrl", dbusUrl);
        parameters.put("dbusS3Url", dbusS3Url);
        parameters.put("supportBundleDbusAccessKey", supportBundleDbusAccessKey);
        parameters.put("supportBundleDbusAccessKeyType", supportBundleDbusAccessKeyType);
        parameters.put("supportBundleDbusPrivateKey", supportBundleDbusPrivateKey);
        parameters.put("supportBundleDbusStreamName", supportBundleDbusStreamName);
        parameters.put("supportBundleDbusAppName", supportBundleDbusAppName);
        parameters.put("statusReason", statusReason);
        if (cloudStorageDiagnosticsParameters != null) {
            parameters.putAll(cloudStorageDiagnosticsParameters.toMap());
        }
        Map<String, Object> fileCollector = new HashMap<>();
        if (StringUtils.isEmpty(root)) {
            fileCollector.put(DEFAULT_ROOT, parameters);
        } else {
            fileCollector.put(root, parameters);
        }
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

    public void setExcludeHosts(Set<String> excludeHosts) {
        this.excludeHosts = excludeHosts;
    }

    public void setAdditionalLogs(List<VmLog> additionalLogs) {
        this.additionalLogs = additionalLogs;
    }

    public void setIncludeSaltLogs(Boolean includeSaltLogs) {
        this.includeSaltLogs = includeSaltLogs;
    }

    public void setIncludeSarOutput(Boolean includeSarOutput) {
        this.includeSarOutput = includeSarOutput;
    }

    public void setIncludeNginxReport(Boolean includeNginxReport) {
        this.includeNginxReport = includeNginxReport;
    }

    public void setIncludeSeLinuxReport(Boolean includeSeLinuxReport) {
        this.includeSeLinuxReport = includeSeLinuxReport;
    }

    public void setUpdatePackage(Boolean updatePackage) {
        this.updatePackage = updatePackage;
    }

    public void setSkipValidation(Boolean skipValidation) {
        this.skipValidation = skipValidation;
    }

    public void setSkipUnresponsiveHosts(Boolean skipUnresponsiveHosts) {
        this.skipUnresponsiveHosts = skipUnresponsiveHosts;
    }

    public void setSkipWorkspaceCleanupOnStartup(Boolean skipWorkspaceCleanupOnStartup) {
        this.skipWorkspaceCleanupOnStartup = skipWorkspaceCleanupOnStartup;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public void setAccountId(String accountId) {
        this.accountId = accountId;
    }

    public void setClusterType(String clusterType) {
        this.clusterType = clusterType;
    }

    public void setClusterVersion(String clusterVersion) {
        this.clusterVersion = clusterVersion;
    }

    public void setDbusUrl(String dbusUrl) {
        this.dbusUrl = dbusUrl;
    }

    private void setDbusS3Url(String dbusS3Url) {
        this.dbusS3Url = dbusS3Url;
    }

    public void setSupportBundleDbusAccessKey(String supportBundleDbusAccessKey) {
        this.supportBundleDbusAccessKey = supportBundleDbusAccessKey;
    }

    public void setSupportBundleDbusPrivateKey(String supportBundleDbusPrivateKey) {
        this.supportBundleDbusPrivateKey = supportBundleDbusPrivateKey;
    }

    public void setSupportBundleDbusAccessKeyType(String supportBundleDbusAccessKeyType) {
        this.supportBundleDbusAccessKeyType = supportBundleDbusAccessKeyType;
    }

    public void setSupportBundleDbusAppName(String supportBundleDbusAppName) {
        this.supportBundleDbusAppName = supportBundleDbusAppName;
    }

    public void setSupportBundleDbusStreamName(String supportBundleDbusStreamName) {
        this.supportBundleDbusStreamName = supportBundleDbusStreamName;
    }

    public void setCloudStorageDiagnosticsParameters(CloudStorageDiagnosticsParameters cloudStorageDiagnosticsParameters) {
        this.cloudStorageDiagnosticsParameters = cloudStorageDiagnosticsParameters;
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

    public Set<String> getExcludeHosts() {
        return excludeHosts;
    }

    public List<VmLog> getAdditionalLogs() {
        return additionalLogs;
    }

    public Boolean getIncludeSaltLogs() {
        return includeSaltLogs;
    }

    public Boolean getIncludeSarOutput() {
        return includeSarOutput;
    }

    public Boolean getIncludeNginxReport() {
        return includeNginxReport;
    }

    public Boolean getIncludeSeLinuxReport() {
        return includeSeLinuxReport;
    }

    public Boolean getUpdatePackage() {
        return updatePackage;
    }

    public Boolean getSkipValidation() {
        return skipValidation;
    }

    public Boolean getSkipUnresponsiveHosts() {
        return skipUnresponsiveHosts;
    }

    public Boolean getSkipWorkspaceCleanupOnStartup() {
        return skipWorkspaceCleanupOnStartup;
    }

    public String getUuid() {
        return uuid;
    }

    public String getAccountId() {
        return accountId;
    }

    public String getClusterType() {
        return clusterType;
    }

    public String getClusterVersion() {
        return clusterVersion;
    }

    public String getDbusUrl() {
        return dbusUrl;
    }

    public String getDbusS3Url() {
        return dbusS3Url;
    }

    public String getSupportBundleDbusAccessKey() {
        return supportBundleDbusAccessKey;
    }

    public String getSupportBundleDbusPrivateKey() {
        return supportBundleDbusPrivateKey;
    }

    public String getSupportBundleDbusAccessKeyType() {
        return supportBundleDbusAccessKeyType;
    }

    public String getSupportBundleDbusAppName() {
        return supportBundleDbusAppName;
    }

    public String getSupportBundleDbusStreamName() {
        return supportBundleDbusStreamName;
    }

    public String getRoot() {
        return root;
    }

    public void setRoot(String root) {
        this.root = root;
    }

    public String getStatusReason() {
        return statusReason;
    }

    public void setStatusReason(String statusReason) {
        this.statusReason = statusReason;
    }

    public CloudStorageDiagnosticsParameters getCloudStorageDiagnosticsParameters() {
        return cloudStorageDiagnosticsParameters;
    }

    public static DiagnosticParametersBuilder builder() {
        return new DiagnosticParametersBuilder();
    }

    public static class DiagnosticParametersBuilder {
        private DiagnosticParameters diagnosticParameters;

        DiagnosticParametersBuilder() {
            diagnosticParameters = new DiagnosticParameters();
        }

        public static DiagnosticParametersBuilder builder() {
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

        public DiagnosticParametersBuilder withUuid(String uuid) {
            diagnosticParameters.setUuid(uuid);
            return this;
        }

        public DiagnosticParametersBuilder withClusterType(String clusterType) {
            diagnosticParameters.setClusterType(clusterType);
            return this;
        }

        public DiagnosticParametersBuilder withClusterVersion(String clusterVersion) {
            diagnosticParameters.setClusterVersion(clusterVersion);
            return this;
        }

        public DiagnosticParametersBuilder withAccountId(String accountId) {
            diagnosticParameters.setAccountId(accountId);
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

        public DiagnosticParametersBuilder withExcludeHosts(Set<String> excludeHosts) {
            diagnosticParameters.setExcludeHosts(excludeHosts);
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

        public DiagnosticParametersBuilder withIncludeSarOutput(Boolean includeSarOutput) {
            diagnosticParameters.setIncludeSarOutput(includeSarOutput);
            return this;
        }

        public DiagnosticParametersBuilder withIncludeNginxReport(Boolean includeNginxReport) {
            diagnosticParameters.setIncludeNginxReport(includeNginxReport);
            return this;
        }

        public DiagnosticParametersBuilder withIncludeSeLinuxReport(Boolean includeSeLinuxReport) {
            diagnosticParameters.setIncludeSeLinuxReport(includeSeLinuxReport);
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

        public DiagnosticParametersBuilder withSkipWorkspaceCleanupOnStartup(Boolean skipWorkspaceCleanupOnStartup) {
            diagnosticParameters.setSkipWorkspaceCleanupOnStartup(skipWorkspaceCleanupOnStartup);
            return this;
        }

        public DiagnosticParametersBuilder withSkipUnresponsiveHosts(Boolean skipUnresponsiveHosts) {
            diagnosticParameters.setSkipUnresponsiveHosts(skipUnresponsiveHosts);
            return this;
        }

        public DiagnosticParametersBuilder withDbusUrl(String dbusUrl) {
            diagnosticParameters.setDbusUrl(dbusUrl);
            return this;
        }

        public DiagnosticParametersBuilder withDbusS3Url(String dbusS3Url) {
            diagnosticParameters.setDbusS3Url(dbusS3Url);
            return this;
        }

        public DiagnosticParametersBuilder withSupportBundleDbusStreamName(String supportBundleDbusStreamName) {
            diagnosticParameters.setSupportBundleDbusStreamName(supportBundleDbusStreamName);
            return this;
        }

        public DiagnosticParametersBuilder withSupportBundleDbusAppName(String supportBundleDbusAppName) {
            diagnosticParameters.setSupportBundleDbusAppName(supportBundleDbusAppName);
            return this;
        }

        public DiagnosticParametersBuilder withRoot(String root) {
            diagnosticParameters.setRoot(root);
            return this;
        }

        public DiagnosticParametersBuilder withStatusReason(String statusReason) {
            diagnosticParameters.setStatusReason(statusReason);
            return this;
        }

        public DiagnosticParametersBuilder withCloudStorageDiagnosticsParameters(
                CloudStorageDiagnosticsParameters cloudStorageDiagnosticsParameters) {
            diagnosticParameters.setCloudStorageDiagnosticsParameters(cloudStorageDiagnosticsParameters);
            return this;
        }

        public DiagnosticParameters build() {
            return diagnosticParameters;
        }
    }
}
