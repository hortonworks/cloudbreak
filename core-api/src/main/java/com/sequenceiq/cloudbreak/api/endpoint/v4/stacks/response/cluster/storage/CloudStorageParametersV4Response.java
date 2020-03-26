package com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.cluster.storage;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sequenceiq.common.model.JsonEntity;
import com.sequenceiq.common.model.FileSystemType;

@JsonIgnoreProperties(ignoreUnknown = true, value = "type")
public interface CloudStorageParametersV4Response extends JsonEntity {

    FileSystemType getType();

}
