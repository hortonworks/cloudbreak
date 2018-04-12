package com.sequenceiq.cloudbreak.blueprint;

import com.google.common.collect.Maps;
import com.sequenceiq.cloudbreak.templateprocessor.processor.TemplatePreparationObject;
import com.sequenceiq.cloudbreak.templateprocessor.processor.TemplateProcessingException;
import com.sequenceiq.cloudbreak.templateprocessor.template.TemplateProcessor;
import groovyx.net.http.HttpResponseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.io.IOException;

@Component
public class CentralBlueprintUpdater {

    private static final Logger LOGGER = LoggerFactory.getLogger(CentralBlueprintUpdater.class);

    @Inject
    private TemplateProcessor blueprintTemplateProcessor;

    @Inject
    private BlueprintSegmentProcessor blueprintSegmentProcessor;

    @Inject
    private BlueprintComponentProviderProcessor blueprintComponentProviderProcessor;

    public String getBlueprintText(TemplatePreparationObject source) throws TemplateProcessingException, HttpResponseException {
        String blueprintText = source.getBlueprintView().getBlueprintText();
        try {
            blueprintText = updateBlueprintConfiguration(source, blueprintText);
        } catch (IOException e) {
            String message = String.format("Unable to update blueprint with default  properties which was: %s", blueprintText);
            LOGGER.warn(message);
            throw new TemplateProcessingException(message, e);
        }
        return blueprintText;
    }

    private String updateBlueprintConfiguration(TemplatePreparationObject source, String blueprint)
            throws IOException {
        blueprint = blueprintTemplateProcessor.process(blueprint, source, Maps.newHashMap());
        blueprint = blueprintSegmentProcessor.process(blueprint, source);
        blueprint = blueprintComponentProviderProcessor.process(source, blueprint);
        return blueprint;
    }
}
