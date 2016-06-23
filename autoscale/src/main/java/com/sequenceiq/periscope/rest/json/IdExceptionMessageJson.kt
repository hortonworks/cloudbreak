package com.sequenceiq.periscope.rest.json

import com.sequenceiq.periscope.api.model.Json

class IdExceptionMessageJson : ExceptionMessageJson, Json {

    var id: Long = 0

    constructor() {
    }

    constructor(id: Long, message: String) : super(message) {
        this.id = id
    }
}
