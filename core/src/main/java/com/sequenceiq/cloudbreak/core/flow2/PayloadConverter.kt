package com.sequenceiq.cloudbreak.core.flow2

interface PayloadConverter<P> {
    fun canConvert(sourceClass: Class<*>): Boolean
    fun convert(payload: Any): P
}
