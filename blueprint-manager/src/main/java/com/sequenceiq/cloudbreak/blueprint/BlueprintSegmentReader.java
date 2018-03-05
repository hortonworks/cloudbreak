package com.sequenceiq.cloudbreak.blueprint;

import static com.sequenceiq.cloudbreak.blueprint.templates.ServiceName.serviceName;
import static com.sequenceiq.cloudbreak.blueprint.templates.TemplateFiles.templateFiles;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
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

    @Value("${cb.blueprint.template.path:blueprints}")
    private String blueprintTemplatePath;

    @Value("${cb.blueprint.basic.path:basics}")
    private String basicTemplatePath;

    private ResourceLoader resourceLoader;

    public Map<ServiceName, TemplateFiles> collectAllServiceFile() {
        return readAllFilesFromParameterDir(blueprintTemplatePath);
    }

    public Map<ServiceName, TemplateFiles> collectAllConfigFile() {
        return readAllFilesFromParameterDir(basicTemplatePath);
    }

    private Map<ServiceName, TemplateFiles> readAllFilesFromParameterDir(String dir) {
        Map<ServiceName, TemplateFiles> collectedFiles = new HashMap<>();
        try {
            File[] allFileInDir = getFiles(dir);
            if (allFileInDir != null) {
                for (final File serviceEntry : allFileInDir) {
                    String insideFolder = String.format("%s/%s", dir, serviceEntry.getName());
                    for (final File configEntry : getFiles(insideFolder)) {
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
        } catch (IOException ex) {
            LOGGER.warn("Could not read files from the definiated folder which was: {}", dir, ex);
        }

        return collectedFiles;
    }

    private File[] getFiles(String configDir) throws IOException {
        File folder = resourceLoader.getResource("classpath:" + configDir).getFile();
        File[] listOfFiles = folder.listFiles();
        return listOfFiles == null ? new File[0] : listOfFiles;
    }

    @Override
    public void setResourceLoader(ResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
    }
}
