package com.sequenceiq.cloudbreak.blueprint;

import static com.sequenceiq.cloudbreak.blueprint.templates.ServiceName.serviceName;
import static com.sequenceiq.cloudbreak.blueprint.templates.TemplateFiles.templateFiles;
import static com.sequenceiq.cloudbreak.util.FileReaderUtils.readFolderFromClasspath;

import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ResourceLoaderAware;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;

import com.google.common.collect.Lists;
import com.sequenceiq.cloudbreak.blueprint.templates.ServiceName;
import com.sequenceiq.cloudbreak.blueprint.templates.TemplateFiles;

@Component
public class BlueprintSegmentReader implements ResourceLoaderAware {

    private static final Logger LOGGER = LoggerFactory.getLogger(BlueprintSegmentReader.class);

    @Value("${cb.blueprint.template.path:blueprints/configurations}")
    private String blueprintTemplatePath;

    @Value("${cb.blueprint.basic.path:blueprints/basics}")
    private String basicTemplatePath;

    @Value("${cb.blueprint.basic.path:blueprints/settings}")
    private String settingsTemplatePath;

    private ResourceLoader resourceLoader;

    public Map<ServiceName, TemplateFiles> collectAllServiceFile() {
        return readAllFilesFromParameterDir(blueprintTemplatePath);
    }

    public Map<ServiceName, TemplateFiles> collectAllConfigFile() {
        return readAllFilesFromParameterDir(basicTemplatePath);
    }

    public Map<ServiceName, TemplateFiles> collectAllSettingsFile() {
        return readAllFilesFromParameterDir(settingsTemplatePath);
    }

    private Map<ServiceName, TemplateFiles> readAllFilesFromParameterDir(String dir) {
        Map<ServiceName, TemplateFiles> collectedFiles = new HashMap<>();
        try {
            List<URL> files = getFiles(dir);
            if (files != null) {
                for (final URL serviceEntry : files) {
                    String[] serviceAndPath = serviceEntry.getPath().split(dir);
                    String insideFolder = String.format("%s/%s", dir, serviceAndPath[1].replaceAll("/", ""));
                    for (final URL configEntry : getFiles(insideFolder)) {
                        String serviceName = serviceAndPath[1].replace("/", "");
                        if (!collectedFiles.keySet().contains(serviceName(serviceName))) {
                            collectedFiles.put(serviceName(serviceName), templateFiles(Lists.newArrayList()));
                        }
                        String file = configEntry.getPath().replace(serviceEntry.getPath() + "/", "");
                        collectedFiles.get(serviceName(serviceName)).getFiles().add(String.format("%s/%s/%s", dir, serviceName, file));
                    }
                }
            }
        } catch (IOException ex) {
            String message = String.format("Could not read files from the definiated folder which was: %s", dir);
            LOGGER.warn(message, ex);
            throw new BlueprintProcessingException(message, ex);
        }

        return collectedFiles;
    }

    private List<URL> getFiles(String configDir) throws IOException {
        return readFolderFromClasspath(configDir);
    }

    @Override
    public void setResourceLoader(ResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
    }
}
