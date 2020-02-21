package com.sequenceiq.cloudbreak.tag;

import java.io.IOException;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class CentralTagUpdater {

    private static final Logger LOGGER = LoggerFactory.getLogger(CentralTagUpdater.class);

    @Inject
    private TagTemplateProcessor tagTemplateProcessor;

    public String getTagText(TagPreparationObject model, String tagText) {
        try {
            tagText = updateTagConfiguration(model, tagText);
        } catch (IOException e) {
            String message = String.format("Unable to update tag with default properties which was: %s", tagText);
            LOGGER.warn(message, e);
            throw new TagProcessingException(message, e);
        }
        return tagText;
    }

    private String updateTagConfiguration(TagPreparationObject model, String sourceTemplate)
            throws IOException {
        sourceTemplate = tagTemplateProcessor.process(sourceTemplate, model);
        return sourceTemplate;
    }
}
