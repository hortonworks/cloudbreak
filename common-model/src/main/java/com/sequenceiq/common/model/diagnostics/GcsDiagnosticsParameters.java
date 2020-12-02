package com.sequenceiq.common.model.diagnostics;

import java.util.HashMap;
import java.util.Map;

public class GcsDiagnosticsParameters implements CloudStorageDiagnosticsParameters {

    private String bucket;

    private String gcsLocation;

    @Override
    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("gcs_bucket", bucket);
        map.put("gcs_location", gcsLocation);
        return map;
    }

    public String getBucket() {
        return bucket;
    }

    public void setBucket(String bucket) {
        this.bucket = bucket;
    }

    public String getGcsLocation() {
        return gcsLocation;
    }

    public void setGcsLocation(String gcsLocation) {
        this.gcsLocation = gcsLocation;
    }

    public static GcsDiagnosticParametersBuilder builder() {
        return new GcsDiagnosticParametersBuilder();
    }

    public static final class GcsDiagnosticParametersBuilder {

        private GcsDiagnosticsParameters gcsDiagnosticsParameters;

        private GcsDiagnosticParametersBuilder() {
            this.gcsDiagnosticsParameters = new GcsDiagnosticsParameters();
        }

        public GcsDiagnosticParametersBuilder withBucket(String bucket) {
            gcsDiagnosticsParameters.setBucket(bucket);
            return this;
        }

        public GcsDiagnosticParametersBuilder withGcsLocation(String gcsLocation) {
            gcsDiagnosticsParameters.setGcsLocation(gcsLocation);
            return this;
        }

        public GcsDiagnosticsParameters build() {
            return gcsDiagnosticsParameters;
        }

    }
}
