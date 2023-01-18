package com.sequenceiq.cloudbreak.api.endpoint.v4.imagecatalog.responses;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.sequenceiq.cloudbreak.api.endpoint.v4.imagecatalog.base.ImageCatalogV4Base;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions.ImageCatalogDescription;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema
@JsonInclude(Include.NON_NULL)
public class ImageCatalogV4ShortResponse extends ImageCatalogV4Base {

    @Schema(description = ImageCatalogDescription.DEFAULT, required = true)
    private boolean usedAsDefault;

    @Schema(description = ModelDescriptions.CRN)
    private String crn;

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

}
