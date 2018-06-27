package com.sequenceiq.cloudbreak.api.model.v2.filesystem;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sequenceiq.cloudbreak.api.model.JsonEntity;
import com.sequenceiq.cloudbreak.api.model.filesystem.FileSystemType;

@JsonIgnoreProperties(ignoreUnknown = true, value = "type")
public interface CloudStorageParameters extends JsonEntity {

    FileSystemType getType();

}
