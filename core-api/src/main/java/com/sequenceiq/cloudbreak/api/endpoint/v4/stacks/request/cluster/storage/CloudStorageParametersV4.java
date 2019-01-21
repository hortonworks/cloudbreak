package com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.storage;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sequenceiq.cloudbreak.api.endpoint.v4.JsonEntity;
import com.sequenceiq.cloudbreak.services.filesystem.FileSystemType;

@JsonIgnoreProperties(ignoreUnknown = true, value = "type")
public interface CloudStorageParametersV4 extends JsonEntity {

    FileSystemType getType();

}
