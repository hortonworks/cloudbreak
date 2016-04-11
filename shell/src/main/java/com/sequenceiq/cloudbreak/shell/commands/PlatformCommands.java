package com.sequenceiq.cloudbreak.shell.commands;

import java.io.File;
import java.util.Map;

public interface PlatformCommands {

    String create(String name, String description, String cloudPlatform, Map<String, String> mapping);

    Map<String, String> convertMappingFile(File file, String url);

    boolean createPlatformAvailable(String platform);
}
