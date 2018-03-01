package com.sequenceiq.cloudbreak.blueprint;

import static com.sequenceiq.cloudbreak.blueprint.templates.ServiceName.serviceName;
import static com.sequenceiq.cloudbreak.blueprint.templates.TemplateFiles.templateFiles;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.google.common.collect.Lists;
import com.sequenceiq.cloudbreak.blueprint.templates.ServiceName;
import com.sequenceiq.cloudbreak.blueprint.templates.TemplateFiles;

@Component
public class BlueprintSegmentReader {

    private static final Logger LOGGER = LoggerFactory.getLogger(BlueprintSegmentReader.class);

    @Value("${cb.blueprint.template.path:blueprints}")
    private String blueprintTemplatePath;

    @Value("${cb.blueprint.basic.path:basics}")
    private String basicTemplatePath;

    public Map<ServiceName, TemplateFiles> collectAllServiceFile() {
        return readAllFilesFromParameterDir(blueprintTemplatePath);
    }

    public Map<ServiceName, TemplateFiles> collectAllConfigFile() {
        return readAllFilesFromParameterDir(basicTemplatePath);
    }

    private Map<ServiceName, TemplateFiles> readAllFilesFromParameterDir(String dir) {
        Map<ServiceName, TemplateFiles> collectedFiles = new HashMap<>();
        File allFileInDir = new File(String.format("src/main/resources/%s", dir));
        if (allFileInDir != null) {
            for (final File serviceEntry : getFiles(allFileInDir)) {
                for (final File configEntry : getFiles(new File(serviceEntry.getPath()))) {
                    if (configEntry.isFile()) {
                        String serviceName = serviceEntry.getPath().replace(serviceEntry.getParent() + "/", "");
                        if (!collectedFiles.keySet().contains(serviceName(serviceName))) {
                            collectedFiles.put(serviceName(serviceName), templateFiles(Lists.newArrayList()));
                        }
                        String file = configEntry.getPath().replace(serviceEntry.getParent() + "/", "");
                        collectedFiles.get(serviceName(serviceName)).getFiles().add(String.format("%s/%s", dir, file));
                    }
                }
            }
        }
        return collectedFiles;
    }

    private File[] getFiles(File allFileInDir) {
        return allFileInDir.listFiles() == null ? new File[0] : allFileInDir.listFiles();
    }

}
