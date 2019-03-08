package com.sequenceiq.cloudbreak.api.endpoint.v4.recipes.responses;

import java.util.Set;

import com.google.common.collect.Sets;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.responses.GeneralCollectionV4Response;

import io.swagger.annotations.ApiModel;

@ApiModel
public class RecipeViewV4Responses extends GeneralCollectionV4Response<RecipeViewV4Response> {
    public RecipeViewV4Responses(Set<RecipeViewV4Response> responses) {
        super(responses);
    }

    public RecipeViewV4Responses() {
        super(Sets.newHashSet());
    }
}
