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
import com.sequenceiq.cloudbreak.template.BlueprintProcessingException;
import com.sequenceiq.cloudbreak.template.TemplatePreparationObject;
import com.sequenceiq.cloudbreak.template.TemplateProcessor;

@Component
public class CentralCmTemplateUpdater {

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
            String message = String.format("Unable to update cmTemplate with default properties which was: %s", source.getBlueprintView().getBlueprintText());
            LOGGER.warn(message);
            throw new BlueprintProcessingException(message, e);
        }
    }

    private CmTemplateProcessor updateCmTemplateConfiguration(TemplatePreparationObject source, Map<String, List<Map<String, String>>> hostGroupMappings)
            throws IOException {
        String cmTemplate = source.getBlueprintView().getBlueprintText();
        cmTemplate = templateProcessor.process(cmTemplate, source, Maps.newHashMap());
        CmTemplateProcessor processor = cmTemplateProcessorFactory.get(cmTemplate);
        processor.addInstantiator(source.getGeneralClusterConfigs().getClusterName());
        processor.addHosts(hostGroupMappings);
        processor = cmTemplateComponentConfigProcessor.process(processor, source);
        return processor;
    }

}
