package com.sequenceiq.cloudbreak.api.endpoint.v4.recipes.responses;

import java.util.Set;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.responses.GeneralSetV4Response;

public class RecipeV4Responses extends GeneralSetV4Response<RecipeV4ViewResponse> {
    public RecipeV4Responses(Set<RecipeV4ViewResponse> responses) {
        super(responses);
    }
}
