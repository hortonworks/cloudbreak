package com.sequenceiq.cloudbreak.blueprint;

import java.io.IOException;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.google.common.collect.Maps;
import com.sequenceiq.cloudbreak.blueprint.template.BlueprintTemplateProcessor;
import com.sequenceiq.cloudbreak.service.CloudbreakServiceException;

import groovyx.net.http.HttpResponseException;

@Component
public class CentralBlueprintUpdater {

    private static final Logger LOGGER = LoggerFactory.getLogger(CentralBlueprintUpdater.class);

    @Inject
    private BlueprintTemplateProcessor blueprintTemplateProcessor;

    @Inject
    private BlueprintSegmentProcessor blueprintSegmentProcessor;

    @Inject
    private BlueprintComponentProviderProcessor blueprintComponentProviderProcessor;

    public String getBlueprintText(BlueprintPreparationObject source) throws CloudbreakServiceException, HttpResponseException {
        String blueprintText = source.getCluster().getBlueprint().getBlueprintText();
        try {
            blueprintText = updateBlueprintConfiguration(source, blueprintText);
        } catch (IOException e) {
            throw new CloudbreakServiceException(e);
        }
        return blueprintText;
    }

    private String updateBlueprintConfiguration(BlueprintPreparationObject source, String blueprint)
            throws IOException {
        blueprint = blueprintTemplateProcessor.process(blueprint, source, Maps.newHashMap());
        blueprint = blueprintSegmentProcessor.process(source, blueprint);
        blueprint = blueprintComponentProviderProcessor.process(source, blueprint);
        return blueprint;
    }
}
