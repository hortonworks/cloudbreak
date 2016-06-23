package com.sequenceiq.cloudbreak.core.flow2

interface FlowEvent {
    fun name(): String
    fun stringRepresentation(): String
}
