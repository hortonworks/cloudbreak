package com.sequenceiq.cloudbreak.cloud.model.logging;

import static com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.ANY;
import static com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.NONE;

import java.io.Serializable;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonAutoDetect(fieldVisibility = ANY, getterVisibility = NONE, setterVisibility = NONE)
@JsonIgnoreProperties(ignoreUnknown = true)
public class S3LoggingAttributes implements Serializable {

    private final String bucket;

    private final String basePath;

    private final Integer partitionIntervalMin;

    public S3LoggingAttributes(@JsonProperty("bucket") String bucket,
            @JsonProperty("basePath") String basePath,
            @JsonProperty("partitionIntervalMin") Integer partitionIntervalMin) {
        this.bucket = bucket;
        this.basePath = basePath;
        this.partitionIntervalMin = partitionIntervalMin;
    }

    public String getBucket() {
        return bucket;
    }

    public String getBasePath() {
        return basePath;
    }

    public Integer getPartitionIntervalMin() {
        return partitionIntervalMin;
    }
}
