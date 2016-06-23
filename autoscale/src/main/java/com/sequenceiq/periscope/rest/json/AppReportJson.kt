package com.sequenceiq.periscope.rest.json

import com.sequenceiq.periscope.api.model.Json

class AppReportJson : Json {

    var appId: String? = null
    var user: String? = null
    var queue: String? = null
    var state: String? = null
    var url: String? = null
    var start: Long = 0
    var finish: Long = 0
    var progress: Float = 0.toFloat()
    var usedContainers: Int = 0
    var reservedContainers: Int = 0
    var usedMemory: Int = 0
    var usedVCores: Int = 0
}
