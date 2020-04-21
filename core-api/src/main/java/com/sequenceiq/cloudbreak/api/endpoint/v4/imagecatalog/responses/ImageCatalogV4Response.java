package com.sequenceiq.cloudbreak.api.endpoint.v4.imagecatalog.responses;

import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.sequenceiq.authorization.resource.ResourceCrnAwareApiModel;
import com.sequenceiq.cloudbreak.api.endpoint.v4.imagecatalog.base.ImageCatalogV4Base;
import com.sequenceiq.cloudbreak.api.endpoint.v4.workspace.responses.WorkspaceResourceV4Response;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions.ImageCatalogDescription;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel
@JsonInclude(Include.NON_NULL)
@NotNull
public class ImageCatalogV4Response extends ImageCatalogV4Base implements ResourceCrnAwareApiModel {

    @ApiModelProperty(value = ImageCatalogDescription.DEFAULT, required = true)
    private boolean usedAsDefault;

    @ApiModelProperty(ImageCatalogDescription.IMAGE_RESPONSES)
    private ImagesV4Response images;

    @ApiModelProperty(ModelDescriptions.WORKSPACE_OF_THE_RESOURCE)
    private WorkspaceResourceV4Response workspace;

    @ApiModelProperty(ModelDescriptions.CRN)
    private String crn;

    @ApiModelProperty(ModelDescriptions.CREATED)
    private Long created;

    public Long getCreated() {
        return created;
    }

    public void setCreated(Long created) {
        this.created = created;
    }

    public String getCrn() {
        return crn;
    }

    public void setCrn(String crn) {
        this.crn = crn;
    }

    public boolean isUsedAsDefault() {
        return usedAsDefault;
    }

    public void setUsedAsDefault(boolean usedAsDefault) {
        this.usedAsDefault = usedAsDefault;
    }

    public ImagesV4Response getImages() {
        return images;
    }

    public void setImages(ImagesV4Response images) {
        this.images = images;
    }

    public WorkspaceResourceV4Response getWorkspace() {
        return workspace;
    }

    public void setWorkspace(WorkspaceResourceV4Response workspace) {
        this.workspace = workspace;
    }

    @Override
    @JsonIgnore
    public String getResourceCrn() {
        return crn;
    }
}
