package com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.parameter.storage;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sequenceiq.cloudbreak.api.endpoint.v4.JsonEntity;
import com.sequenceiq.cloudbreak.common.type.filesystem.FileSystemType;

@JsonIgnoreProperties(ignoreUnknown = true, value = "type")
public interface CloudStorageV4Parameters extends JsonEntity {

    FileSystemType getType();

}
