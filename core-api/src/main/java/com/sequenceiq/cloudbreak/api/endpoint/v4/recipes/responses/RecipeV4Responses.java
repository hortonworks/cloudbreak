package com.sequenceiq.cloudbreak.api.endpoint.v4.recipes.responses;

import java.util.Set;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.responses.GeneralSetV4Response;

public class RecipeV4Responses extends GeneralSetV4Response<RecipeViewV4Response> {
    public RecipeV4Responses(Set<RecipeViewV4Response> responses) {
        super(responses);
    }
}
