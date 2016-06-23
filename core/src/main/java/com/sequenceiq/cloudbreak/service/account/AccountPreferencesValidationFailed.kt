package com.sequenceiq.cloudbreak.service.account


class AccountPreferencesValidationFailed : Exception {

    constructor(message: String) : super(message) {
    }

    constructor(message: String, cause: Throwable) : super(message, cause) {
    }

    constructor(cause: Throwable) : super(cause) {
    }

}
