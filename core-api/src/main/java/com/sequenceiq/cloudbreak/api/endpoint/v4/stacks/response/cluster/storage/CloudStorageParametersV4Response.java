package com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.cluster.storage;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sequenceiq.common.model.FileSystemType;
import com.sequenceiq.common.model.JsonEntity;

@JsonIgnoreProperties(ignoreUnknown = true, value = "type")
public interface CloudStorageParametersV4Response extends JsonEntity {

    FileSystemType getType();

}
