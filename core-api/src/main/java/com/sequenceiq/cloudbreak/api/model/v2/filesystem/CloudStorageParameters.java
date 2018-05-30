package com.sequenceiq.cloudbreak.api.model.v2.filesystem;

import com.sequenceiq.cloudbreak.api.model.JsonEntity;
import com.sequenceiq.cloudbreak.api.model.filesystem.FileSystemType;

public interface CloudStorageParameters extends JsonEntity {

    FileSystemType getType();

}
