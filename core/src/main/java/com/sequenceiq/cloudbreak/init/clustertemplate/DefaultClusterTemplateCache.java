package com.sequenceiq.cloudbreak.init.clustertemplate;

import static com.sequenceiq.cloudbreak.util.FileReaderUtils.readFileFromClasspath;

import java.io.File;
import java.io.IOException;
import java.util.Base64;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.api.endpoint.v4.clustertemplate.requests.DefaultClusterTemplateV4Request;
import com.sequenceiq.cloudbreak.api.util.ConverterUtil;
import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.cloudbreak.common.json.JsonUtil;
import com.sequenceiq.cloudbreak.domain.stack.cluster.ClusterTemplate;
import com.sequenceiq.cloudbreak.util.FileReaderUtils;

@Service
public class DefaultClusterTemplateCache {

    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultClusterTemplateCache.class);

    private final Map<String, String> defaultClusterTemplates = new HashMap<>();

    @Value("#{'${cb.clustertemplate.defaults:}'.split(',')}")
    private List<String> clusterTemplates;

    private String defaultTemplateDir = "defaults/clustertemplates";

    @Inject
    private ConverterUtil converterUtil;

    @PostConstruct
    public void loadClusterTemplatesFromFile() {
        if (clusterTemplates.stream().anyMatch(StringUtils::isNotEmpty)) {
            LOGGER.debug("Default clustertemplate to load into cache by property: {}", clusterTemplates);
            loadByProperty();
        } else {
            loadByResourceDir();
        }
    }

    private void loadByResourceDir() {
        List<String> files;
        try {
            files = FileReaderUtils.getFileNamesRecursivelyFromClasspathByDirPath(defaultTemplateDir, (dir, name) -> {
                boolean ret = name.endsWith(".json");
                if (!ret) {
                    LOGGER.info("The {} does not end with .json", name);
                }
                return ret;
            });
        } catch (IOException e) {
            LOGGER.warn("Failed to load files from: {}, original msg: {}", defaultTemplateDir, e.getMessage(), e);
            return;
        }
        if (!files.isEmpty()) {
            LOGGER.debug("Default clustertemplate to load into cache by resource dir: {}", String.join(", ", files));
            loadByNames(files);
        } else {
            LOGGER.debug("No default cluster template");
        }
    }

    private void loadByProperty() {
        loadByNames(clusterTemplates.stream().map(s -> defaultTemplateDir + File.separator + s).collect(Collectors.toList()));
    }

    private void loadByNames(Collection<String> names) {
        names.stream()
                .filter(StringUtils::isNotBlank)
                .forEach(clusterTemplateName -> {
                    try {
                        String templateAsString = readFileFromClasspath(clusterTemplateName);
                        convertToClusterTemplate(templateAsString);
                    } catch (IOException e) {
                        String msg = "Could not load cluster template: " + clusterTemplateName;
                        if (!clusterTemplateName.endsWith(".json")) {
                            msg += ". The json postfix is missing?";
                        }
                        LOGGER.warn(msg, e);
                    }
                });
    }

    private void convertToClusterTemplate(String templateAsString) throws IOException {
        DefaultClusterTemplateV4Request clusterTemplateRequest = new Json(templateAsString).get(DefaultClusterTemplateV4Request.class);
        if (defaultClusterTemplates.get(clusterTemplateRequest.getName()) != null) {
            LOGGER.warn("Default cluster template exists and it will be override: {}", clusterTemplateRequest.getName());
        }
        defaultClusterTemplates.put(clusterTemplateRequest.getName(), Base64.getEncoder().encodeToString(templateAsString.getBytes()));
    }

    public Map<String, String> defaultClusterTemplateRequests() {
        return defaultClusterTemplates;
    }

    public Map<String, ClusterTemplate> defaultClusterTemplates() {
        Map<String, ClusterTemplate> defaultTemplates = new HashMap<>();
        defaultClusterTemplateRequests().forEach((key, value) -> {
            String defaultTemplateJson = new String(Base64.getDecoder().decode(value));
            DefaultClusterTemplateV4Request defaultClusterTemplate = getDefaultClusterTemplate(defaultTemplateJson);
            ClusterTemplate clusterTemplate = converterUtil.convert(defaultClusterTemplate, ClusterTemplate.class);
            defaultTemplates.put(key, clusterTemplate);
        });
        return defaultTemplates;
    }

    private DefaultClusterTemplateV4Request getDefaultClusterTemplate(String defaultTemplateJson) {
        try {
            return JsonUtil.readValue(defaultTemplateJson, DefaultClusterTemplateV4Request.class);
        } catch (IOException e) {
            throw new IllegalArgumentException("Default cluster template could not be added, causes: " + e.getMessage(), e);
        }
    }

    protected void setClusterTemplates(List<String> clusterTemplates) {
        this.clusterTemplates = clusterTemplates;
    }

    protected void setDefaultTemplateDir(String defaultTemplateDir) {
        this.defaultTemplateDir = defaultTemplateDir;
    }

    public String getByName(String name) {
        return defaultClusterTemplates.get(name);
    }
}
