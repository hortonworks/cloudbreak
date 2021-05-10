package com.sequenceiq.common.model.diagnostics;

import static com.sequenceiq.common.model.diagnostics.DiagnosticParameters.DEFAULT_ROOT;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.sequenceiq.common.api.telemetry.model.DiagnosticsDestination;

public class CmDiagnosticsParameters implements Serializable {

    // used for salt to know the collection is for cloudera manager and not for filecollector only
    private static final String CM_DIAGNOSTICS_MODE = "CLOUDERA_MANAGER";

    private DiagnosticsDestination destination;

    private Boolean updatePackage;

    private Boolean skipValidation;

    private String ticketNumber;

    private String comments;

    private BigDecimal bundleSizeBytes;

    private Date startTime;

    private Date endTime;

    private String clusterName;

    private Boolean enableMonitorMetricsCollection;

    private List<String> roles;

    private String s3Bucket;

    private String s3Region;

    private String s3Location;

    private String adlsv2StorageAccount;

    private String adlsv2StorageContainer;

    private String adlsv2StorageLocation;

    private String gcsBucket;

    private String gcsLocation;

    private CmDiagnosticsParameters() {
    }

    public DiagnosticsDestination getDestination() {
        return destination;
    }

    public void setDestination(DiagnosticsDestination destination) {
        this.destination = destination;
    }

    public String getTicketNumber() {
        return ticketNumber;
    }

    public void setTicketNumber(String ticketNumber) {
        this.ticketNumber = ticketNumber;
    }

    public String getComments() {
        return comments;
    }

    public void setComments(String comments) {
        this.comments = comments;
    }

    public BigDecimal getBundleSizeBytes() {
        return bundleSizeBytes;
    }

    public void setBundleSizeBytes(BigDecimal bundleSizeBytes) {
        this.bundleSizeBytes = bundleSizeBytes;
    }

    public Date getStartTime() {
        return startTime;
    }

    public void setStartTime(Date startTime) {
        this.startTime = startTime;
    }

    public Date getEndTime() {
        return endTime;
    }

    public void setEndTime(Date endTime) {
        this.endTime = endTime;
    }

    public String getClusterName() {
        return clusterName;
    }

    public void setClusterName(String clusterName) {
        this.clusterName = clusterName;
    }

    public Boolean getEnableMonitorMetricsCollection() {
        return enableMonitorMetricsCollection;
    }

    public void setEnableMonitorMetricsCollection(Boolean enableMonitorMetricsCollection) {
        this.enableMonitorMetricsCollection = enableMonitorMetricsCollection;
    }

    public List<String> getRoles() {
        return roles;
    }

    public void setRoles(List<String> roles) {
        this.roles = roles;
    }

    public String getS3Bucket() {
        return s3Bucket;
    }

    public void setS3Bucket(String s3Bucket) {
        this.s3Bucket = s3Bucket;
    }

    public String getS3Region() {
        return s3Region;
    }

    public void setS3Region(String s3Region) {
        this.s3Region = s3Region;
    }

    public String getS3Location() {
        return s3Location;
    }

    public void setS3Location(String s3Location) {
        this.s3Location = s3Location;
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

    public String getGcsBucket() {
        return gcsBucket;
    }

    public void setGcsBucket(String gcsBucket) {
        this.gcsBucket = gcsBucket;
    }

    public String getGcsLocation() {
        return gcsLocation;
    }

    public void setGcsLocation(String gcsLocation) {
        this.gcsLocation = gcsLocation;
    }

    public Boolean getUpdatePackage() {
        return updatePackage;
    }

    public void setUpdatePackage(Boolean updatePackage) {
        this.updatePackage = updatePackage;
    }

    public Boolean getSkipValidation() {
        return skipValidation;
    }

    public void setSkipValidation(Boolean skipValidation) {
        this.skipValidation = skipValidation;
    }

    public Map<String, Object> toMap() {
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("destination", destination.toString());
        parameters.put("mode", CM_DIAGNOSTICS_MODE);
        parameters.put("updatePackage", updatePackage);
        parameters.put("skipValidation", skipValidation);
        parameters.put("s3_bucket", s3Bucket);
        parameters.put("s3_location", s3Location);
        parameters.put("s3_region", s3Region);
        parameters.put("adlsv2_storage_account", adlsv2StorageAccount);
        parameters.put("adlsv2_storage_container", adlsv2StorageContainer);
        parameters.put("adlsv2_storage_location", adlsv2StorageLocation);
        parameters.put("gcs_bucket", gcsBucket);
        parameters.put("gcs_location", gcsLocation);
        Map<String, Object> fileCollector = new HashMap<>();
        fileCollector.put(DEFAULT_ROOT, parameters);
        return fileCollector;
    }

    public static CmDiagnosticsParametersBuilder builder() {
        return new CmDiagnosticsParametersBuilder();
    }

    public static class CmDiagnosticsParametersBuilder {

        private CmDiagnosticsParameters diagnosticParameters;

        private CmDiagnosticsParametersBuilder() {
            diagnosticParameters = new CmDiagnosticsParameters();
        }

        public CmDiagnosticsParametersBuilder withTicketNumber(String ticketNumber) {
            this.diagnosticParameters.setTicketNumber(ticketNumber);
            return this;
        }

        public CmDiagnosticsParametersBuilder withComments(String comments) {
            this.diagnosticParameters.setTicketNumber(comments);
            return this;
        }

        public CmDiagnosticsParametersBuilder withStartTime(Date startTime) {
            this.diagnosticParameters.setStartTime(startTime);
            return this;
        }

        public CmDiagnosticsParametersBuilder withEndTime(Date endTime) {
            this.diagnosticParameters.setEndTime(endTime);
            return this;
        }

        public CmDiagnosticsParametersBuilder withClusterName(String clusterName) {
            this.diagnosticParameters.setClusterName(clusterName);
            return this;
        }

        public CmDiagnosticsParametersBuilder withRoles(List<String> roles) {
            this.diagnosticParameters.setRoles(roles);
            return this;
        }

        public CmDiagnosticsParametersBuilder withDestination(DiagnosticsDestination destination) {
            this.diagnosticParameters.setDestination(destination);
            return this;
        }

        public CmDiagnosticsParametersBuilder withBundleSizeBytes(BigDecimal bundleSizeBytes) {
            this.diagnosticParameters.setBundleSizeBytes(bundleSizeBytes);
            return this;
        }

        public CmDiagnosticsParametersBuilder withEnableMonitorMetricsCollection(
                Boolean enableMonitorMetricsCollection) {
            this.diagnosticParameters.setEnableMonitorMetricsCollection(enableMonitorMetricsCollection);
            return this;
        }

        public CmDiagnosticsParametersBuilder withS3Bucket(String s3Bucket) {
            this.diagnosticParameters.setS3Bucket(s3Bucket);
            return this;
        }

        public CmDiagnosticsParametersBuilder withS3Location(String s3Location) {
            this.diagnosticParameters.setS3Location(s3Location);
            return this;
        }

        public CmDiagnosticsParametersBuilder withS3Region(String s3Region) {
            this.diagnosticParameters.setS3Region(s3Region);
            return this;
        }

        public CmDiagnosticsParametersBuilder withAdlsv2StorageAccount(String adlsv2StorageAccount) {
            this.diagnosticParameters.setAdlsv2StorageAccount(adlsv2StorageAccount);
            return this;
        }

        public CmDiagnosticsParametersBuilder withAdlsv2StorageContainer(String adlsv2StorageContainer) {
            this.diagnosticParameters.setAdlsv2StorageContainer(adlsv2StorageContainer);
            return this;
        }

        public CmDiagnosticsParametersBuilder withAdlsv2StorageLocation(String adlsv2StorageLocation) {
            this.diagnosticParameters.setAdlsv2StorageLocation(adlsv2StorageLocation);
            return this;
        }

        public CmDiagnosticsParametersBuilder withGcsBucket(String gcsBucket) {
            this.diagnosticParameters.setGcsBucket(gcsBucket);
            return this;
        }

        public CmDiagnosticsParametersBuilder withGcsLocation(String gcsLocation) {
            this.diagnosticParameters.setGcsLocation(gcsLocation);
            return this;
        }

        public CmDiagnosticsParametersBuilder withUpdatePackage(Boolean updatePackage) {
            this.diagnosticParameters.setUpdatePackage(updatePackage);
            return this;
        }

        public CmDiagnosticsParametersBuilder withSkipValidation(Boolean skipValidation) {
            this.diagnosticParameters.setSkipValidation(skipValidation);
            return this;
        }

        public CmDiagnosticsParameters build() {
            return this.diagnosticParameters;
        }
    }
}
