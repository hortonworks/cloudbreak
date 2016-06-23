package com.sequenceiq.cloudbreak.orchestrator.salt.client.target

interface Target<T> {

    val target: T

    val type: String
}
