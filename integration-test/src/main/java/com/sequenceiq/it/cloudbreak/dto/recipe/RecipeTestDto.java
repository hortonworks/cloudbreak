package com.sequenceiq.it.cloudbreak.dto.recipe;

import java.util.List;
import java.util.stream.Collectors;

import com.sequenceiq.cloudbreak.api.endpoint.v4.recipes.RecipeV4Endpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v4.recipes.requests.RecipeV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.recipes.requests.RecipeV4Type;
import com.sequenceiq.cloudbreak.api.endpoint.v4.recipes.responses.RecipeV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.recipes.responses.RecipeViewV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.recipes.responses.RecipeViewV4Responses;
import com.sequenceiq.cloudbreak.common.base64.Base64Util;
import com.sequenceiq.cloudbreak.common.exception.NotFoundException;
import com.sequenceiq.it.cloudbreak.Prototype;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.DeletableTestDto;
import com.sequenceiq.it.cloudbreak.microservice.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.util.ResponseUtil;

@Prototype
public class RecipeTestDto extends DeletableTestDto<RecipeV4Request, RecipeV4Response, RecipeTestDto, RecipeViewV4Response> {

    private static final String RECIPE_RESOURCE_NAME = "recipeName";

    private RecipeViewV4Responses simpleResponses;

    private String accountId;

    public RecipeTestDto(TestContext testContext) {
        super(new RecipeV4Request(), testContext);
    }

    @Override
    public void deleteForCleanup() {
        try {
            CloudbreakClient client = getClientForCleanup();
            client.getDefaultClient(getTestContext()).recipeV4Endpoint().deleteByCrn(client.getWorkspaceId(), getResponse().getCrn());
        } catch (NotFoundException nfe) {
            LOGGER.info("recipe not found, thus cleanup not needed.");
        }
    }

    @Override
    public boolean deletable(RecipeViewV4Response entity) {
        return name(entity).startsWith(getResourcePropertyProvider().prefix(getCloudPlatform()));
    }

    @Override
    public String getName() {
        return getRequest().getName();
    }

    public RecipeTestDto valid() {
        return withName(getResourcePropertyProvider().getName(getCloudPlatform()))
                .withDescription(getResourcePropertyProvider().getDescription("recipe"))
                .withRecipeType(RecipeV4Type.PRE_SERVICE_DEPLOYMENT)
                .withContent(Base64Util.encode("#!/bin/bash%necho ALMAA"));
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

    public RecipeTestDto withAccountId(String accountId) {
        this.accountId = accountId;
        return this;
    }

    public String getAccountId() {
        return accountId;
    }

    @Override
    public List<RecipeViewV4Response> getAll(CloudbreakClient client) {
        RecipeV4Endpoint recipeV4Endpoint = client.getDefaultClient(getTestContext()).recipeV4Endpoint();
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
            client.getDefaultClient(getTestContext()).recipeV4Endpoint().deleteByName(client.getWorkspaceId(), entity.getName());
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
        return 600;
    }
}