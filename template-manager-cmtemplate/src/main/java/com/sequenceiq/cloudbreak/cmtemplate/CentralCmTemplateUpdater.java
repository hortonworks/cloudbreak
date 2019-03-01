package com.sequenceiq.cloudbreak.cmtemplate;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.cloudera.api.swagger.model.ApiClusterTemplate;
import com.google.common.collect.Maps;
import com.sequenceiq.cloudbreak.cluster.api.ClusterApi;
import com.sequenceiq.cloudbreak.template.ClusterDefinitionProcessingException;
import com.sequenceiq.cloudbreak.template.ClusterDefinitionUpdater;
import com.sequenceiq.cloudbreak.template.TemplatePreparationObject;
import com.sequenceiq.cloudbreak.template.TemplateProcessor;
import com.sequenceiq.cloudbreak.util.JsonUtil;

@Component
public class CentralCmTemplateUpdater implements ClusterDefinitionUpdater {

    private static final Logger LOGGER = LoggerFactory.getLogger(CentralCmTemplateUpdater.class);

    @Inject
    private TemplateProcessor templateProcessor;

    @Inject
    private CmTemplateProcessorFactory cmTemplateProcessorFactory;

    @Inject
    private CmTemplateComponentConfigProcessor cmTemplateComponentConfigProcessor;

    public ApiClusterTemplate getCmTemplate(TemplatePreparationObject source, Map<String, List<Map<String, String>>> hostGroupMappings) {
        try {
            CmTemplateProcessor cmTemplate = updateCmTemplateConfiguration(source, hostGroupMappings);
            return cmTemplate.getTemplate();
        } catch (IOException e) {
            String message = String.format("Unable to update cmTemplate with default properties which was: %s",
                    source.getClusterDefinitionView().getClusterDefinitionText());
            LOGGER.warn(message);
            throw new ClusterDefinitionProcessingException(message, e);
        }
    }

    private CmTemplateProcessor updateCmTemplateConfiguration(TemplatePreparationObject source, Map<String, List<Map<String, String>>> hostGroupMappings)
            throws IOException {
        String cmTemplate = source.getClusterDefinitionView().getClusterDefinitionText();
        cmTemplate = templateProcessor.process(cmTemplate, source, Maps.newHashMap());
        CmTemplateProcessor processor = cmTemplateProcessorFactory.get(cmTemplate);
        processor.addInstantiator(source.getGeneralClusterConfigs().getClusterName());
        processor.addHosts(hostGroupMappings);
        processor = cmTemplateComponentConfigProcessor.process(processor, source);
        return processor;
    }

    @Override
    public String getClusterDefinitionText(TemplatePreparationObject source) {
        ApiClusterTemplate template = getCmTemplate(source, Map.of());
        return JsonUtil.writeValueAsStringSilent(template);
    }

    @Override
    public String getVariant() {
        return ClusterApi.CLOUDERA_MANAGER;
    }
}
