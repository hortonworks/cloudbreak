package com.sequenceiq.cloudbreak.recipe;

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
public class CentralRecipeUpdater {

    private static final Logger LOGGER = LoggerFactory.getLogger(CentralRecipeUpdater.class);

    @Inject
    private TemplateProcessor templateProcessor;

    public String getRecipeText(TemplatePreparationObject source, String recipeText) {
        try {
            recipeText = updateRecipeConfiguration(source, recipeText);
        } catch (IOException e) {
            String message = String.format("Unable to update recipe with default properties which was: %s", recipeText);
            LOGGER.warn(message);
            throw new ClusterDefinitionProcessingException(message, e);
        }
        return recipeText;
    }

    private String updateRecipeConfiguration(TemplatePreparationObject source, String recipe)
            throws IOException {
        recipe = templateProcessor.process(recipe, source, Maps.newHashMap());
        return recipe;
    }
}
