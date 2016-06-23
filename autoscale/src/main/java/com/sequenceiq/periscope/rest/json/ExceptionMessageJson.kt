package com.sequenceiq.periscope.rest.json

open class ExceptionMessageJson {

    var message: String? = null

    constructor() {
    }

    constructor(message: String) {
        this.message = message
    }
}
