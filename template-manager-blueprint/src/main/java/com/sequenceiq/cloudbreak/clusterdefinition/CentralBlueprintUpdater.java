package com.sequenceiq.cloudbreak.clusterdefinition;

import java.io.IOException;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.google.common.collect.Maps;
import com.sequenceiq.cloudbreak.template.ClusterDefinitionProcessingException;
import com.sequenceiq.cloudbreak.template.TemplatePreparationObject;
import com.sequenceiq.cloudbreak.template.TemplateProcessor;

@Component
public class CentralBlueprintUpdater {

    private static final Logger LOGGER = LoggerFactory.getLogger(CentralBlueprintUpdater.class);

    @Inject
    private TemplateProcessor templateProcessor;

    @Inject
    private AmbariBlueprintSegmentProcessor ambariBlueprintSegmentProcessor;

    @Inject
    private AmbariBlueprintComponentProviderProcessor ambariBlueprintComponentProviderProcessor;

    public String getBlueprintText(TemplatePreparationObject source) {
        String blueprintText = source.getClusterDefinitionView().getBlueprintText();
        try {
            blueprintText = updateBlueprintConfiguration(source, blueprintText);
        } catch (IOException e) {
            String message = String.format("Unable to update cluster definition with default properties which was: %s", blueprintText);
            LOGGER.warn(message);
            throw new ClusterDefinitionProcessingException(message, e);
        }
        return blueprintText;
    }

    private String updateBlueprintConfiguration(TemplatePreparationObject source, String blueprint)
            throws IOException {
        blueprint = templateProcessor.process(blueprint, source, Maps.newHashMap());
        blueprint = ambariBlueprintSegmentProcessor.process(blueprint, source);
        blueprint = ambariBlueprintComponentProviderProcessor.process(source, blueprint);
        return blueprint;
    }
}
