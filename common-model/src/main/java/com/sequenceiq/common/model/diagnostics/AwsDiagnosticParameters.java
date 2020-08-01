package com.sequenceiq.common.model.diagnostics;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.sequenceiq.common.api.telemetry.model.DiagnosticsDestination;
import com.sequenceiq.common.api.telemetry.model.VmLog;

public class AwsDiagnosticParameters extends DiagnosticParameters {

    private String s3Bucket;

    private String s3Region;

    private String s3Location;

    public Map<String, Object> toMap() {
        Map<String, Object> map = super.toMap();
        Map<String, Object> parameters = (Map<String, Object>) map.get(FILECOLLECTOR_ROOT);
        parameters.put("s3_bucket", s3Bucket);
        parameters.put("s3_location", s3Location);
        parameters.put("s3_region", s3Region);
        return map;
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

    public static AwsDiagnosticParametersBuilder builder() {
        return new AwsDiagnosticParametersBuilder();
    }

    public static final class AwsDiagnosticParametersBuilder extends DiagnosticParametersBuilder {
        private AwsDiagnosticParameters awsDiagnosticParameters;

        private AwsDiagnosticParametersBuilder() {
            awsDiagnosticParameters = new AwsDiagnosticParameters();
        }

        public AwsDiagnosticParametersBuilder withS3Bucket(String s3Bucket) {
            awsDiagnosticParameters.setS3Bucket(s3Bucket);
            return this;
        }

        public AwsDiagnosticParametersBuilder withS3Region(String s3Region) {
            awsDiagnosticParameters.setS3Region(s3Region);
            return this;
        }

        public AwsDiagnosticParametersBuilder withS3Location(String s3Location) {
            awsDiagnosticParameters.setS3Location(s3Location);
            return this;
        }

        public AwsDiagnosticParametersBuilder withIssue(String issue) {
            awsDiagnosticParameters.setIssue(issue);
            return this;
        }

        public AwsDiagnosticParametersBuilder withDescription(String description) {
            awsDiagnosticParameters.setDescription(description);
            return this;
        }

        public AwsDiagnosticParametersBuilder withLabels(List<String> labels) {
            awsDiagnosticParameters.setLabels(labels);
            return this;
        }

        public AwsDiagnosticParametersBuilder withStartTime(Date startTime) {
            awsDiagnosticParameters.setStartTime(startTime);
            return this;
        }

        public AwsDiagnosticParametersBuilder withEndTime(Date endTime) {
            awsDiagnosticParameters.setEndTime(endTime);
            return this;
        }

        public AwsDiagnosticParametersBuilder withDestination(DiagnosticsDestination destination) {
            awsDiagnosticParameters.setDestination(destination);
            return this;
        }

        public AwsDiagnosticParametersBuilder withHostGroups(Set<String> hostGroups) {
            awsDiagnosticParameters.setHostGroups(hostGroups);
            return this;
        }

        public AwsDiagnosticParametersBuilder withHosts(Set<String> hosts) {
            awsDiagnosticParameters.setHosts(hosts);
            return this;
        }

        public AwsDiagnosticParametersBuilder withAdditionalLogs(List<VmLog> additionalLogs) {
            awsDiagnosticParameters.setAdditionalLogs(additionalLogs);
            return this;
        }

        public AwsDiagnosticParametersBuilder withIncludeSaltLogs(Boolean includeSaltLogs) {
            awsDiagnosticParameters.setIncludeSaltLogs(includeSaltLogs);
            return this;
        }

        public AwsDiagnosticParametersBuilder withUpdatePackage(Boolean updatePackage) {
            awsDiagnosticParameters.setUpdatePackage(updatePackage);
            return this;
        }

        public AwsDiagnosticParameters build() {
            return awsDiagnosticParameters;
        }
    }
}
