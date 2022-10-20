package com.sequenceiq.cloudbreak.api.endpoint.v4.filesystems.requests;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sequenceiq.common.model.FileSystemType;
import com.sequenceiq.common.model.JsonEntity;

@JsonIgnoreProperties(ignoreUnknown = true, value = "type")
public interface CloudStorageParameters extends JsonEntity {

    FileSystemType getType();

}
