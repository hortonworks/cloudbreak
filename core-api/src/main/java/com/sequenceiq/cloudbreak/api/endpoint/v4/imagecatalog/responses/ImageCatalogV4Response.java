package com.sequenceiq.cloudbreak.api.endpoint.v4.imagecatalog.responses;

import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.sequenceiq.cloudbreak.api.endpoint.v4.imagecatalog.base.ImageCatalogV4Base;
import com.sequenceiq.cloudbreak.api.model.users.WorkspaceResourceResponse;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions.ImageCatalogDescription;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel
@JsonInclude(Include.NON_NULL)
@NotNull
public class ImageCatalogV4Response extends ImageCatalogV4Base {

    @ApiModelProperty(value = ModelDescriptions.ID, required = true)
    private Long id;

    @ApiModelProperty(value = ImageCatalogDescription.DEFAULT, required = true)
    private boolean usedAsDefault;

    @ApiModelProperty(ImageCatalogDescription.IMAGE_RESPONSES)
    private ImagesV4Response images;

    @ApiModelProperty(ModelDescriptions.WORKSPACE_OF_THE_RESOURCE)
    private WorkspaceResourceResponse workspace;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public WorkspaceResourceResponse getWorkspace() {
        return workspace;
    }

    public void setWorkspace(WorkspaceResourceResponse workspace) {
        this.workspace = workspace;
    }
}
