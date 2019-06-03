package com.sequenceiq.cloudbreak.cloud.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public enum LoggingOutputType {
    @JsonProperty("s3") S3, @JsonProperty("wasb") WASB,
    @JsonProperty("abfs") ABFS, @JsonProperty("gcs") GCS,
    @JsonProperty("cloudwatch") CLOUDWATCH
}
