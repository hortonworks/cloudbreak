package com.sequenceiq.cloudbreak.core.flow2

interface EventConverter<E> {
    fun convert(key: String): E
}
