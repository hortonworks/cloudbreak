package com.sequenceiq.cloudbreak.api.model.filesystem;

import java.util.Map;

import com.sequenceiq.cloudbreak.api.model.FileSystemType;
import com.sequenceiq.cloudbreak.api.model.JsonEntity;

public interface FileSystemParameters extends JsonEntity {

    FileSystemType getType();

    Map<String, String> getAsMap();

}
