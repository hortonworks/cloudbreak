package com.sequenceiq.common.api.cloudstorage;

import java.io.Serializable;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sequenceiq.common.api.filesystem.FileSystemType;

@JsonIgnoreProperties(ignoreUnknown = true, value = "type")
public interface CloudStorageV1Parameters extends Serializable {

    FileSystemType getType();

}
