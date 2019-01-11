package com.sequenceiq.cloudbreak.api.endpoint.v4.filesystems.requests;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sequenceiq.cloudbreak.api.model.JsonEntity;
import com.sequenceiq.cloudbreak.api.endpoint.v4.filesystems.requests.FileSystemType;

@JsonIgnoreProperties(ignoreUnknown = true, value = "type")
public interface CloudStorageParameters extends JsonEntity {

    FileSystemType getType();

}
