package com.sequenceiq.common.model;

import java.io.Serializable;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true, value = "type")
public interface FileSystemAwareCloudStorage extends Serializable {

    FileSystemType getType();

}
