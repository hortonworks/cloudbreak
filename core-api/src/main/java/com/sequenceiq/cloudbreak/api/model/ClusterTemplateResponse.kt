package com.sequenceiq.cloudbreak.api.model

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
class ClusterTemplateResponse : ClusterTemplateBase() {

    var id: Long? = null

}
