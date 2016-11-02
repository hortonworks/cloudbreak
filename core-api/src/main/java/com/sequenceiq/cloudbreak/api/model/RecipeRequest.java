package com.sequenceiq.cloudbreak.api.model;

import com.sequenceiq.cloudbreak.doc.ModelDescriptions;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel
public class RecipeRequest extends RecipeBase {

    @ApiModelProperty(value = ModelDescriptions.RecipeModelDescription.PRE_URL)
    private String preUrl;

    @ApiModelProperty(value = ModelDescriptions.RecipeModelDescription.POST_URL)
    private String postUrl;

    public String getPreUrl() {
        return preUrl;
    }

    public void setPreUrl(String preUrl) {
        this.preUrl = preUrl;
    }

    public String getPostUrl() {
        return postUrl;
    }

    public void setPostUrl(String postUrl) {
        this.postUrl = postUrl;
    }
}
