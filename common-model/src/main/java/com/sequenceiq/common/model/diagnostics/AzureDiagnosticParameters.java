package com.sequenceiq.common.model.diagnostics;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.sequenceiq.common.api.telemetry.model.DiagnosticsDestination;
import com.sequenceiq.common.api.telemetry.model.VmLog;

public class AzureDiagnosticParameters extends DiagnosticParameters {

    private String adlsv2StorageAccount;

    private String adlsv2StorageContainer;

    private String adlsv2StorageLocation;

    public Map<String, Object> toMap() {
        Map<String, Object> map = super.toMap();
        Map<String, Object> parameters = (Map<String, Object>) map.get(FILECOLLECTOR_ROOT);
        parameters.put("adlsv2_storage_account", adlsv2StorageAccount);
        parameters.put("adlsv2_storage_container", adlsv2StorageContainer);
        parameters.put("adlsv2_storage_location", adlsv2StorageLocation);
        return map;
    }

    public String getAdlsv2StorageAccount() {
        return adlsv2StorageAccount;
    }

    public void setAdlsv2StorageAccount(String adlsv2StorageAccount) {
        this.adlsv2StorageAccount = adlsv2StorageAccount;
    }

    public String getAdlsv2StorageContainer() {
        return adlsv2StorageContainer;
    }

    public void setAdlsv2StorageContainer(String adlsv2StorageContainer) {
        this.adlsv2StorageContainer = adlsv2StorageContainer;
    }

    public String getAdlsv2StorageLocation() {
        return adlsv2StorageLocation;
    }

    public void setAdlsv2StorageLocation(String adlsv2StorageLocation) {
        this.adlsv2StorageLocation = adlsv2StorageLocation;
    }

    public static AzureDiagnosticParametersBuilder builder() {
        return new AzureDiagnosticParametersBuilder();
    }

    public static final class AzureDiagnosticParametersBuilder extends DiagnosticParametersBuilder {
        private AzureDiagnosticParameters azureDiagnosticParameters;

        private AzureDiagnosticParametersBuilder() {
            azureDiagnosticParameters = new AzureDiagnosticParameters();
        }

        public AzureDiagnosticParametersBuilder withAdlsv2StorageAccount(String adlsv2StorageAccount) {
            azureDiagnosticParameters.setAdlsv2StorageAccount(adlsv2StorageAccount);
            return this;
        }

        public AzureDiagnosticParametersBuilder withAdlsv2StorageContainer(String adlsv2StorageContainer) {
            azureDiagnosticParameters.setAdlsv2StorageContainer(adlsv2StorageContainer);
            return this;
        }

        public AzureDiagnosticParametersBuilder withAdlsv2StorageLocation(String adlsv2StorageLocation) {
            azureDiagnosticParameters.setAdlsv2StorageLocation(adlsv2StorageLocation);
            return this;
        }

        public AzureDiagnosticParametersBuilder withIssue(String issue) {
            azureDiagnosticParameters.setIssue(issue);
            return this;
        }

        public AzureDiagnosticParametersBuilder withDescription(String description) {
            azureDiagnosticParameters.setDescription(description);
            return this;
        }

        public AzureDiagnosticParametersBuilder withLabels(List<String> labels) {
            azureDiagnosticParameters.setLabels(labels);
            return this;
        }

        public AzureDiagnosticParametersBuilder withStartTime(Date startTime) {
            azureDiagnosticParameters.setStartTime(startTime);
            return this;
        }

        public AzureDiagnosticParametersBuilder withEndTime(Date endTime) {
            azureDiagnosticParameters.setEndTime(endTime);
            return this;
        }

        public AzureDiagnosticParametersBuilder withDestination(DiagnosticsDestination destination) {
            azureDiagnosticParameters.setDestination(destination);
            return this;
        }

        public AzureDiagnosticParametersBuilder withHostGroups(Set<String> hostGroups) {
            azureDiagnosticParameters.setHostGroups(hostGroups);
            return this;
        }

        public AzureDiagnosticParametersBuilder withHosts(Set<String> hosts) {
            azureDiagnosticParameters.setHosts(hosts);
            return this;
        }

        public AzureDiagnosticParametersBuilder withAdditionalLogs(List<VmLog> additionalLogs) {
            azureDiagnosticParameters.setAdditionalLogs(additionalLogs);
            return this;
        }

        public AzureDiagnosticParametersBuilder withIncludeSaltLogs(Boolean includeSaltLogs) {
            azureDiagnosticParameters.setIncludeSaltLogs(includeSaltLogs);
            return this;
        }

        public AzureDiagnosticParametersBuilder withUpdatePackage(Boolean updatePackage) {
            azureDiagnosticParameters.setUpdatePackage(updatePackage);
            return this;
        }

        public AzureDiagnosticParameters build() {
            return azureDiagnosticParameters;
        }
    }
}
