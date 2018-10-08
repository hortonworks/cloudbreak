package com.sequenceiq.cloudbreak.init.clustertemplate;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import org.apache.commons.lang3.SerializationUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.api.model.ResourceStatus;
import com.sequenceiq.cloudbreak.api.model.template.ClusterTemplateRequest;
import com.sequenceiq.cloudbreak.converter.mapper.ClusterTemplateMapper;
import com.sequenceiq.cloudbreak.domain.json.Json;
import com.sequenceiq.cloudbreak.domain.stack.cluster.ClusterTemplate;
import com.sequenceiq.cloudbreak.util.FileReaderUtils;

@Service
public class DefaultClusterTemplateCache {

    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultClusterTemplateCache.class);

    private final Map<String, ClusterTemplate> defaultClusterTemplates = new HashMap<>();

    @Value("#{'${cb.clustertemplate.defaults:}'.split(',')}")
    private List<String> clusterTemplates;

    @Inject
    private ClusterTemplateMapper clusterTemplateMapper;

    @PostConstruct
    public void loadClusterTemplatesFromFile() {
        LOGGER.debug("Default clustertemplate to load into cache: {}", clusterTemplates);
        for (String clusterTemplateName : clusterTemplates) {
            if (StringUtils.isNotBlank(clusterTemplateName)) {
                try {
                    String templateAsString = FileReaderUtils.readFileFromClasspath("defaults/clustertemplates/" + clusterTemplateName + ".json");
                    ClusterTemplateRequest clusterTemplateRequest = new Json(templateAsString).get(ClusterTemplateRequest.class);
                    ClusterTemplate clusterTemplate = clusterTemplateMapper.mapRequestToEntity(clusterTemplateRequest);
                    clusterTemplate.setStatus(ResourceStatus.DEFAULT);
                    defaultClusterTemplates.put(clusterTemplate.getName(), clusterTemplate);
                } catch (IOException e) {
                    LOGGER.warn("Could not load cluster template: " + clusterTemplateName, e);
                }
            }
        }
    }

    public Map<String, ClusterTemplate> defaultClusterTemplates() {
        Map<String, ClusterTemplate> result = new HashMap<>();
        defaultClusterTemplates.forEach((key, value) -> result.put(key, SerializationUtils.clone(value)));
        return result;
    }
}
