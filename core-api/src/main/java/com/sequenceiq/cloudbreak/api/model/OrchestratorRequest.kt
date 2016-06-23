package com.sequenceiq.cloudbreak.api.model

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

import io.swagger.annotations.ApiModel

@ApiModel
@JsonIgnoreProperties(ignoreUnknown = true)
class OrchestratorRequest : OrchestratorBase()
