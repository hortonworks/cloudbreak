package com.sequenceiq.cloudbreak.init.clustertemplate;

import static com.sequenceiq.cloudbreak.util.FileReaderUtils.readFileFromClasspath;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import org.apache.commons.lang3.SerializationUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.api.endpoint.v4.clustertemplate.requests.DefaultClusterTemplateV4Request;
import com.sequenceiq.cloudbreak.api.util.ConverterUtil;
import com.sequenceiq.cloudbreak.domain.json.Json;
import com.sequenceiq.cloudbreak.domain.stack.cluster.ClusterTemplate;
import com.sequenceiq.cloudbreak.util.FileReaderUtils;

@Service
public class DefaultClusterTemplateCache {

    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultClusterTemplateCache.class);

    private final Map<String, DefaultClusterTemplateV4Request> defaultClusterTemplates = new HashMap<>();

    @Value("#{'${cb.clustertemplate.defaults:}'.split(',')}")
    private List<String> clusterTemplates;

    private String defaultTemplateDir = "defaults/clustertemplates";

    @Inject
    private ConverterUtil converterUtil;

    @PostConstruct
    public void loadClusterTemplatesFromFile() {
        if (clusterTemplates.stream().anyMatch(StringUtils::isNoneEmpty)) {
            LOGGER.debug("Default clustertemplate to load into cache by property: {}", clusterTemplates);
            loadByProperty();
        } else {
            loadByResourceDir();
        }
    }

    private void loadByResourceDir() {
        List<String> files;
        try {
            files = FileReaderUtils.getFileNamesRecursivelyFromClasspathByDirPath(defaultTemplateDir, (dir, name) -> name.endsWith(".json"));
        } catch (IOException e) {
            LOGGER.warn(e.getMessage());
            return;
        }
        if (!files.isEmpty()) {
            LOGGER.debug("Default clustertemplate to load into cache by property: {}", String.join(", ", files));
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
        defaultClusterTemplates.put(clusterTemplateRequest.getName(), clusterTemplateRequest);
    }

    public Map<String, DefaultClusterTemplateV4Request> defaultClusterTemplateRequests() {
        Map<String, DefaultClusterTemplateV4Request> ret = new HashMap<>();
        defaultClusterTemplates.forEach((key, value) -> ret.put(key, SerializationUtils.clone(value)));
        return ret;
    }

    public Map<String, ClusterTemplate> defaultClusterTemplates() {
        Map<String, ClusterTemplate> defaultTemplates = new HashMap<>();
        defaultClusterTemplateRequests().forEach((key, value) -> {
            ClusterTemplate clusterTemplate = converterUtil.convert(value, ClusterTemplate.class);
            defaultTemplates.put(key, clusterTemplate);
        });
        return defaultTemplates;
    }

    protected void setClusterTemplates(List<String> clusterTemplates) {
        this.clusterTemplates = clusterTemplates;
    }

    protected void setDefaultTemplateDir(String defaultTemplateDir) {
        this.defaultTemplateDir = defaultTemplateDir;
    }
}
