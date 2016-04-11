package com.sequenceiq.cloudbreak.shell.commands;

import java.util.Map;


public interface TemplateCommands {

    String create(String name, String instanceType, Integer volumeCount, Integer volumeSize, String volumeType, Boolean publicInAccount, String description,
            Map<String, Object> parameters, Long platformId, String platform);

    boolean createTemplateAvailable(String platform);
}
