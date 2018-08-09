package com.sequenceiq.cloudbreak.api.model.users;

import com.sequenceiq.cloudbreak.api.model.JsonEntity;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions.RecipeModelDescription;

import io.swagger.annotations.ApiModelProperty;

public class OrganizationResourceResponse implements JsonEntity {

    @ApiModelProperty(RecipeModelDescription.ORGANIZATION_ID)
    private Long id;

    @ApiModelProperty(RecipeModelDescription.ORGANIZATION_NAME)
    private String name;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
