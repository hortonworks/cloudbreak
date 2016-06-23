package com.sequenceiq.cloudbreak.service

import java.util.HashSet

import com.sequenceiq.cloudbreak.domain.Resource

class BuildStackFailureException : CloudbreakServiceException {

    val resourceSet: Set<Resource>

    constructor(ex: Exception) : super(ex) {
        this.resourceSet = HashSet<Resource>()
    }

    constructor(message: String, cause: Throwable) : super(message, cause) {
        this.resourceSet = HashSet<Resource>()
    }

    constructor(message: String, cause: Throwable, resourceSet: Set<Resource>) : super(message, cause) {
        this.resourceSet = resourceSet
    }
}
