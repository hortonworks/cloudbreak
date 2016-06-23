package com.sequenceiq.cloudbreak.api.model

import javax.validation.constraints.NotNull

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.sequenceiq.cloudbreak.doc.ModelDescriptions

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("RDSConfig")
@JsonIgnoreProperties(ignoreUnknown = true)
class RDSConfigJson {

    @NotNull
    @ApiModelProperty(value = ModelDescriptions.RDSConfig.CONNECTION_URL, required = true)
    var connectionURL: String? = null
    @NotNull
    @ApiModelProperty(value = ModelDescriptions.RDSConfig.DB_TYPE, required = true)
    var databaseType: RDSDatabase? = null
    @NotNull
    @ApiModelProperty(value = ModelDescriptions.RDSConfig.USERNAME, required = true)
    var connectionUserName: String? = null
    @NotNull
    @ApiModelProperty(value = ModelDescriptions.RDSConfig.PASSWORD, required = true)
    var connectionPassword: String? = null
}
