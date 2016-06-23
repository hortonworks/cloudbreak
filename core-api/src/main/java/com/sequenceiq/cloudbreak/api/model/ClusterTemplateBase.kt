package com.sequenceiq.cloudbreak.api.model

import com.fasterxml.jackson.annotation.JsonRawValue

open class ClusterTemplateBase : JsonEntity {

    var name: String? = null
    var template: String? = null
    var type: ClusterTemplateType? = null
}
