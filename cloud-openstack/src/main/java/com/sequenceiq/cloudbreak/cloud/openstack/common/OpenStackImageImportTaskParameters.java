package com.sequenceiq.cloudbreak.cloud.openstack.common;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class OpenStackImageImportTaskParameters {

    @Value("${cb.os.import.root.url.pattern}")
    private String rootUrlPattern;

    @Value("${cb.os.import.from.format}")
    private String importFromFormat;

    @Value("${cb.os.import.disk.format}")
    private String diskFormat;

    @Value("${cb.os.import.container.format}")
    private String containerFormat;

    public String getImportLocation(String name) {
        return String.format(rootUrlPattern, name);
    }

    public Map<String, Object> buildInput(String name) {
        Map<String, Object> input = new HashMap<>();
        input.put("import_from", getImportLocation(name));
        input.put("import_from_format", importFromFormat);
        Map<String, String> imageProperties = new HashMap<>();
        imageProperties.put("disk_format", diskFormat);
        imageProperties.put("container_format", containerFormat);
        imageProperties.put("name", name);
        input.put("image_properties", imageProperties);
        return input;
    }
}
