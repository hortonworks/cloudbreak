package com.sequenceiq.cloudbreak.service.cluster.flow;

import java.util.List;
import java.util.Map;

import com.sequenceiq.cloudbreak.domain.FileSystemType;

public interface FileSystemConfigurator {

    List<BlueprintConfigurationEntry> getBlueprintProperties(Map<String, String> fsProperties);

    String getDefaultFsValue(Map<String, String> fsProperties);

    List<FileSystemScript> getScripts();

    FileSystemType getFileSystemType();
}
