package com.sequenceiq.distrox.api.v1.distrox.model.image;

import java.io.Serializable;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.sequenceiq.authorization.annotation.ResourceObjectField;
import com.sequenceiq.authorization.resource.AuthorizationResourceAction;
import com.sequenceiq.authorization.resource.AuthorizationVariableType;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions.StackModelDescription;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(Include.NON_NULL)
public class DistroXImageV1Request implements Serializable {

    @ResourceObjectField(action = AuthorizationResourceAction.DESCRIBE_IMAGE_CATALOG,
            variableType = AuthorizationVariableType.NAME, skipAuthzOnNull = true)
    @ApiModelProperty(StackModelDescription.IMAGE_CATALOG)
    private String catalog;

    @ApiModelProperty(StackModelDescription.IMAGE_ID)
    private String id;

    public String getCatalog() {
        return catalog;
    }

    public void setCatalog(String catalog) {
        this.catalog = catalog;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }
}
