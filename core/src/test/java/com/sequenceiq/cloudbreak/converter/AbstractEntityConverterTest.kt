package com.sequenceiq.cloudbreak.converter

abstract class AbstractEntityConverterTest<S> : AbstractConverterTest() {
    val source = createSource()

    abstract fun createSource(): S
}
