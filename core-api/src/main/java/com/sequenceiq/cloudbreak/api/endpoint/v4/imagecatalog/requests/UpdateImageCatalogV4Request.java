package com.sequenceiq.cloudbreak.api.endpoint.v4.imagecatalog.requests;

import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sequenceiq.authorization.annotation.ResourceObjectField;
import com.sequenceiq.authorization.resource.AuthorizationResourceAction;
import com.sequenceiq.authorization.resource.AuthorizationResourceType;
import com.sequenceiq.authorization.resource.AuthorizationVariableType;
import com.sequenceiq.cloudbreak.api.endpoint.v4.imagecatalog.base.ImageCatalogV4Base;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel
@JsonIgnoreProperties(ignoreUnknown = true)
public class UpdateImageCatalogV4Request extends ImageCatalogV4Base {

    @NotNull
    @ApiModelProperty(ModelDescriptions.CRN)
    @ResourceObjectField(action = AuthorizationResourceAction.EDIT_IMAGE_CATALOG, type = AuthorizationResourceType.IMAGE_CATALOG,
            variableType = AuthorizationVariableType.CRN)
    private String crn;

    public String getCrn() {
        return crn;
    }

    public void setCrn(String crn) {
        this.crn = crn;
    }

}
