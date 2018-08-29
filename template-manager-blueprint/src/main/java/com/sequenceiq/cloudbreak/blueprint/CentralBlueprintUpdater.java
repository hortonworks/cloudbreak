package com.sequenceiq.cloudbreak.blueprint;

import java.io.IOException;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.google.common.collect.Maps;
import com.sequenceiq.cloudbreak.template.BlueprintProcessingException;
import com.sequenceiq.cloudbreak.template.TemplatePreparationObject;
import com.sequenceiq.cloudbreak.template.TemplateProcessor;

@Component
public class CentralBlueprintUpdater {

    private static final Logger LOGGER = LoggerFactory.getLogger(CentralBlueprintUpdater.class);

    @Inject
    private TemplateProcessor templateProcessor;

    @Inject
    private BlueprintSegmentProcessor blueprintSegmentProcessor;

    @Inject
    private BlueprintComponentProviderProcessor blueprintComponentProviderProcessor;

    public String getBlueprintText(TemplatePreparationObject source) {
        String blueprintText = source.getBlueprintView().getBlueprintText();
        try {
            blueprintText = updateBlueprintConfiguration(source, blueprintText);
        } catch (IOException e) {
            String message = String.format("Unable to update blueprint with default properties which was: %s", blueprintText);
            LOGGER.warn(message);
            throw new BlueprintProcessingException(message, e);
        }
        return blueprintText;
    }

    private String updateBlueprintConfiguration(TemplatePreparationObject source, String blueprint)
            throws IOException {
        blueprint = templateProcessor.process(blueprint, source, Maps.newHashMap());
        blueprint = blueprintSegmentProcessor.process(blueprint, source);
        blueprint = blueprintComponentProviderProcessor.process(source, blueprint);
        return blueprint;
    }
}
