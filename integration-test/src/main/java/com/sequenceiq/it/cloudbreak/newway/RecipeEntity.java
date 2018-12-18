package com.sequenceiq.it.cloudbreak.newway;

import java.util.Base64;
import java.util.List;
import java.util.stream.Collectors;

import javax.ws.rs.WebApplicationException;

import com.sequenceiq.cloudbreak.api.endpoint.v3.RecipeV3Endpoint;
import com.sequenceiq.cloudbreak.api.model.RecipeRequest;
import com.sequenceiq.cloudbreak.api.model.RecipeResponse;
import com.sequenceiq.cloudbreak.api.model.RecipeType;
import com.sequenceiq.cloudbreak.api.model.RecipeViewResponse;
import com.sequenceiq.it.cloudbreak.newway.context.Purgable;
import com.sequenceiq.it.cloudbreak.newway.context.TestContext;

public class RecipeEntity extends AbstractCloudbreakEntity<RecipeRequest, RecipeResponse, RecipeEntity> implements Purgable<RecipeViewResponse> {
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
                .withContent(new String(Base64.getEncoder().encode("#!/bin/bash%necho ALMAA".getBytes())));
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

    @Override
    public List<RecipeViewResponse> getAll(CloudbreakClient client) {
        RecipeV3Endpoint recipeV3Endpoint = client.getCloudbreakClient().recipeV3Endpoint();
        return recipeV3Endpoint.listByWorkspace(client.getWorkspaceId()).stream()
                .filter(s -> s.getName() != null)
                .collect(Collectors.toList());
    }

    @Override
    public boolean deletable(RecipeViewResponse entity) {
        return entity.getName().startsWith("mock-");
    }

    @Override
    public void delete(RecipeViewResponse entity, CloudbreakClient client) {
        try {
            client.getCloudbreakClient().recipeV3Endpoint().deleteInWorkspace(client.getWorkspaceId(), entity.getName());
        } catch (Exception e) {
            LOGGER.warn("Something went wrong on {} purge. {}", entity.getName(), e.getMessage(), e);
        }
    }

    @Override
    public int order() {
        return 500;
    }
}