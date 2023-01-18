package com.sequenceiq.cloudbreak.api.endpoint.v4.imagecatalog.responses;

import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.sequenceiq.cloudbreak.api.endpoint.v4.imagecatalog.base.ImageCatalogV4Base;
import com.sequenceiq.cloudbreak.api.endpoint.v4.workspace.responses.WorkspaceResourceV4Response;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions.ImageCatalogDescription;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema
@JsonInclude(Include.NON_NULL)
@NotNull
public class ImageCatalogV4Response extends ImageCatalogV4Base {

    @Schema(description = ImageCatalogDescription.DEFAULT, required = true)
    private boolean usedAsDefault;

    @Schema(description = ImageCatalogDescription.IMAGE_RESPONSES)
    private ImagesV4Response images;

    @Schema(description = ModelDescriptions.WORKSPACE_OF_THE_RESOURCE)
    private WorkspaceResourceV4Response workspace;

    @Schema(description = ModelDescriptions.CRN)
    private String crn;

    @Schema(description = ModelDescriptions.CREATED)
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
}
