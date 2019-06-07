package com.sequenceiq.cloudbreak.cloud.model;

import java.io.Serializable;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.cloud.model.logging.CommonLoggingAttributes;
import com.sequenceiq.cloudbreak.cloud.model.logging.S3LoggingAttributes;

@JsonIgnoreProperties(ignoreUnknown = true)
public class LoggingAttributesHolder implements Serializable {

    @JsonProperty("common")
    private CommonLoggingAttributes commonAttributes;

    @JsonProperty("s3")
    private S3LoggingAttributes s3Attributes;

    public CommonLoggingAttributes getCommonAttributes() {
        return commonAttributes;
    }

    public void setCommonAttributes(CommonLoggingAttributes commonAttributes) {
        this.commonAttributes = commonAttributes;
    }

    public S3LoggingAttributes getS3Attributes() {
        return s3Attributes;
    }

    public void setS3Attributes(S3LoggingAttributes s3Attributes) {
        this.s3Attributes = s3Attributes;
    }
}
