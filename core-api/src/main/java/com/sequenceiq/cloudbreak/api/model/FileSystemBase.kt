package com.sequenceiq.cloudbreak.api.model

import javax.validation.constraints.NotNull

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.sequenceiq.cloudbreak.doc.ModelDescriptions
import io.swagger.annotations.ApiModelProperty

@JsonIgnoreProperties(ignoreUnknown = true)
abstract class FileSystemBase {

    @NotNull
    @ApiModelProperty(value = ModelDescriptions.FileSystem.NAME, required = true)
    var name: String? = null

    @NotNull
    @ApiModelProperty(value = ModelDescriptions.FileSystem.TYPE, required = true)
    var type: FileSystemType? = null

    @ApiModelProperty(value = ModelDescriptions.FileSystem.DEFAULT, required = true)
    var isDefaultFs: Boolean = false

    @ApiModelProperty(value = ModelDescriptions.FileSystem.PROPERTIES, required = true)
    var properties: Map<String, String>? = null
}
