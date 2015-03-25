package com.sequenceiq.cloudbreak.controller.json;

import com.sequenceiq.cloudbreak.controller.doc.ModelDescriptions;
import com.wordnik.swagger.annotations.ApiModel;
import com.wordnik.swagger.annotations.ApiModelProperty;

@ApiModel
public class TemplateResponse extends TemplateBase {
    @ApiModelProperty(ModelDescriptions.ID)
    private Long id;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }
}
