package com.sequenceiq.cloudbreak.core.flow2

interface StateConverter<S> {
    fun convert(stateRepresentation: String): S
}
