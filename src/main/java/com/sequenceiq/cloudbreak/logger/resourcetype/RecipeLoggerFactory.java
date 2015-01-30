package com.sequenceiq.cloudbreak.logger.resourcetype;

import org.slf4j.MDC;

import com.sequenceiq.cloudbreak.domain.Recipe;
import com.sequenceiq.cloudbreak.logger.LoggerContextKey;
import com.sequenceiq.cloudbreak.logger.LoggerResourceType;

public class RecipeLoggerFactory {

    private RecipeLoggerFactory() {

    }

    public static void buildMdcContext(Recipe recipe) {
        if (recipe.getOwner() != null) {
            MDC.put(LoggerContextKey.OWNER_ID.toString(), recipe.getOwner());
        }
        MDC.put(LoggerContextKey.RESOURCE_TYPE.toString(), LoggerResourceType.RECIPE.toString());
        if (recipe.getName() != null) {
            MDC.put(LoggerContextKey.RESOURCE_NAME.toString(), recipe.getName());
        }
        if (recipe.getId() == null) {
            MDC.put(LoggerContextKey.RESOURCE_ID.toString(), "null");
        } else {
            MDC.put(LoggerContextKey.RESOURCE_ID.toString(), recipe.getId().toString());
        }
    }
}
