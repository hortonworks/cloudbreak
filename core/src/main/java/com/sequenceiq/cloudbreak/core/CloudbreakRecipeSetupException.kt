package com.sequenceiq.cloudbreak.core

class CloudbreakRecipeSetupException : CloudbreakException {
    constructor(message: String) : super(message) {
    }

    constructor(message: String, cause: Throwable) : super(message, cause) {
    }

    constructor(cause: Throwable) : super(cause) {
    }
}
