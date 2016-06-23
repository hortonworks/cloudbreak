package com.sequenceiq.cloudbreak.controller.json

import java.util.HashMap

class ValidationResult {

    var validationErrors: MutableMap<String, String> = HashMap()
        get() = validationErrors

    fun addValidationError(field: String, message: String) {
        validationErrors.put(field, message)
    }

}
