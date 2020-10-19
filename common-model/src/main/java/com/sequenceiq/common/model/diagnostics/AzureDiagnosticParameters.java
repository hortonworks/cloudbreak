package com.sequenceiq.common.model.diagnostics;

import java.util.HashMap;
import java.util.Map;

public class AzureDiagnosticParameters implements CloudStorageDiagnosticsParameters {

    private String adlsv2StorageAccount;

    private String adlsv2StorageContainer;

    private String adlsv2StorageLocation;

    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("adlsv2_storage_account", adlsv2StorageAccount);
        map.put("adlsv2_storage_container", adlsv2StorageContainer);
        map.put("adlsv2_storage_location", adlsv2StorageLocation);
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

    public static final class AzureDiagnosticParametersBuilder {
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

        public AzureDiagnosticParameters build() {
            return azureDiagnosticParameters;
        }
    }
}
