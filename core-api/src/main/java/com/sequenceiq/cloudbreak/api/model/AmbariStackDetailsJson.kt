package com.sequenceiq.cloudbreak.api.model

import javax.validation.constraints.NotNull

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.sequenceiq.cloudbreak.doc.ModelDescriptions.AmbariStackDetailsDescription
import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("AmbariStackDetails")
@JsonIgnoreProperties(ignoreUnknown = true)
class AmbariStackDetailsJson {

    @NotNull
    @ApiModelProperty(value = AmbariStackDetailsDescription.STACK, required = true)
    var stack: String? = null
    @NotNull
    @ApiModelProperty(value = AmbariStackDetailsDescription.VERSION, required = true)
    var version: String? = null
    @NotNull
    @ApiModelProperty(value = AmbariStackDetailsDescription.OS, required = true)
    var os: String? = null
    @NotNull
    @ApiModelProperty(value = AmbariStackDetailsDescription.STACK_REPO_ID, required = true)
    var stackRepoId: String? = null
    @NotNull
    @ApiModelProperty(value = AmbariStackDetailsDescription.STACK_BASE_URL, required = true)
    var stackBaseURL: String? = null
    @NotNull
    @ApiModelProperty(value = AmbariStackDetailsDescription.UTILS_REPO_ID, required = true)
    var utilsRepoId: String? = null
    @NotNull
    @ApiModelProperty(value = AmbariStackDetailsDescription.UTILS_BASE_URL, required = true)
    var utilsBaseURL: String? = null
    @NotNull
    @ApiModelProperty(value = AmbariStackDetailsDescription.VERIFY, required = true)
    var verify: Boolean? = null
}
