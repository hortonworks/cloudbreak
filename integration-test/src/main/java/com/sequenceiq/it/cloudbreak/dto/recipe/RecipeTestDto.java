package com.sequenceiq.it.cloudbreak.dto.recipe;

import static com.sequenceiq.it.cloudbreak.context.RunningParameter.key;

import java.util.Base64;
import java.util.List;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.ws.rs.WebApplicationException;

import com.sequenceiq.cloudbreak.api.endpoint.v4.recipes.RecipeV4Endpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v4.recipes.requests.RecipeV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.recipes.requests.RecipeV4Type;
import com.sequenceiq.cloudbreak.api.endpoint.v4.recipes.responses.RecipeV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.recipes.responses.RecipeViewV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.recipes.responses.RecipeViewV4Responses;
import com.sequenceiq.it.cloudbreak.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.MicroserviceClient;
import com.sequenceiq.it.cloudbreak.Prototype;
import com.sequenceiq.it.cloudbreak.client.RecipeTestClient;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.DeletableTestDto;
import com.sequenceiq.it.cloudbreak.util.ResponseUtil;

@Prototype
public class RecipeTestDto extends DeletableTestDto<RecipeV4Request, RecipeV4Response, RecipeTestDto, RecipeViewV4Response> {

    private static final String RECIPE_RESOURCE_NAME = "recipeName";

    private RecipeViewV4Responses simpleResponses;

    @Inject
    private RecipeTestClient recipeTestClient;

    public RecipeTestDto(TestContext testContext) {
        super(new RecipeV4Request(), testContext);
    }

    @Override
    public void cleanUp(TestContext context, MicroserviceClient cloudbreakClient) {
        LOGGER.info("Cleaning up recipe with name: {}", getName());
        try {
            when(recipeTestClient.deleteV4(), key("delete-recipe-" + getName()).withSkipOnFail(false));
        } catch (WebApplicationException ignore) {
            LOGGER.warn("Something went wrong during {} recipe delete, because of: {}", getName(), ignore.getMessage(), ignore);
        }
    }

    @Override
    public String getName() {
        return getRequest().getName();
    }

    public RecipeTestDto valid() {
        return withName(getResourcePropertyProvider().getName(getCloudPlatform()))
                .withDescription(getResourcePropertyProvider().getDescription("recipe"))
                .withRecipeType(RecipeV4Type.PRE_CLOUDERA_MANAGER_START)
                .withContent(new String(Base64.getEncoder().encode("#!/bin/bash%necho ALMAA".getBytes())));
    }

    public RecipeTestDto withName(String name) {
        getRequest().setName(name);
        setName(name);
        return this;
    }

    @Override
    public String getResourceNameType() {
        return RECIPE_RESOURCE_NAME;
    }

    public RecipeTestDto withDescription(String description) {
        getRequest().setDescription(description);
        return this;
    }

    public RecipeTestDto withContent(String content) {
        getRequest().setContent(content);
        return this;
    }

    public RecipeTestDto withRecipeType(RecipeV4Type recipeType) {
        getRequest().setType(recipeType);
        return this;
    }

    @Override
    public List<RecipeViewV4Response> getAll(CloudbreakClient client) {
        RecipeV4Endpoint recipeV4Endpoint = client.getDefaultClient().recipeV4Endpoint();
        return recipeV4Endpoint.list(client.getWorkspaceId()).getResponses().stream()
                .filter(s -> s.getName() != null)
                .collect(Collectors.toList());
    }

    @Override
    protected String name(RecipeViewV4Response entity) {
        return entity.getName();
    }

    @Override
    public void delete(TestContext testContext, RecipeViewV4Response entity, CloudbreakClient client) {
        try {
            client.getDefaultClient().recipeV4Endpoint().deleteByName(client.getWorkspaceId(), entity.getName());
        } catch (Exception e) {
            LOGGER.warn("Something went wrong on {} purge. {}", entity.getName(), ResponseUtil.getErrorMessage(e), e);
        }
    }

    public void setSimpleResponses(RecipeViewV4Responses simpleResponses) {
        this.simpleResponses = simpleResponses;
    }

    public RecipeViewV4Responses getSimpleResponses() {
        return simpleResponses;
    }

    @Override
    public String getCrn() {
        return getResponse().getCrn();
    }

    @Override
    public int order() {
        return 500;
    }
}