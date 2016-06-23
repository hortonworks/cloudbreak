package com.sequenceiq.cloudbreak.cloud.event

interface Selectable : Payload {
    fun selector(): String
}
