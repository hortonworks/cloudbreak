package com.sequenceiq.cloudbreak.init.clustertemplate;

import static com.sequenceiq.cloudbreak.util.FileReaderUtils.readFileFromClasspath;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
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
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.api.endpoint.v4.clustertemplate.requests.DefaultClusterTemplateV4Request;
import com.sequenceiq.cloudbreak.api.util.ConverterUtil;
import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.cloudbreak.common.json.JsonUtil;
import com.sequenceiq.cloudbreak.domain.stack.cluster.ClusterTemplate;

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
            LOGGER.debug("Default cluster template is loaded into cache by property: {}", clusterTemplates);
            loadByProperty();
        } else {
            loadByResourceDir();
        }
    }

    private void loadByResourceDir() {
        List<String> files;
        try {
            files = getFiles();
        } catch (Exception e) {
            LOGGER.warn("Failed to load files from: {}, original msg: {}", defaultTemplateDir, e.getMessage(), e);
            return;
        }
        if (!files.isEmpty()) {
            LOGGER.debug("Default clustertemplate is loaded into cache by resource dir: {}", String.join(", ", files));
            loadByClasspathPath(files);
        } else {
            LOGGER.debug("No default cluster template");
        }
    }

    private void loadByProperty() {
        loadByClasspathPath(clusterTemplates
                .stream()
                .filter(StringUtils::isNotBlank)
                .map(s -> defaultTemplateDir + File.separator + s.trim()).collect(Collectors.toList()));
    }

    private void loadByClasspathPath(Collection<String> names) {
        names.stream()
                .filter(StringUtils::isNotBlank)
                .forEach(clusterTemplateName -> {
                    try {
                        String templateAsString = readFileFromClasspath(clusterTemplateName);
                        convertToClusterTemplate(templateAsString);
                        LOGGER.debug("Default clustertemplate is loaded into cache by resource file: {}", clusterTemplateName);
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

    public DefaultClusterTemplateV4Request getDefaultClusterTemplate(String defaultTemplateJson) {
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

    private List<String> getFiles() throws IOException {
        ResourcePatternResolver patternResolver = new PathMatchingResourcePatternResolver();
        return Arrays.stream(patternResolver.getResources("classpath:" + defaultTemplateDir + "/**/*.json"))
                .map(resource -> {
                    try {
                        String[] path = resource.getURL().getPath().split(defaultTemplateDir);
                        return String.format("%s%s", defaultTemplateDir, path[1]);
                    } catch (IOException e) {
                        // wrap to runtime exception because of lambda and log the error in the caller method.
                        throw new RuntimeException(e);
                    }
                }).collect(Collectors.toList());
    }
}
