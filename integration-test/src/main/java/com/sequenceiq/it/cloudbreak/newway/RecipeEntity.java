package com.sequenceiq.it.cloudbreak.newway;

import java.util.Base64;
import java.util.List;
import java.util.stream.Collectors;

import javax.ws.rs.WebApplicationException;

import com.sequenceiq.cloudbreak.api.endpoint.v4.recipes.RecipeV4Endpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v4.recipes.requests.RecipeV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.recipes.requests.RecipeV4Type;
import com.sequenceiq.cloudbreak.api.endpoint.v4.recipes.responses.RecipeV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.recipes.responses.RecipeV4ViewResponse;
import com.sequenceiq.it.cloudbreak.newway.context.Purgable;
import com.sequenceiq.it.cloudbreak.newway.context.TestContext;

@Prototype
public class RecipeEntity extends AbstractCloudbreakEntity<RecipeV4Request, RecipeV4Response, RecipeEntity> implements Purgable<RecipeV4ViewResponse> {
    public static final String RECIPE = "RECIPE";

    public RecipeEntity(String newId) {
        super(newId);
        setRequest(new RecipeV4Request());
    }

    public RecipeEntity() {
        this(RECIPE);
    }

    public RecipeEntity(TestContext testContext) {
        super(new RecipeV4Request(), testContext);
    }

    @Override
    public void cleanUp(TestContext context, CloudbreakClient cloudbreakClient) {
        LOGGER.info("Cleaning up resource with name: {}", getName());
        try {
            cloudbreakClient.getCloudbreakClient().recipeV4Endpoint().delete(cloudbreakClient.getWorkspaceId(), getName());
        } catch (WebApplicationException ignore) {
            LOGGER.info("Something happend.");
        }
    }

    public RecipeEntity valid() {
        return withName(getNameCreator().getRandomNameForMock())
                .withRecipeType(RecipeV4Type.PRE_CLUSTER_MANAGER_START)
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

    public RecipeEntity withRecipeType(RecipeV4Type recipeType) {
        getRequest().setType(recipeType);
        return this;
    }

    @Override
    public List<RecipeV4ViewResponse> getAll(CloudbreakClient client) {
        RecipeV4Endpoint recipeV4Endpoint = client.getCloudbreakClient().recipeV4Endpoint();
        return recipeV4Endpoint.list(client.getWorkspaceId()).getResponses().stream()
                .filter(s -> s.getName() != null)
                .collect(Collectors.toList());
    }

    @Override
    public boolean deletable(RecipeV4ViewResponse entity) {
        return entity.getName().startsWith("mock-");
    }

    @Override
    public void delete(RecipeV4ViewResponse entity, CloudbreakClient client) {
        try {
            client.getCloudbreakClient().recipeV4Endpoint().delete(client.getWorkspaceId(), entity.getName());
        } catch (Exception e) {
            LOGGER.warn("Something went wrong on {} purge. {}", entity.getName(), e.getMessage(), e);
        }
    }

    @Override
    public int order() {
        return 500;
    }
}