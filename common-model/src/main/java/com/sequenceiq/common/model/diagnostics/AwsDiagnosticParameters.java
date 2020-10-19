package com.sequenceiq.common.model.diagnostics;

import java.util.HashMap;
import java.util.Map;

public class AwsDiagnosticParameters implements CloudStorageDiagnosticsParameters {

    private String s3Bucket;

    private String s3Region;

    private String s3Location;

    @Override
    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("s3_bucket", s3Bucket);
        map.put("s3_location", s3Location);
        map.put("s3_region", s3Region);
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

    public static final class AwsDiagnosticParametersBuilder {
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

        public AwsDiagnosticParameters build() {
            return awsDiagnosticParameters;
        }
    }
}
