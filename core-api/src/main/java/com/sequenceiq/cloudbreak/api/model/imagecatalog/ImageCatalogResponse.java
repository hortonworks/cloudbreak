package com.sequenceiq.cloudbreak.api.model.imagecatalog;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.sequenceiq.cloudbreak.api.model.users.OrganizationResourceResponse;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions.ImageCatalogDescription;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel
@JsonInclude(Include.NON_NULL)
public class ImageCatalogResponse extends ImageCatalogBase {

    @ApiModelProperty(value = ModelDescriptions.ID, required = true)
    private Long id;

    @ApiModelProperty(value = ModelDescriptions.PUBLIC_IN_ACCOUNT, required = true)
    private boolean publicInAccount;

    @ApiModelProperty(value = ImageCatalogDescription.DEFAULT, required = true)
    private boolean usedAsDefault;

    @ApiModelProperty(ImageCatalogDescription.IMAGE_RESPONSES)
    private ImagesResponse imagesResponse;

    @ApiModelProperty(ModelDescriptions.ORGANIZATION_OF_THE_RESOURCE)
    private OrganizationResourceResponse organization;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public boolean isPublicInAccount() {
        return publicInAccount;
    }

    public void setPublicInAccount(boolean publicInAccount) {
        this.publicInAccount = publicInAccount;
    }

    public boolean isUsedAsDefault() {
        return usedAsDefault;
    }

    public void setUsedAsDefault(boolean usedAsDefault) {
        this.usedAsDefault = usedAsDefault;
    }

    public ImagesResponse getImagesResponse() {
        return imagesResponse;
    }

    public void setImagesResponse(ImagesResponse imagesResponse) {
        this.imagesResponse = imagesResponse;
    }

    public OrganizationResourceResponse getOrganization() {
        return organization;
    }

    public void setOrganization(OrganizationResourceResponse organization) {
        this.organization = organization;
    }
}
