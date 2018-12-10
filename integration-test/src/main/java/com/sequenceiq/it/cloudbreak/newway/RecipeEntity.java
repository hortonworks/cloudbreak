package com.sequenceiq.it.cloudbreak.newway;

import com.sequenceiq.cloudbreak.api.model.RecipeRequest;
import com.sequenceiq.cloudbreak.api.model.RecipeResponse;
import com.sequenceiq.cloudbreak.api.model.RecipeType;
import com.sequenceiq.it.cloudbreak.newway.context.TestContext;

import javax.ws.rs.WebApplicationException;

public class RecipeEntity extends AbstractCloudbreakEntity<RecipeRequest, RecipeResponse, RecipeEntity> {
    public static final String RECIPE = "RECIPE";

    public RecipeEntity(String newId) {
        super(newId);
        setRequest(new RecipeRequest());
    }

    public RecipeEntity() {
        this(RECIPE);
    }

    public RecipeEntity(TestContext testContext) {
        super(new RecipeRequest(), testContext);
    }

    @Override
    public void cleanUp(TestContext context, CloudbreakClient cloudbreakClient) {
        LOGGER.info("Cleaning up resource with name: {}", getName());
        try {
            cloudbreakClient.getCloudbreakClient().recipeV3Endpoint().deleteInWorkspace(cloudbreakClient.getWorkspaceId(), getName());
        } catch (WebApplicationException ignore) {
            LOGGER.info("Something happend.");
        }
    }

    public RecipeEntity valid() {
        return withName(getNameCreator().getRandomNameForMock())
                .withRecipeType(RecipeType.PRE_AMBARI_START)
                .withContent(String.format("#!/bin/bash%necho ALMAA"));
    }

    public RecipeEntity withName(String name) {
        getRequest().setName(name);
        setName(name);
        return this;
    }

    public RecipeEntity withDescription(String description) {
        getRequest().setDescription(description);
        return this;
    }

    public RecipeEntity withContent(String content) {
        getRequest().setContent(content);
        return this;
    }

    public RecipeEntity withRecipeType(RecipeType recipeType) {
        getRequest().setRecipeType(recipeType);
        return this;
    }
}
